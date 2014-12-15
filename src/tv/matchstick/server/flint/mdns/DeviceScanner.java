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

package tv.matchstick.server.flint.mdns;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import tv.matchstick.client.internal.LOG;
import tv.matchstick.flint.FlintDevice;
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
import android.os.SystemClock;
import android.util.Log;

public abstract class DeviceScanner {
    static final LOG log = new LOG("DeviceScanner");

    protected final Handler mHandler = new Handler(Looper.getMainLooper());

    protected final Context mContext;
    private final List<IDeviceScanListener> mListenerList = new ArrayList<IDeviceScanListener>();
    private final AtomicBoolean mGuard = new AtomicBoolean();
    private final ConnectivityManager mConnectivityManager;
    private BroadcastReceiver mConnectChangeReceiver;
    private final WifiManager mWifiManager;
    private String mBSSID;
    private boolean mScanning;
    private boolean m;
    private boolean mErrorState;

    protected final Map<String, ScannerDeviceData> mScannerData = new HashMap<String, ScannerDeviceData>();


    protected DeviceScanner(Context context) {
        mContext = context;
        mConnectivityManager = (ConnectivityManager) context
                .getSystemService("connectivity");
        mWifiManager = (WifiManager) context.getSystemService("wifi");
    }

    private static List<NetworkInterface> getFlintNetworkInterfaceList() {
        ArrayList<NetworkInterface> networkInterfaces = new ArrayList<NetworkInterface>();
        try {
            Enumeration<NetworkInterface> enumeration = NetworkInterface
                    .getNetworkInterfaces();
            if (enumeration != null) {

                while (enumeration.hasMoreElements()) {
                    NetworkInterface networkinterface = (NetworkInterface) enumeration
                            .nextElement();
                    if (networkinterface.isUp()
                            && !networkinterface.isLoopback()
                            && !networkinterface.isPointToPoint()
                            && networkinterface.supportsMulticast()) {
                        Iterator<InterfaceAddress> addresses = networkinterface
                                .getInterfaceAddresses().iterator();
                        while (addresses.hasNext()) {
                            if (((InterfaceAddress) addresses.next())
                                    .getAddress() instanceof Inet4Address)
                                networkInterfaces.add(networkinterface);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.d(e.toString(), "Exception while selecting network interface");
        }

        return networkInterfaces;
    }

    private void startScanInit() {
        log.d("startScanInit");

        mErrorState = false;
        checkBSSID();
        startScanInternal(getFlintNetworkInterfaceList());
    }

    private void stopScanInit() {
        stopScanInternal();
    }

    private void checkBSSID() {
        WifiInfo wifiinfo = mWifiManager.getConnectionInfo();
        String bssId = null;
        if (wifiinfo != null)
            bssId = wifiinfo.getBSSID();
        if (mBSSID == null || bssId == null || !mBSSID.equals(bssId)) {
            log.d("BSSID changed");
            onAllDevicesOffline();
        }
        mBSSID = bssId;
    }

    public final void startScan() {
        if (mScanning) {
            return;
        }

        mScanning = true;

        if (mConnectChangeReceiver == null) {
            mConnectChangeReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    NetworkInfo networkInfo = mConnectivityManager
                            .getActiveNetworkInfo();
                    boolean connected;
                    if ((networkInfo != null) && (networkInfo.isConnected())) {
                        connected = true;
                        log.d("connectivity state changed. connected? %b, errorState? %b",
                                connected, mErrorState);
                        checkBSSID();
                        if (!connected)
                            onAllDevicesOffline();
                        if (m) {
                            stopScanInit();
                            m = false;
                        }
                        if (connected) {
                            log.d("re-established connectivity after connectivity changed;  restarting scan");
                            startScanInit();

                            return;
                        }

                        if (mErrorState) {
                            return;
                        }

                        log.d("lost connectivity while scanning;");
                        reportError();
                    }
                }

            };

            IntentFilter intentfilter = new IntentFilter(
                    "android.net.conn.CONNECTIVITY_CHANGE");
            mContext.registerReceiver(mConnectChangeReceiver, intentfilter);
        }
        startScanInit();

        log.d("scan started");
    }

    public final void addListener(IDeviceScanListener listener) {
        if (listener == null)
            throw new IllegalArgumentException("listener cannot be null");
        synchronized (mListenerList) {
            if (mListenerList.contains(listener))
                throw new IllegalArgumentException(
                        "the same listener cannot be added twice");
            mListenerList.add(listener);
        }
    }

    protected final void notifyDeviceOffline(final FlintDevice device) {
        log.d("notifyDeviceOffline: %s", device);

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

    public abstract void setDeviceOffline(String deviceId);

    protected abstract void startScanInternal(List<NetworkInterface> list);

    public final void stopScan() {
        if (!mScanning) {
            return;
        }
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
        log.d("scan stopped");
    }

    protected abstract void stopScanInternal();

    public abstract void onAllDevicesOffline();

    protected List<IDeviceScanListener> getDeviceScannerListenerList() {
        synchronized (mListenerList) {
            if (mListenerList.isEmpty()) {
                return null;
            }
        }
        return mListenerList;
    }

    protected final void reportNetworkError() {
        if (!mErrorState) {
            log.d("reportNetworkError; errorState now true");

            mErrorState = true;

            onAllDevicesOffline();
        }
    }

    protected final void reportError() {
        if (!mGuard.getAndSet(true)) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    reportNetworkError();
                }
            });
        }
    }

    public class ScannerDeviceData {
        public FlintDevice mFlintDevice;
        public long mScannedTime;
        public long mTTl;
        public boolean mIsOffline;

        public ScannerDeviceData(FlintDevice device, long ttl) {
            mFlintDevice = device;
            mTTl = ttl;
            mScannedTime = SystemClock.elapsedRealtime();
        }
    }
}
