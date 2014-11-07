/*
 * Copyright (C) 2013-2014, Infthink (Beijing) Technology Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS-IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package tv.matchstick.server.fling.mdns;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import tv.matchstick.client.internal.LOG;
import tv.matchstick.fling.FlingDevice;
import tv.matchstick.fling.images.WebImage;
import android.content.Context;
import android.net.Uri;
import android.os.SystemClock;

public final class MdnsDeviceScanner extends DeviceScanner {
    private static final LOG log = new LOG("MdnsDeviceScanner");

    private final List<MdnsClient> mMdnsClientList = new ArrayList<MdnsClient>();

    private final Map<String, ScannerDeviceData> mFoundDevices = new HashMap<String, ScannerDeviceData>();

    private final String mName;

    private Thread mScannerLoopThread;

    private boolean mQuit;

    public MdnsDeviceScanner(Context context) {
        super(context);
        // TODO Auto-generated constructor stub

        mName = "Fling Device";
    }

    @Override
    public void setDeviceOffline(String deviceId) {
        // TODO Auto-generated method stub
        // let device offline according to the input 'deviceId';
        synchronized (this.mFoundDevices) {
            ScannerDeviceData deviceInfo = (ScannerDeviceData) mFoundDevices
                    .get(deviceId);

            if (deviceInfo != null) {
                deviceInfo.mScannedTime = SystemClock.elapsedRealtime();
                deviceInfo.mIsOffline = true;

                FlingDevice device = deviceInfo.mFlingDevice;
                if (device != null) {
                    notifyDeviceOffline(device);
                }
            }
        }
    }

    @Override
    protected synchronized void startScanInternal(List<NetworkInterface> list) {
        // TODO Auto-generated method stub

        if (list.isEmpty()) {
            log.w("No network interfaces to scan on!");
            return;
        }

        // stop all mdns client???!!!
        if (mMdnsClientList.size() > 0) {
            stopScanInternal();
        }

        Iterator<NetworkInterface> it = list.iterator();
        while (it.hasNext()) {
            NetworkInterface network = (NetworkInterface) it.next();

            MdnsClient mdnsClient = new MdnsClient("_openflint._tcp.local.",
                    network) {

                @Override
                protected void onScanResults(FlingDeviceInfo info) {
                    onResults(info);
                }

            };

            try {
                mdnsClient.startScan();

                mMdnsClientList.add(mdnsClient);
            } catch (Exception e) {
                e.printStackTrace();
                log.w("Couldn't start MDNS client for %s", network);
            }
        }

        mScannerLoopThread = new Thread(new Runnable() {

            @Override
            public void run() {
                scanLoop();
            }

        });

        mScannerLoopThread.start();
    }

    @Override
    protected void stopScanInternal() {
        // TODO Auto-generated method stub
        
        synchronized (mMdnsClientList) {
            if (!mMdnsClientList.isEmpty()) {
                Iterator<MdnsClient> it = mMdnsClientList.iterator();
                while (it.hasNext()) {
                    ((MdnsClient) it.next()).stopScan();
                }

                mMdnsClientList.clear();
            }
        }
        mQuit = true;
        if (mScannerLoopThread != null) {
            boolean needWait = true;
            while (needWait) {
                try {
                    mScannerLoopThread.interrupt();
                    mScannerLoopThread.join();

                    needWait = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();

                    needWait = true;
                }
            }
        }

        mScannerLoopThread = null;
    }

    @Override
    public void onAllDevicesOffline() {
        // TODO Auto-generated method stub
        synchronized (mMdnsClientList) {
            if (!mMdnsClientList.isEmpty()) {
                mMdnsClientList.clear();

            }
                final List<IDeviceScanListener> listeners = super
                        .getDeviceScannerListenerList();

                if (listeners != null) {
                    this.mHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            Iterator<IDeviceScanListener> it = listeners
                                    .iterator();
                            while (it.hasNext()) {
                                ((IDeviceScanListener) it.next())
                                        .onAllDevicesOffline();
                            }
                        }

                    });
                }
            }
    }

    void onResults(FlingDeviceInfo info) {
        // print found device info
        if (log.isDebugEnabled()) {
            log.d("FQDN: %s", info.mFQDN);

            if (info.mIpV4AddrList != null) {
                Iterator<Inet4Address> it = info.mIpV4AddrList.iterator();
                while (it.hasNext()) {
                    log.d("IPv4 address: %s", (Inet4Address) it.next());
                }
            }

            if (info.mIpV6AddrList != null) {
                Iterator<Inet6Address> it = info.mIpV6AddrList.iterator();
                while (it.hasNext()) {
                    log.d("IPv6 address: %s", (Inet6Address) it.next());
                }
            }

            log.d("service name: %s", info.mName);

            log.d("service host: %s", info.mHost);

            log.d("service proto: %d", info.mProtocol);

            log.d("service port: %d", info.mPort);

            log.d("service priority: %d", info.mPriority);

            log.d("service weight: %d", info.mWeight);

            if (info.mTextStringList != null) {
                Iterator<String> it = info.mTextStringList.iterator();
                while (it.hasNext()) {
                    log.d("text string: %s", (String) it.next());
                }
            }

            log.d("TTL: %d", info.mTTL);
        }

        List<String> deviceInfos = info.mTextStringList;
        if (deviceInfos != null) {
            String icon = null;
            String deviceVersion = null;
            String deviceName = null;
            String deviceId = null;

            Iterator<String> it = deviceInfos.iterator();
            while (it.hasNext()) {
                String line = (String) it.next();
                int split = line.indexOf('=');
                if (split > 0) {
                    String key = line.substring(0, split);
                    String val = line.substring(split + 1);
                    if ("id".equalsIgnoreCase(key)) {
                        deviceId = val;
                    } else if ("md".equalsIgnoreCase(key)) {
                        deviceName = val.replaceAll(
                                "(Eureka|Chromekey)( Dongle)?", "Dongle");
                    } else if ("ve".equalsIgnoreCase(key)) {
                        deviceVersion = val;
                    } else if ("ic".equalsIgnoreCase(key)) {
                        icon = val;
                    }
                }
            }

            if (deviceId == null) {
                return;
            }

            if (deviceName == null) {
                deviceName = mName;
            }

            final FlingDevice device;
            ScannerDeviceData scannerDeviceData;
            synchronized (mFoundDevices) {

                if ((info.mIpV4AddrList == null)
                        || (info.mIpV4AddrList.isEmpty())) {
                    mFoundDevices.remove(deviceId);
                    return;
                }

                Inet4Address v4Address = (Inet4Address) info.mIpV4AddrList
                        .get(0);
                ArrayList<WebImage> iconList = new ArrayList<WebImage>();
                if (icon != null) {
                    String address = v4Address.toString();
                    int split = address.indexOf('/');
                    if (split >= 0) {
                        address = address.substring(split + 1);
                    }

                    iconList.add(new WebImage(Uri.parse(String.format(
                            "http://%s:8008%s", address, icon))));
                }

                deviceId = deviceId + v4Address.getHostAddress();

                device = FlingDevice.Builder.create((String) deviceId,
                        v4Address);
                FlingDevice.setFriendlyName(device, info.mFriendlyName);
                FlingDevice.setModelName(device, deviceName);
                FlingDevice.setDeviceVersion(device, (String) deviceVersion);
                FlingDevice.setServicePort(device, info.mPort);
                FlingDevice.setIconList(device, iconList);
                FlingDevice.setFoundSource(device, FlingDevice.FOUND_SOURCE_MDNS);
                scannerDeviceData = (ScannerDeviceData) mFoundDevices
                        .get(deviceId);
                if (scannerDeviceData != null) {
                    if (device.equals(scannerDeviceData.mFlingDevice)) {
                        if (!scannerDeviceData.mIsOffline) {
                            scannerDeviceData.mScannedTime = SystemClock
                                    .elapsedRealtime();
                        }
                        return;
                    } else {
                        mFoundDevices.remove(deviceId);
                    }
                }

                mFoundDevices.put((String) deviceId, new ScannerDeviceData(
                        device, info.mTTL));
            }

            if (scannerDeviceData != null
                    && scannerDeviceData.mFlingDevice != null) {
                notifyDeviceOffline(scannerDeviceData.mFlingDevice);
            }

            if (device == null) {
                return;
            }

            log.d("notifyDeviceOnline: %s", device);

            final List<IDeviceScanListener> listenerList = getDeviceScannerListenerList();
            if (listenerList == null) {
                return;
            }

            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    Iterator<IDeviceScanListener> it = listenerList.iterator();
                    while (it.hasNext()) {
                        ((IDeviceScanListener) it.next())
                                .onDeviceOnline(device);
                    }
                }
            });
        }
    }

    /**
     * periodically check whether the scanned devices are expired.
     */
    private void scanLoop() {
        while (!mQuit) {
            try {
                Thread.sleep(3000L);
            } catch (InterruptedException e) {
                if (mQuit) {
                    break;
                }
            }

            synchronized (mFoundDevices) {
                long currentTime = SystemClock.elapsedRealtime();

                Iterator<Entry<String, ScannerDeviceData>> it = mFoundDevices
                        .entrySet().iterator();
                while (it.hasNext()) {
                    ScannerDeviceData deviceInfo = it.next().getValue();

                    int offline = 0;
                    if (currentTime - deviceInfo.mScannedTime < 60000L) {
                        offline = 0;
                    } else {
                        offline = 1;
                    }

                    if (offline == 0) {
                        continue;
                    }

                    final FlingDevice device = deviceInfo.mFlingDevice;

                    mHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            notifyDeviceOffline(device);
                        }

                    });

                    log.d("expired record for %s", device);

                    it.remove();
                }
            }
        }

        log.d("refreshLoop exiting");

        return;
    }

    private static final class ScannerDeviceData {
        FlingDevice mFlingDevice;
        long mScannedTime;
        long mTTl;
        boolean mIsOffline;

        ScannerDeviceData(FlingDevice device, long ttl) {
            mFlingDevice = device;
            mTTl = ttl;
            mScannedTime = SystemClock.elapsedRealtime();
        }
    }

    protected static final class FlingDeviceInfo {
        String mFQDN;
        List<Inet4Address> mIpV4AddrList;
        List<Inet6Address> mIpV6AddrList;
        String mName;
        String mFriendlyName;
        String mHost;
        int mProtocol; // 1:tcp, 2: udp
        int mPort;
        int mPriority;
        int mWeight;
        List<String> mTextStringList;
        long mTTL;

        public FlingDeviceInfo(String fqdn) {
            this.mFQDN = fqdn;
            this.mProtocol = 0;
            this.mTTL = -1L;
        }
    }

}
