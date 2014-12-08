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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import tv.matchstick.client.internal.LOG;
import tv.matchstick.fling.FlingDevice;
import tv.matchstick.fling.service.FlingDeviceService;
import tv.matchstick.server.common.checker.MainThreadChecker;
import tv.matchstick.server.common.checker.ObjEqualChecker;
import tv.matchstick.server.fling.mdns.DeviceScanner;
import tv.matchstick.server.fling.mdns.IDeviceScanListener;
import tv.matchstick.server.fling.mdns.MdnsDeviceScanner;
import tv.matchstick.server.fling.media.RouteController;
import tv.matchstick.server.fling.ssdp.SsdpDeviceScanner;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;

public class FlingMediaRouteProvider extends MediaRouteProvider {
    private static final LOG log = new LOG("FlingMediaRouteProvider");

    private static FlingMediaRouteProvider mInstance;

    private final Map<String, DiscoveryCriteriaHelper> mDiscoveryCriteriaMap = new HashMap<String, DiscoveryCriteriaHelper>();
    private final Map<String, FlingDeviceControllerHelper> mFlingDeviceControllerMap = new HashMap<String, FlingDeviceControllerHelper>();
    private final DeviceFilter mFlingDeviceFilter;
    private final Set<DiscoveryCriteria> mDiscoveryCriterias = new HashSet<DiscoveryCriteria>();

    private final DeviceScanner mMdnsDeviceScanner;
    private final DeviceScanner mSsdpDeviceScanner;

    private final IDeviceScanListener mDeviceScannerListener = new IDeviceScanListener() {

        @Override
        public void onAllDevicesOffline() {
            log.d("DeviceScanner.Listener#onAllDevicesOffline");
            Iterator iterator = mDiscoveryCriteriaMap.entrySet().iterator();
            while (iterator.hasNext()) {
                DiscoveryCriteriaHelper helper = (DiscoveryCriteriaHelper) ((Entry) iterator
                        .next()).getValue();
                if (helper != null) {
                    FlingDevice flingdevice = helper.mFlingDevice;
                    FlingDeviceControllerHelper controller = (FlingDeviceControllerHelper) mFlingDeviceControllerMap
                            .get(flingdevice.getDeviceId());
                    if (controller != null) {
                        controller.isValid = false;
                        log.d("device %s is in use; not removing route",
                                flingdevice);
                    } else {
                        iterator.remove();
                    }
                } else {
                    iterator.remove();
                }
            }

            publishRoutes();
        }

        @Override
        public void onDeviceOnline(FlingDevice flingdevice) {

            log.d("DeviceScanner.Listener#onDeviceOnline :%s", flingdevice);

            FlingDeviceControllerHelper helper = (FlingDeviceControllerHelper) mFlingDeviceControllerMap
                    .get(flingdevice.getDeviceId());
            if (helper != null) {
                helper.isValid = true;
            }

            mFlingDeviceFilter.connectOrAcceptDevice(flingdevice);
        }

        @Override
        public void onDeviceOffline(FlingDevice flingdevice) {
            log.d("DeviceScanner.Listener#onDeviceOffline :%s", flingdevice);

            FlingDeviceControllerHelper helper = (FlingDeviceControllerHelper) mFlingDeviceControllerMap
                    .get(flingdevice.getDeviceId());
            if (helper != null) {
                helper.isValid = false;
            } else {
                removeCriteria(flingdevice);
                publishRoutes();
            }
        }
    };

    public Map<String, FlingDeviceControllerHelper> getFlingDeviceControllerMap() {
        return mFlingDeviceControllerMap;
    }

    private FlingMediaRouteProvider(Context context) {
        super(context);

        mFlingDeviceFilter = new DeviceFilter(mDiscoveryCriterias) {

            @Override
            protected void setDeviceOffline(FlingDevice flingdevice) {
                if (flingdevice.getFoundSource().equals(
                        FlingDevice.FOUND_SOURCE_MDNS))
                    mMdnsDeviceScanner.setDeviceOffline(flingdevice
                            .getDeviceId());
                else
                    mSsdpDeviceScanner.setDeviceOffline(flingdevice
                            .getDeviceId());
            }

            @Override
            protected void onDeviceAccepted(FlingDevice flingdevice, Set set) {
                log().d("DeviceFilter#onDeviceAccepted: %s", flingdevice);

                addFlingDevice(flingdevice, set);

                publishRoutes();
            }
        };

        mMdnsDeviceScanner = new MdnsDeviceScanner(context);
        mMdnsDeviceScanner.addListener(mDeviceScannerListener);
        mSsdpDeviceScanner = new SsdpDeviceScanner(context);
        mSsdpDeviceScanner.addListener(mDeviceScannerListener);

        publishRoutes();
    }

