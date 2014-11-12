package tv.matchstick.server.fling.ssdp;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import tv.matchstick.fling.FlingDevice;
import tv.matchstick.fling.images.WebImage;
import tv.matchstick.server.fling.mdns.DeviceScanner;
import tv.matchstick.server.fling.mdns.IDeviceScanListener;
import tv.matchstick.server.fling.ssdp.SSDP.ParsedDatagram;
import android.content.Context;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;

public class SsdpDeviceScanner extends DeviceScanner {
    private final static String TAG = "SsdpDeviceScanner";
    private final Map<String, ScannerPrivData> mScannerData = new HashMap<String, ScannerPrivData>();
    private final static int NUM_OF_THREADS = 20;
    private final static int RESCAN_INTERVAL = 10000;
    private SSDPSocket mSSDPSocket;
    private SSDPSearchMsg mSearchMsg;
    private Thread mResponseThread;
    private Thread mNotifyThread;
    private Pattern uuidReg;
    private List<String> mDiscoveredDeviceList = new ArrayList<String>();
    private ConcurrentHashMap<String, String> mFoundDeviceMap = new ConcurrentHashMap<String, String>();
    private Timer mCheckOfflineTimer;
    private Timer mSendSearchTimer;
    private boolean mStarting = false;

    private Executor mExecutor = Executors.newFixedThreadPool(NUM_OF_THREADS,
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread th = new Thread(r);
                    th.setName("SSDP Thread");
                    return th;
                }
            });

    public SsdpDeviceScanner(Context context) {
        super(context);
        uuidReg = Pattern.compile("(?<=uuid:)(.+?)(?=(::)|$)");
    }

    private void openSocket() {
        if (mSSDPSocket != null && mSSDPSocket.isConnected())
            return;

        try {
            InetAddress source = getIpAddress(mContext);
            if (source == null)
                return;

            mSSDPSocket = new SSDPSocket(source);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private InetAddress getIpAddress(Context context)
            throws UnknownHostException {
        WifiManager wifiMgr = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();

        if (ip == 0) {
            return null;
        } else {
            byte[] ipAddress = convertIpAddress(ip);
            return InetAddress.getByAddress(ipAddress);
        }
    }

    private byte[] convertIpAddress(int ip) {
        return new byte[] { (byte) (ip & 0xFF), (byte) ((ip >> 8) & 0xFF),
                (byte) ((ip >> 16) & 0xFF), (byte) ((ip >> 24) & 0xFF) };
    }

    public void start() {
        if (mStarting)
            return;
        stop();
        mStarting = true;
        openSocket();
        mSearchMsg = new SSDPSearchMsg();
        mCheckOfflineTimer = new Timer();
        mCheckOfflineTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                List<String> removeList = new ArrayList<String>();
                synchronized (mScannerData) {
                    Iterator iterator = mScannerData.keySet().iterator();
                    while (iterator.hasNext()) {
                        String key = (String) iterator.next();
                        ScannerPrivData value = mScannerData.get(key);
                        if (value.mElapsedRealtime < SystemClock
                                .elapsedRealtime() - RESCAN_INTERVAL) {
                            removeList.add(key);
                        }
                    }
                }
                for (String id : removeList) {
                    setDeviceOffline(id);
                }
            }
        }, 100, RESCAN_INTERVAL);
        
        mSendSearchTimer = new Timer();
        mSendSearchTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                if (mSSDPSocket != null) {
                    try {
                        android.util.Log.d(TAG, "send msg");
                        mSSDPSocket.send(mSearchMsg.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 100, 5000);

        mResponseThread = new Thread(mResponseHandler);
        mNotifyThread = new Thread(mRespNotifyHandler);

        mResponseThread.start();
        mNotifyThread.start();
    }

    public void stop() {
        mStarting = false;
        if (mCheckOfflineTimer != null) {
            mCheckOfflineTimer.cancel();
        }
        if (mSendSearchTimer != null) {
            mSendSearchTimer.cancel();
        }
        if (mResponseThread != null) {
            mResponseThread.interrupt();
        }
        if (mNotifyThread != null) {
            mNotifyThread.interrupt();
        }
        if (mSSDPSocket != null) {
            mSSDPSocket.close();
            mSSDPSocket = null;
        }
    }

    private Runnable mResponseHandler = new Runnable() {
        @Override
        public void run() {
            while (mSSDPSocket != null) {
                try {
                    handleDatagramPacket(SSDP.convertDatagram(mSSDPSocket
                            .responseReceive()));
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    };

    private Runnable mRespNotifyHandler = new Runnable() {
        @Override
        public void run() {
            while (mSSDPSocket != null) {
                try {
                    handleDatagramPacket(SSDP.convertDatagram(mSSDPSocket
                            .notifyReceive()));
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    };

    private void handleDatagramPacket(final ParsedDatagram pd) {
        if (pd.data == null)
            return;

        String serviceFilter = pd.data
                .get(SSDP.SL_NOTIFY.equals(pd.type) ? SSDP.NT : SSDP.ST);
        if (!"urn:dial-multiscreen-org:service:dial:1".equals(serviceFilter))
            return;

        String usnKey = pd.data.get(SSDP.USN);
        if (usnKey == null || usnKey.length() == 0)
            return;

        Matcher m = uuidReg.matcher(usnKey);

        if (!m.find())
            return;

        String uuid = m.group();

        if (SSDP.NTS_BYEBYE.equals(pd.data.get(SSDP.NTS))) {
            android.util.Log.d(TAG, "byebye uuid = " + uuid);
            String deviceId = mFoundDeviceMap.get(uuid);
            setDeviceOffline(deviceId);
        } else {
            String location = pd.data.get(SSDP.LOCATION);

            if (location == null || location.length() == 0)
                return;
            android.util.Log.d(TAG, "location = " + location);
            if (!mDiscoveredDeviceList.contains(uuid)
                    && mFoundDeviceMap.get(uuid) == null) {
                mDiscoveredDeviceList.add(uuid);
                android.util.Log.d(TAG, "getLocationData");
                getLocationData(location, uuid);
            } else {
                android.util.Log.d(TAG, "update");
                String deviceId = mFoundDeviceMap.get(uuid);
                if (deviceId != null) {
                    synchronized (mScannerData) {
                        if (mScannerData.get(deviceId) != null) {
                            mScannerData.get(deviceId).mElapsedRealtime = SystemClock
                                    .elapsedRealtime();
                        }
                    }
                }
            }
        }
    }

    private void getLocationData(final String location, final String uuid) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                boolean success = false;
                final LocationDevice device = new LocationDevice();

                DefaultHandler dh = new DefaultHandler() {
                    String currentValue = null;
                    int iconPosition = 0;

                    @Override
                    public void characters(char[] ch, int start, int length)
                            throws SAXException {
                        if (currentValue == null) {
                            currentValue = new String(ch, start, length);
                        } else {
                            currentValue += new String(ch, start, length);
                        }
                    }

                    @Override
                    public void startElement(String uri, String localName,
                            String qName, Attributes attributes)
                            throws SAXException {
                        if ("iconList".equals(qName)) {
                            device.iconImages = new ArrayList<LocationImage>();
                        } else if ("icon".equals(qName)) {
                            LocationImage image = new LocationImage();
                            device.iconImages.add(image);
                        }
                        currentValue = null;
                    }

                    @Override
                    public void endElement(String uri, String localName,
                            String qName) throws SAXException {
                        if ("URLBase".equals(qName)) {
                            device.url = currentValue;
                        } else if ("friendlyName".equals(qName)) {
                            device.friendlyName = currentValue;
                        } else if ("manufacturer".equals(qName)) {
                            device.manufacturer = currentValue;
                        } else if ("modelName".equals(qName)) {
                            device.modelName = currentValue;
                        } else if ("icon".equals(qName)) {
                            iconPosition++;
                        } else if ("mimetype".equals(qName)) {
                            device.iconImages.get(iconPosition).mimetype = currentValue;
                        } else if ("width".equals(qName)) {
                            device.iconImages.get(iconPosition).width = currentValue;
                        } else if ("height".equals(qName)) {
                            device.iconImages.get(iconPosition).height = currentValue;
                        } else if ("depth".equals(qName)) {
                            device.iconImages.get(iconPosition).depth = currentValue;
                        } else if ("url".equals(qName)) {
                            device.iconImages.get(iconPosition).url = currentValue;
                        }
                        currentValue = null;
                    }
                };

                SAXParserFactory factory = SAXParserFactory.newInstance();

                SAXParser parser;
                try {
                    URL mURL = new URL(location);
                    URLConnection urlConnection = mURL.openConnection();
                    InputStream in = new BufferedInputStream(urlConnection
                            .getInputStream());
                    try {
                        Scanner s = new Scanner(in).useDelimiter("\\A");
                        String xml = s.hasNext() ? s.next() : "";

                        parser = factory.newSAXParser();
                        parser.parse(new ByteArrayInputStream(xml.getBytes()),
                                dh);
                        success = true;
                    } finally {
                        in.close();
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (success) {
                    onResult(uuid, device);
                } else {
                    mDiscoveredDeviceList.remove(uuid);
                }
            }
        });
    }

    private void onResult(String uuid, LocationDevice device) {
        try {
            if (!"openflint".equals(device.manufacturer))
                return;
            String deviceId = device.friendlyName;
            ScannerPrivData data = null;
            final FlingDevice flingDevice;
            if (deviceId != null) {
                synchronized (mScannerData) {
                    if (device.url == null) {
                        mScannerData.remove(deviceId);
                        mDiscoveredDeviceList.remove(uuid);
                        return;
                    }
                    int position = device.url.lastIndexOf(":");
                    String ip = device.url.substring("http://".length(),
                            position);
                    String port = device.url.substring(position + 1,
                            device.url.length());
                    Inet4Address address = (Inet4Address) InetAddress
                            .getByName(ip);
                    ArrayList<WebImage> iconList = new ArrayList<WebImage>();
                    if (device.iconImages != null) {
                        for (LocationImage image : device.iconImages)
                            iconList.add(new WebImage(Uri.parse(String.format(
                                    "http://%s:8008%s", new Object[] { ip,
                                            image.url }))));
                    }

                    deviceId = deviceId + address.getHostAddress();

                    flingDevice = FlingDevice.Builder.create((String) deviceId,
                            address);
                    FlingDevice.setFriendlyName(flingDevice,
                            device.friendlyName);
                    FlingDevice.setModelName(flingDevice, device.modelName);
                    FlingDevice.setDeviceVersion(flingDevice, "02");
                    FlingDevice.setServicePort(flingDevice,
                            Integer.valueOf(port));
                    FlingDevice.setIconList(flingDevice, iconList);
                    FlingDevice.setFoundSource(flingDevice,
                            FlingDevice.FOUND_SOURCE_SSDP);
                    data = (ScannerPrivData) mScannerData.get(deviceId);
                    if (data != null) {
                        if (flingDevice.equals(data.mFlingDevice)) {
                            if (!data.d) {
                                data.mElapsedRealtime = SystemClock
                                        .elapsedRealtime();
                            }
                            mDiscoveredDeviceList.remove(uuid);
                            return;
                        } else {
                            mScannerData.remove(deviceId);
                        }
                    }
                    mScannerData.put((String) deviceId, new ScannerPrivData(
                            flingDevice, 10L, uuid));
                    mFoundDeviceMap.put(uuid, deviceId);
                    mDiscoveredDeviceList.remove(uuid);
                }

                if (data != null && data.mFlingDevice != null) {
                    notifyDeviceOffline(data.mFlingDevice);
                }

                final List<IDeviceScanListener> listenerList = getDeviceScannerListenerList();
                if (listenerList == null)
                    return;

                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        for (IDeviceScanListener listener : listenerList)
                            listener.onDeviceOnline(flingDevice);
                    }
                });
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setDeviceOffline(String id) {
        FlingDevice device = null;
        synchronized (mScannerData) {
            ScannerPrivData data = (ScannerPrivData) this.mScannerData.get(id);
            if (data != null) {
                data.mElapsedRealtime = SystemClock.elapsedRealtime();
                data.d = true;
                device = data.mFlingDevice;
                if (device != null) {
                    notifyDeviceOffline(device);
                    mFoundDeviceMap.remove(data.mUuid);
                    mScannerData.remove(id);
                }
            }
        }
    }

    @Override
    protected void startScanInternal(List<NetworkInterface> list) {
        start();
    }

    @Override
    protected void stopScanInternal() {
        stop();
    }

    @Override
    public void onAllDevicesOffline() {
        synchronized (mScannerData) {
            boolean isEmpty = mScannerData.isEmpty();
            if (!isEmpty) {
                mScannerData.clear();
                mDiscoveredDeviceList.clear();
                mFoundDeviceMap.clear();
                final List<IDeviceScanListener> list = getDeviceScannerListenerList();
                if (list != null) {
                    mHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            for (IDeviceScanListener listener : list) {
                                listener.onAllDevicesOffline();
                            }
                        }
                    });
                }
            }
        }
    }

    final class ScannerPrivData {
        FlingDevice mFlingDevice;
        long mElapsedRealtime;
        long mTTl;
        boolean d;
        String mUuid;

        ScannerPrivData(FlingDevice device, long ttl, String uuid) {
            super();
            mFlingDevice = device;
            mTTl = ttl;
            mElapsedRealtime = SystemClock.elapsedRealtime();
            mUuid = uuid;
        }
    }

    final class LocationDevice {
        List<LocationImage> iconImages;
        String url;
        String friendlyName;
        String manufacturer;
        String modelName;
    }

    final class LocationImage {
        String mimetype;
        String width;
        String height;
        String depth;
        String url;
    }
}
