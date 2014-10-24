package tv.matchstick.server.fling.mdns;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.net.InterfaceAddress;

import tv.matchstick.fling.FlingDevice;
import tv.matchstick.server.utils.LOG;

public abstract class DeviceScanner {
    static final LOG mLogs = new LOG("DeviceScanner");
    final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Context mContext;
    private final List<IDeviceScanListener> mListenerList = new ArrayList<IDeviceScanListener>();
    private final AtomicBoolean f = new AtomicBoolean();
    private final ConnectivityManager mConnectivityManager;
    private BroadcastReceiver mConnectChangeReceiver;
    private final WifiManager mWifiManager;
    private String mBSSID;
    private boolean mScanning;
    private boolean m;
    private boolean mErrorState;

    protected DeviceScanner(Context context) {
        mContext = context;
        mConnectivityManager = (ConnectivityManager) context
                .getSystemService("connectivity");
        mWifiManager = (WifiManager) context.getSystemService("wifi");
    }

    static LOG getLogs() {
        return mLogs;
    }

    private static List<NetworkInterface> getFlingNetworkInterfaceList() {
        ArrayList<NetworkInterface> arraylist = new ArrayList<NetworkInterface>();
        try {
            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            if (enumeration != null) {

                while (enumeration.hasMoreElements()) {
                    NetworkInterface networkinterface = (NetworkInterface) enumeration
                            .nextElement();
                    if (networkinterface.isUp()
                            && !networkinterface.isLoopback()
                            && !networkinterface.isPointToPoint()
                            && networkinterface.supportsMulticast()) {
                        Iterator<InterfaceAddress> iterator = networkinterface
                                .getInterfaceAddresses().iterator();
                        while (iterator.hasNext()) {
                            if (((InterfaceAddress) iterator.next())
                                    .getAddress() instanceof Inet4Address)
                                arraylist.add(networkinterface);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            mLogs.d(e, "Exception while selecting network interface",
                    new Object[0]);
        }
        return arraylist;
    }

    private void startScanInit() {
        mLogs.d("startScanInit");
        mErrorState = false;
        checkBSSID();
        startScanInternal(getFlingNetworkInterfaceList());
    }

    private void stopScanInit() {
        mLogs.d("stopScanInit");
        stopScanInternal();
    }

    private void checkBSSID() {
        WifiInfo wifiinfo = mWifiManager.getConnectionInfo();
        String bssId = null;
        if (wifiinfo != null)
            bssId = wifiinfo.getBSSID();
        if (mBSSID == null || bssId == null || !mBSSID.equals(bssId)) {
            mLogs.d("BSSID changed", new Object[0]);
            onAllDevicesOffline();
        }
        mBSSID = bssId;
    }

    public final void startScan() {
        if (mScanning)
            return;
        mScanning = true;
        if (mConnectChangeReceiver == null) {
            mConnectChangeReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    NetworkInfo localNetworkInfo = mConnectivityManager
                            .getActiveNetworkInfo();
                    boolean connected;
                    if ((localNetworkInfo != null)
                            && (localNetworkInfo.isConnected())) {
                        connected = true;
                        DeviceScanner
                                .getLogs()
                                .d("connectivity state changed. connected? %b, errorState? %b",
                                        connected, mErrorState);
                        checkBSSID();
                        if (!connected)
                            onAllDevicesOffline();
                        if (m) {
                            stopScanInit();
                            m = false;
                        }
                        if (connected) {
                            DeviceScanner
                                    .getLogs()
                                    .d("re-established connectivity after connectivity changed;  restarting scan");
                            startScanInit();

                            return;
                        }

                        if (mErrorState) {
                            return;
                        }

                        DeviceScanner.getLogs().d(
                                "lost connectivity while scanning;");
                        g();
                    }
                }

            };

            IntentFilter intentfilter = new IntentFilter(
                    "android.net.conn.CONNECTIVITY_CHANGE");
            mContext.registerReceiver(mConnectChangeReceiver, intentfilter);
        }
        startScanInit();

        mLogs.d("scan started");
    }

    public final void addListener(IDeviceScanListener auy) {
        if (auy == null)
            throw new IllegalArgumentException("listener cannot be null");
        synchronized (mListenerList) {
            if (mListenerList.contains(auy))
                throw new IllegalArgumentException(
                        "the same listener cannot be added twice");
            mListenerList.add(auy);
        }
    }

    protected final void notifyDeviceOffline(final FlingDevice device) {
        mLogs.d("notifyDeviceOffline: %s", device);

        final List<IDeviceScanListener> list = getDeviceScannerListenerList();

        if (list != null) {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    for (IDeviceScanListener listener : list) {
                        listener.onDeviceOffline(device);
                    }
                }
            });
        }
    }

    public abstract void setDeviceOffline(String s);

    protected abstract void startScanInternal(List<NetworkInterface> list);

    public final void stopScan() {
        if (!mScanning)
            return;
        if (mConnectChangeReceiver != null) {
            try {
                mContext.unregisterReceiver(mConnectChangeReceiver);
            } catch (IllegalArgumentException illegalargumentexception) {
            }
            mConnectChangeReceiver = null;
        }
        stopScanInit();
        m = false;
        mHandler.removeCallbacksAndMessages(null);
        mScanning = false;
        mLogs.d("scan stopped");
    }

    protected abstract void stopScanInternal();

    public abstract void onAllDevicesOffline();

    List<IDeviceScanListener> getDeviceScannerListenerList() {
        synchronized (mListenerList) {
            boolean flag = mListenerList.isEmpty();
            if (flag) {
                return null;
            }
        }
        return mListenerList;
    }

    protected final void reportNetworkError() {
        if (!mErrorState) {
            mLogs.d("reportNetworkError; errorState now true");
            mErrorState = true;
            onAllDevicesOffline();
        }
    }

    protected final void g() {
        if (!f.getAndSet(true)) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    reportNetworkError();
                }
            });
        }
    }
}