    public static LOG log() {
        return log;
    }

    public static synchronized FlingMediaRouteProvider getInstance(
            Context context) {
        if (mInstance == null) {
            mInstance = new FlingMediaRouteProvider(
                    context.getApplicationContext());
        }

        return mInstance;
    }

    public void setDeviceControllerListener(FlingRouteController controller) {
        FlingDevice flingdevice = controller.getFlingDevice();
        String id = flingdevice.getDeviceId();
        FlingDeviceControllerHelper helper = (FlingDeviceControllerHelper) mFlingDeviceControllerMap
                .get(id);
        if (helper == null) {
            helper = new FlingDeviceControllerHelper();
            log.d("set FlingDeviceController Listener %s", flingdevice);
            helper.isConnecting = true;
            mFlingDeviceControllerMap.put(id, helper);
        }
        helper.mFlingRouteControllerList.add(controller);
    }

    public static String getFriendlyName(FlingDevice flingdevice) {
        return flingdevice.getFriendlyName();
    }

    private MediaRouteDescriptor buildRouteDescriptorForDevice(
            DiscoveryCriteriaHelper criteriaHelper) {
        FlingDevice flingdevice = criteriaHelper.mFlingDevice;
        Set set = criteriaHelper.mDiscoveryCriteriaSet;
        FlingDeviceControllerHelper controllerHelper = (FlingDeviceControllerHelper) mFlingDeviceControllerMap
                .get(flingdevice.getDeviceId());
        boolean isConnecting;
        if (controllerHelper != null) {
            isConnecting = controllerHelper.isConnecting;
        } else {
            isConnecting = false;
        }
        Bundle bundle = new Bundle();
        flingdevice.putInBundle(bundle);
        ArrayList<IntentFilter> arraylist = new ArrayList<IntentFilter>();
        Iterator iterator = set.iterator();
        while (iterator.hasNext()) {
            DiscoveryCriteria criteria = (DiscoveryCriteria) iterator.next();
            IntentFilter filter = new IntentFilter();
            String category = criteria.mCategory;
            filter.addCategory(category);
            arraylist.add(filter);
        }

        log.d("buildRouteDescriptorForDevice: id=%s, description=%s, connecting=%b",
                flingdevice.getDeviceId(), flingdevice.getFriendlyName(),
                isConnecting);

        MediaRouteDescriptorPrivateData data = new MediaRouteDescriptorPrivateData(
                flingdevice.getDeviceId(), flingdevice.getFriendlyName());
        data.mBundle.putString("status", flingdevice.getModelName());
        data.mBundle.putBoolean("connecting", isConnecting);
        data.mBundle.putInt("volumeHandling", 0);
        data.mBundle.putInt("volume", 0);
        data.mBundle.putInt("volumeMax", 20);
        data.mBundle.putInt("playbackType", 1);
        MediaRouteDescriptorPrivateData privateData = data
                .addIntentFilterList(arraylist);
        privateData.mBundle.putBundle("extras", bundle);
        if (privateData.mControlIntentFilterList != null) {
            privateData.mBundle.putParcelableArrayList("controlFilters",
                    privateData.mControlIntentFilterList);
        }

        return new MediaRouteDescriptor(privateData.mBundle,
                privateData.mControlIntentFilterList);
    }

    void addFlingDevice(FlingDevice flingdevice, Set set) {
        DiscoveryCriteriaHelper criteriaHelper = (DiscoveryCriteriaHelper) mDiscoveryCriteriaMap
                .get(flingdevice.getDeviceId());
        if (criteriaHelper != null) {
            log.d("merging in criteria for existing device %s",
                    flingdevice.getFriendlyName());
            Iterator iterator = set.iterator();
            while (iterator.hasNext()) {
                DiscoveryCriteria criteria = (DiscoveryCriteria) iterator
                        .next();
                if (!criteriaHelper.mDiscoveryCriteriaSet.contains(criteria))
                    criteriaHelper.mDiscoveryCriteriaSet.add(criteria);
            }
        } else {
            mDiscoveryCriteriaMap.put(flingdevice.getDeviceId(),
                    new DiscoveryCriteriaHelper(flingdevice, set));
        }
    }

