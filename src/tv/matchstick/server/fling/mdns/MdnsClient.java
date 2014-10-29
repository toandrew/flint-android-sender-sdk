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

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

import tv.matchstick.client.internal.LOG;
import tv.matchstick.server.fling.mdns.MdnsDeviceScanner.FlingDeviceInfo;
import android.util.Log;

abstract class MdnsClient {
    private static final LOG log = new LOG("MdnsClient");

    private final String mHostName;

    private final NetworkInterface mNetwork;

    private Inet4Address mAddress;

    private JmDNS mJmDNS;

    private Timer mDataTimer;

    private final static int RESCAN_INTERVAL = 10000;

    public MdnsClient(String hostName, NetworkInterface network) {
        mHostName = hostName;
        mNetwork = network;

        mAddress = getAddress(mNetwork);
    }

    ServiceListener mJmdnsListener = new ServiceListener() {

        @Override
        public void serviceAdded(ServiceEvent event) {
            // TODO Auto-generated method stub

            log.d("serviceAdded:" + event);

            // Required to force serviceResolved to be called again
            // (after the first search)
            mJmDNS.requestServiceInfo(event.getType(), event.getName(), 1);
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            // TODO Auto-generated method stub

        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            // TODO Auto-generated method stub

            Log.d("MdnsClient", "serviceResolved:" + event);

            // get device info
            FlingDeviceInfo deviceInfo = extractDeviceInfo(event);

            // notify device scan result: online or offline
            onScanResults(deviceInfo);
        }

        private FlingDeviceInfo extractDeviceInfo(ServiceEvent event) {
            FlingDeviceInfo deviceInfo = new FlingDeviceInfo(event.getType());

            deviceInfo.mName = event.getInfo().getApplication();
            deviceInfo.mFriendlyName = event.getInfo().getName();

            deviceInfo.mHost = event.getInfo().getServer();
            deviceInfo.mPort = event.getInfo().getPort();
            deviceInfo.mPriority = event.getInfo().getPriority();

            String protocol = event.getInfo().getProtocol();
            if (protocol.equalsIgnoreCase("tcp")) {
                deviceInfo.mProtocol = 1;
            } else {
                deviceInfo.mProtocol = 0;
            }

            deviceInfo.mWeight = event.getInfo().getWeight();
            deviceInfo.mTTL = 10; // ???

            java.util.Enumeration<String> names = event.getInfo()
                    .getPropertyNames();

            deviceInfo.mTextStringList = new ArrayList<String>();

            while (names.hasMoreElements()) {
                String name = (String) names.nextElement();
                String element = name + "="
                        + event.getInfo().getPropertyString(name);
                deviceInfo.mTextStringList.add(element);
            }

            deviceInfo.mIpV4AddrList = Arrays.asList(event.getInfo()
                    .getInet4Addresses());
            deviceInfo.mIpV6AddrList = Arrays.asList(event.getInfo()
                    .getInet6Addresses());

            return deviceInfo;
        }

    };

    /**
     * Called when device scanning finished.
     * 
     * @param deviceInfo
     */
    protected abstract void onScanResults(FlingDeviceInfo deviceInfo);

    /**
     * Start device scan
     */
    public final synchronized void startScan() {
        stopScan();

        mDataTimer = new Timer();
        MDNSSearchTask sendSearch = new MDNSSearchTask();
        mDataTimer.schedule(sendSearch, 100, RESCAN_INTERVAL);

    }

    /**
     * Stop device scan
     */
    public final synchronized void stopScan() {
        if (mDataTimer != null) {
            mDataTimer.cancel();
        }

        if (mJmDNS != null) {
            try {
                mJmDNS.removeServiceListener(mHostName, mJmdnsListener);

                mJmDNS.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Inet4Address getAddress(final NetworkInterface networkInterface) {

        try {
            for (Enumeration<NetworkInterface> e = NetworkInterface
                    .getNetworkInterfaces(); e.hasMoreElements();) {
                NetworkInterface item = e.nextElement();

                for (InterfaceAddress address : item.getInterfaceAddresses()) {
                    if (address.getAddress() instanceof Inet4Address) {
                        Inet4Address inet4Address = (Inet4Address) address
                                .getAddress();
                        if (!inet4Address.isLoopbackAddress()) {
                            return inet4Address;
                        }
                    }
                }
            }
        } catch (IOException ex) {

        }

        return null;
    }

    private class MDNSSearchTask extends TimerTask {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            if (mNetwork != null) {
                try {
                    if (mJmDNS != null) {
                        mJmDNS.close();
                    }

                    if (mAddress != null) {
                        Log.e("MdnsClient", "address:" + mAddress);
                        mJmDNS = JmDNS.create(mAddress, mHostName);
                    } else {
                        Log.e("MdnsClient", "address is null??!!!");
                        mJmDNS = JmDNS.create(mHostName);
                    }

                    mJmDNS.addServiceListener(mHostName, mJmdnsListener);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
