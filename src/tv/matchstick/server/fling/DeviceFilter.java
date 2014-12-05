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

package tv.matchstick.server.fling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tv.matchstick.client.internal.LOG;
import tv.matchstick.fling.FlingDevice;
import tv.matchstick.server.fling.socket.FlingSocketListener;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * This is used to filter device
 */
abstract class DeviceFilter {
    private static final LOG log = new LOG("DeviceFilter");
    private final List<FlingDeviceManager> mDeviceConnections = new ArrayList<FlingDeviceManager>();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Set<DiscoveryCriteria> mDiscoveryCriterias;

    public DeviceFilter(Set<DiscoveryCriteria> set) {
        mDiscoveryCriterias = new HashSet<DiscoveryCriteria>(set);
    }

    protected abstract void setDeviceOffline(FlingDevice flingdevice);

    protected abstract void onDeviceAccepted(FlingDevice flingdevice, Set set);

    /**
     * Reset discovery criteria.
     * 
     * @param criterias
     */
    public final void reset(Set<DiscoveryCriteria> criterias) {
        Iterator<FlingDeviceManager> iterator = mDeviceConnections.iterator();
        while (iterator.hasNext()) {
            ((FlingDeviceManager) iterator.next()).mIsConnecting = false;
        }

        mDeviceConnections.clear();

        mDiscoveryCriterias = new HashSet<DiscoveryCriteria>(criterias);
    }

    /**
     * Connect or accept device
     * 
     * @param device
     */
    public final void connectOrAcceptDevice(FlingDevice device) {
        FlingDeviceManager manager = new FlingDeviceManager(this, device);
        manager.acceptDevice(manager.mFlingDevice,
                manager.mDeviceFilter.mDiscoveryCriterias);
        mDeviceConnections.add(manager);
    }

    private final class FlingDeviceManager implements FlingSocketListener {
        final FlingDevice mFlingDevice;
        boolean mNoApp;
        boolean mNoNamespace;
        boolean mIsConnecting;
        final DeviceFilter mDeviceFilter;
        private final AppInfoHelper mAppInfoHelper = new AppInfoHelper();
        private final JSONArray mApplicationIds = new JSONArray();

        public FlingDeviceManager(DeviceFilter deviceFilter,
                FlingDevice flingDevice) {
            super();

            mDeviceFilter = deviceFilter;

            mNoApp = true;
            mNoNamespace = false;
            mIsConnecting = true;
            mFlingDevice = flingDevice;

            if (mDiscoveryCriterias.size() > 0) {
                boolean noNameSpace = true;
                boolean noApp = true;

                Iterator<DiscoveryCriteria> iterator = mDiscoveryCriterias
                        .iterator();
                while (iterator.hasNext()) {
                    DiscoveryCriteria criteria = (DiscoveryCriteria) iterator
                            .next();
                    Set namespaces = Collections
                            .unmodifiableSet(criteria.mNamespaceList);
                    if (criteria.mAppid != null) {
                        mApplicationIds.put(criteria.mAppid);
                        noApp = false;
                    }
                    if (namespaces.size() > 0) {
                        noNameSpace = false;
                    }
                }

                if (noApp) {
                    setNoApp();
                }

                if (noNameSpace) {
                    setNoNameSpace();
                }
            }
        }

        private void setNoApp() {
            mNoApp = true;
            if (mNoApp && mNoNamespace) {
                updateStatus();
            }
        }

        private void setNoNameSpace() {
            mNoNamespace = true;
            if (mNoApp && mNoNamespace) {
                updateStatus();
            }
        }

        private void updateStatus() {
            HashSet<DiscoveryCriteria> hashset = new HashSet<DiscoveryCriteria>();
            Iterator<DiscoveryCriteria> iterator = mDiscoveryCriterias
                    .iterator();
            while (iterator.hasNext()) {
                DiscoveryCriteria criteria = (DiscoveryCriteria) iterator
                        .next();
                boolean flag = false;
                if (mAppInfoHelper.mAppNamespaceList.containsAll(Collections
                        .unmodifiableSet(criteria.mNamespaceList))) {
                    flag = true;
                }

                if (flag) {
                    hashset.add(criteria);
                }
            }

            if (mIsConnecting && hashset.size() > 0) {
                acceptDevice(mFlingDevice, hashset);
            } else {
                log.d("rejected device: %s", mFlingDevice);
            }
        }

        @Override
        public final void onConnected() {

        }

        @Override
        public final void onConnectionFailed() {
            log.w("Connection to %s:%d (%s) failed with error net work",
                    mFlingDevice.getIpAddress().toString(),
                    mFlingDevice.getServicePort(),
                    mFlingDevice.getFriendlyName());

            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    mDeviceFilter.setDeviceOffline(mFlingDevice);
                }

            });
        }

        final void acceptDevice(final FlingDevice flingdevice, final Set set) {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    mDeviceFilter.onDeviceAccepted(flingdevice, set);
                }

            });
        }

        @Override
        public final void onDisconnected(int reason) {
            log.d("Device filter disconnected:" + reason);
        }

        private final class AppInfoHelper {
            final Set<String> mAppNamespaceList;
            final Set<String> mAppAvailabityList;

            private AppInfoHelper() {
                mAppNamespaceList = new HashSet<String>();
                mAppAvailabityList = new HashSet<String>();
            }

            public final void fillNamespaceList(JSONObject jsonobject) {
                try {
                    JSONArray applications = jsonobject.getJSONObject("status")
                            .getJSONArray("applications");
                    for (int i = 0; i < applications.length(); i++) {
                        JSONArray namespaces = applications.getJSONObject(i)
                                .getJSONArray("namespaces");
                        for (int j = 0; j < namespaces.length(); j++) {
                            mAppNamespaceList.add(namespaces.getJSONObject(j)
                                    .getString("name"));
                        }

                    }
                } catch (JSONException e) {
                    e.printStackTrace();

                    log.d("No namespaces found in receiver response: %s",
                            e.getMessage());
                }
            }

            public final void fillAppAvailabityList(JSONObject obj) {
                try {
                    JSONObject availability = obj.getJSONObject("availability");
                    Iterator<String> iterator = availability.keys();
                    while (iterator.hasNext()) {
                        String appId = (String) iterator.next();
                        if ("APP_AVAILABLE".equals(availability
                                .optString(appId))) {
                            mAppAvailabityList.add(appId);
                        }
                    }
                } catch (JSONException e) {
                    log.d("No app availabities found in receiver response: %s",
                            e.getMessage());
                }
            }
        }

        @Override
        public void onMessageReceived(String message) {
            Log.d("DeviceFilter", "onMessageReceived:" + message);

            try {
                JSONObject obj = new JSONObject(message);
                long requestId = obj.optLong("requestId", -1L);
                if (requestId == -1L) {
                    return;
                }
                if (requestId != 1L) {
                    if (requestId != 2L) {
                        log.d("Unrecognized request ID: " + requestId);
                        return;
                    }
                    mAppInfoHelper.fillNamespaceList(obj);
                    setNoNameSpace();
                    return;
                }
                mAppInfoHelper.fillAppAvailabityList(obj);
                setNoApp();
            } catch (JSONException e) {
                log.e("Failed to parse response: %s", e.getMessage());
            }

        }
    }

}