    private void publishRoutes() {
        ArrayList<MediaRouteDescriptor> routeList = new ArrayList<MediaRouteDescriptor>();
        Iterator iterator = mDiscoveryCriteriaMap.values().iterator();
        while (iterator.hasNext()) {
            routeList
                    .add(buildRouteDescriptorForDevice((DiscoveryCriteriaHelper) iterator
                            .next()));
        }
        MediaRouteProviderDescriptor providerDescriptor = buildMediaRouteProviderDescriptor(routeList);

        MainThreadChecker.isOnAppMainThread();

        if (mMediaRouteProviderDescriptor != providerDescriptor) {
            mMediaRouteProviderDescriptor = providerDescriptor;
            if (!mPendingDescriptorChange) {
                mPendingDescriptorChange = true;
                mHandler.sendEmptyMessage(MSG_DELIVER_DESCRIPTOR_CHANGED);
            }
        }

        log.d("published %d routes", routeList.size());
    }

    private MediaRouteProviderDescriptor buildMediaRouteProviderDescriptor(
            ArrayList<MediaRouteDescriptor> routeList) {
        ArrayList<MediaRouteDescriptor> list = new ArrayList<MediaRouteDescriptor>();
        Bundle bundle = new Bundle();

        if (routeList == null) {
            throw new IllegalArgumentException("routes must not be null");
        }

        Iterator iterator = routeList.iterator();
        while (iterator.hasNext()) {
            MediaRouteDescriptor descriptor = (MediaRouteDescriptor) iterator
                    .next();
            if (descriptor == null) {
                throw new IllegalArgumentException("route must not be null");
            }

            if (list.contains(descriptor)) {
                throw new IllegalArgumentException(
                        "route descriptor already added");
            }

            list.add(descriptor);
        }
        if (list != null) {
            int size = list.size();
            ArrayList routes = new ArrayList(size);
            for (int i = 0; i < size; i++) {
                routes.add(((MediaRouteDescriptor) list.get(i)).mData);
            }

            bundle.putParcelableArrayList("routes", routes);
        }
        return new MediaRouteProviderDescriptor(bundle, list);
    }

    public void onUnselect(FlingRouteController controller) {
        FlingDevice flingdevice = controller.getFlingDevice();
        String id = flingdevice.getDeviceId();
        FlingDeviceControllerHelper helper = (FlingDeviceControllerHelper) mFlingDeviceControllerMap
                .get(id);
        if (helper != null) {
            helper.mFlingRouteControllerList.remove(controller);
            if (helper.isEmpty()) {
                log.d("disposing FlingDeviceController for %s", flingdevice);

                if (helper.isOffline) {
                    if (flingdevice.getFoundSource().equals(
                            FlingDevice.FOUND_SOURCE_MDNS))
                        mMdnsDeviceScanner.setDeviceOffline(id);
                    else
                        mSsdpDeviceScanner.setDeviceOffline(id);
                }

                if (!helper.isValid || helper.isOffline) {
                    removeCriteria(flingdevice);
                    publishRoutes();
                }

                mFlingDeviceControllerMap.remove(id);
            }
        }
    }

    private void removeCriteria(FlingDevice flingdevice) {
        mDiscoveryCriteriaMap.remove(flingdevice.getDeviceId());
    }

    private void onDiscoveryRequestChanged() {
        boolean isStartScan, flag1, flag2;
        HashSet<DiscoveryCriteria> hashset = new HashSet<DiscoveryCriteria>(
                mDiscoveryCriterias);
        DiscoveryRequest request;
        isStartScan = true;
        mDiscoveryCriterias.clear();
        request = super.mDiscoveryRequest;
        if (request == null) {
            flag1 = false;
        } else {
            List list;
            int size;
            int index;
            list = request.getSelector().getControlCategories();
            size = list.size();
            index = 0;
            flag1 = false;
            while (index < size) {
                String category = (String) list.get(index);
                if (!category
                        .equals("tv.matchstick.fling.CATEGORY_FLING_REMOTE_PLAYBACK")
                        && !category
                                .startsWith("tv.matchstick.fling.CATEGORY_FLING_REMOTE_PLAYBACK/")
                        && !category
                                .equals("tv.matchstick.fling.CATEGORY_FLING")
                        && !category
                                .startsWith("tv.matchstick.fling.CATEGORY_FLING/")) {
                    flag2 = flag1;
                } else {
                    try {
                        DiscoveryCriteria criteria = DiscoveryCriteria
                                .getDiscoveryCriteria(category);
                        mDiscoveryCriterias.add(criteria);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } finally {
                        flag2 = isStartScan;
                    }
                }

                index++;
                flag1 = flag2;
            }
        }
        isStartScan = flag1;

        if (hashset.equals(mDiscoveryCriterias)) {
            isStartScan = false;
        } else {
            mFlingDeviceFilter.reset(mDiscoveryCriterias);
        }

        if (isStartScan) {
            log.d("starting the scan");

            FlingDeviceService.stopScanFlingDevice(super.mContext,
                    mMdnsDeviceScanner);
            FlingDeviceService.startScanFlingDevice(super.mContext,
                    mMdnsDeviceScanner);

            FlingDeviceService.stopScanFlingDevice(super.mContext,
                    mSsdpDeviceScanner);
            FlingDeviceService.startScanFlingDevice(super.mContext,
                    mSsdpDeviceScanner);

            return;
        }

        log.d("stopping the scan");

        FlingDeviceService.stopScanFlingDevice(super.mContext,
                mMdnsDeviceScanner);
        FlingDeviceService.stopScanFlingDevice(super.mContext,
                mSsdpDeviceScanner);
    }

    public final RouteController getRouteController(String routeId) {
        DiscoveryCriteriaHelper helper = (DiscoveryCriteriaHelper) mDiscoveryCriteriaMap
                .get(routeId);

        if (helper == null) {
            return null;
        }

        return new FlingRouteController(this, helper.mFlingDevice);
    }

    public final void onDiscoveryRequestChanged(DiscoveryRequest request) {
        log.d("in onDiscoveryRequestChanged: request=%s", request);

        onDiscoveryRequestChanged();
    }

    final class DiscoveryCriteriaHelper {
        final FlingDevice mFlingDevice;
        final Set<DiscoveryCriteria> mDiscoveryCriteriaSet;

        public DiscoveryCriteriaHelper(FlingDevice flingdevice,
                Set<DiscoveryCriteria> set) {
            mFlingDevice = flingdevice;
            mDiscoveryCriteriaSet = set;
        }

        @Override
        public final boolean equals(Object obj) {
            boolean flag = true;
            if (obj == null || !(obj instanceof DiscoveryCriteriaHelper))
                flag = false;
            else if (obj != this) {
                DiscoveryCriteriaHelper helper = (DiscoveryCriteriaHelper) obj;
                if (!ObjEqualChecker
                        .isEquals(mFlingDevice, helper.mFlingDevice)
                        || !ObjEqualChecker.isEquals(mDiscoveryCriteriaSet,
                                helper.mDiscoveryCriteriaSet))
                    return false;
            }
            return flag;
        }

        @Override
        public final int hashCode() {
            Object aobj[] = new Object[2];
            aobj[0] = mFlingDevice;
            aobj[1] = mDiscoveryCriteriaSet;
            return Arrays.hashCode(aobj);
        }
    }

    final class FlingDeviceControllerHelper {
        public boolean isValid;
        public boolean isConnecting;
        public boolean isOffline;
        public final List<FlingRouteController> mFlingRouteControllerList = new ArrayList<FlingRouteController>();

        public FlingDeviceControllerHelper() {
            super();
            isValid = true;
        }

        public final boolean isEmpty() {
            return mFlingRouteControllerList.isEmpty();
        }

    }

    final class MediaRouteDescriptorPrivateData {
        public final Bundle mBundle = new Bundle();
        public ArrayList mControlIntentFilterList;

        public MediaRouteDescriptorPrivateData(String id, String name) {
            mBundle.putString("id", id);
            mBundle.putString("name", name);
        }

        public final MediaRouteDescriptorPrivateData addIntentFilterList(
                Collection collection) {
            if (collection == null)
                throw new IllegalArgumentException("filters must not be null");
            if (!collection.isEmpty()) {
                Iterator iterator = collection.iterator();
                do {
                    if (!iterator.hasNext()) {
                        break;
                    }
                    IntentFilter filter = (IntentFilter) iterator.next();

                    if (filter == null) {
                        throw new IllegalArgumentException(
                                "filter must not be null");
                    }

                    if (mControlIntentFilterList == null) {
                        mControlIntentFilterList = new ArrayList();
                    }

                    if (!mControlIntentFilterList.contains(filter)) {
                        mControlIntentFilterList.add(filter);
                    }
                } while (true);
            }
            return this;
        }
    }

}
