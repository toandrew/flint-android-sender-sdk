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

package tv.matchstick.server.flint;

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
import tv.matchstick.flint.FlintDevice;
import tv.matchstick.flint.service.FlintDeviceService;
import tv.matchstick.server.common.checker.MainThreadChecker;
import tv.matchstick.server.common.checker.ObjEqualChecker;
import tv.matchstick.server.flint.mdns.DeviceScanner;
import tv.matchstick.server.flint.mdns.IDeviceScanListener;
import tv.matchstick.server.flint.mdns.MdnsDeviceScanner;
import tv.matchstick.server.flint.media.RouteController;
import tv.matchstick.server.flint.ssdp.SsdpDeviceScanner;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;

public class FlintMediaRouteProvider extends MediaRouteProvider {
    private static final LOG log = new LOG("FlintMediaRouteProvider");

    private static FlintMediaRouteProvider mInstance;

    private FlintDevice mCurrentDevice;

    private final Map<String, DiscoveryCriteriaHelper> mDiscoveryCriteriaMap = new HashMap<String, DiscoveryCriteriaHelper>();
    private final Map<String, FlintDeviceControllerHelper> mFlintDeviceControllerMap = new HashMap<String, FlintDeviceControllerHelper>();
    private final DeviceFilter mFlintDeviceFilter;
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
                    FlintDevice flintdevice = helper.mFlintDevice;
                    FlintDeviceControllerHelper controller = (FlintDeviceControllerHelper) mFlintDeviceControllerMap
                            .get(flintdevice.getDeviceId());
                    if (controller != null) {
                        controller.isValid = false;
                        log.d("device %s is in use; not removing route",
                                flintdevice);
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
        public void onDeviceOnline(FlintDevice flintdevice) {
            log.d("DeviceScanner.Listener#onDeviceOnline :%s", flintdevice);

            FlintDeviceControllerHelper helper = (FlintDeviceControllerHelper) mFlintDeviceControllerMap
                    .get(flintdevice.getDeviceId());
            if (helper != null) {
                helper.isValid = true;
            }

            mFlintDeviceFilter.connectOrAcceptDevice(flintdevice);
        }

        @Override
        public void onDeviceOffline(FlintDevice flintdevice) {
            log.d("DeviceScanner.Listener#onDeviceOffline :%s", flintdevice);
            if (flintdevice.getFoundSource().equals(FlintDevice.FOUND_SOURCE_MDNS)) {
                if (mSsdpDeviceScanner.hasDevice(flintdevice.getDeviceId())) {
                    log.d("ssdp has device, return");
                    return;
                }
            } else if (flintdevice.getFoundSource().equals(FlintDevice.FOUND_SOURCE_SSDP)) {
                if (mMdnsDeviceScanner.hasDevice(flintdevice.getDeviceId())) {
                    log.d("mdns has device, return");
                    return;
                }
            }
            log.d("all scanner offline, remove");
            FlintDeviceControllerHelper helper = (FlintDeviceControllerHelper) mFlintDeviceControllerMap
                    .get(flintdevice.getDeviceId());
            if (helper != null) {
                helper.isValid = false;
            } else {
                removeCriteria(flintdevice);
                publishRoutes();
            }
        }
    };

    public Map<String, FlintDeviceControllerHelper> getFlintDeviceControllerMap() {
        return mFlintDeviceControllerMap;
    }

    private FlintMediaRouteProvider(Context context) {
        super(context);

        mFlintDeviceFilter = new DeviceFilter(mDiscoveryCriterias) {

            @Override
            protected void setDeviceOffline(FlintDevice flintdevice) {
                if (flintdevice.getFoundSource().equals(
                        FlintDevice.FOUND_SOURCE_MDNS))
                    mMdnsDeviceScanner.setDeviceOffline(flintdevice
                            .getDeviceId());
                else if (flintdevice.getFoundSource().equals(
                        FlintDevice.FOUND_SOURCE_SSDP))
                    mSsdpDeviceScanner.setDeviceOffline(flintdevice
                            .getDeviceId());
            }

            @Override
            protected void onDeviceAccepted(FlintDevice flintdevice, Set set) {
                log().d("DeviceFilter#onDeviceAccepted: %s", flintdevice);

                addFlintDevice(flintdevice, set);

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

    public static synchronized FlintMediaRouteProvider getInstance(
            Context context) {
        if (mInstance == null) {
            mInstance = new FlintMediaRouteProvider(
                    context.getApplicationContext());
        }

        return mInstance;
    }

    public void setDeviceControllerListener(FlintRouteController controller) {
        FlintDevice flintdevice = controller.getFlintDevice();
        String id = flintdevice.getDeviceId();
        FlintDeviceControllerHelper helper = (FlintDeviceControllerHelper) mFlintDeviceControllerMap
                .get(id);
        if (helper == null) {
            helper = new FlintDeviceControllerHelper();
            log.d("set FlintDeviceController Listener %s", flintdevice);
            if (mCurrentDevice != null
                    && mCurrentDevice.getDeviceId().equals(id)) {
                helper.isConnecting = false;
            } else {
                helper.isConnecting = true;
            }
            mFlintDeviceControllerMap.put(id, helper);
        }
        helper.mFlintRouteControllerList.add(controller);
    }

    public void setCurrentConnectedDevce(FlintDevice flintDevice) {
        mCurrentDevice = flintDevice;
    }

    public static String getFriendlyName(FlintDevice flintdevice) {
        return flintdevice.getFriendlyName();
    }

    private MediaRouteDescriptor buildRouteDescriptorForDevice(
            DiscoveryCriteriaHelper criteriaHelper) {
        FlintDevice flintdevice = criteriaHelper.mFlintDevice;
        Set set = criteriaHelper.mDiscoveryCriteriaSet;
        FlintDeviceControllerHelper controllerHelper = (FlintDeviceControllerHelper) mFlintDeviceControllerMap
                .get(flintdevice.getDeviceId());
        boolean isConnecting;
        if (controllerHelper != null) {
            isConnecting = controllerHelper.isConnecting;
        } else {
            isConnecting = false;
        }
        Bundle bundle = new Bundle();
        flintdevice.putInBundle(bundle);
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
                flintdevice.getDeviceId(), flintdevice.getFriendlyName(),
                isConnecting);

        MediaRouteDescriptorPrivateData data = new MediaRouteDescriptorPrivateData(
                flintdevice.getDeviceId(), flintdevice.getFriendlyName());
        data.mBundle.putString("status", flintdevice.getModelName());
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

    void addFlintDevice(FlintDevice flintdevice, Set set) {
        DiscoveryCriteriaHelper criteriaHelper = (DiscoveryCriteriaHelper) mDiscoveryCriteriaMap
                .get(flintdevice.getDeviceId());
        if (criteriaHelper != null) {
            log.d("merging in criteria for existing device %s",
                    flintdevice.getFriendlyName());
            Iterator iterator = set.iterator();
            while (iterator.hasNext()) {
                DiscoveryCriteria criteria = (DiscoveryCriteria) iterator
                        .next();
                if (!criteriaHelper.mDiscoveryCriteriaSet.contains(criteria))
                    criteriaHelper.mDiscoveryCriteriaSet.add(criteria);
            }
        } else {
            mDiscoveryCriteriaMap.put(flintdevice.getDeviceId(),
                    new DiscoveryCriteriaHelper(flintdevice, set));
        }
    }

    private void publishRoutes() {
        ArrayList<MediaRouteDescriptor> routeList = new ArrayList<MediaRouteDescriptor>();
        Iterator<DiscoveryCriteriaHelper> iterator = mDiscoveryCriteriaMap.values().iterator();
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

    public void onUnselect(FlintRouteController controller) {
        FlintDevice flintdevice = controller.getFlintDevice();
        String id = flintdevice.getDeviceId();
        FlintDeviceControllerHelper helper = (FlintDeviceControllerHelper) mFlintDeviceControllerMap
                .get(id);
        if (helper != null) {
            helper.mFlintRouteControllerList.remove(controller);
            if (helper.isEmpty()) {
                log.d("disposing FlintDeviceController for %s", flintdevice);

                if (helper.isOffline) {
                    if (flintdevice.getFoundSource().equals(
                            FlintDevice.FOUND_SOURCE_MDNS))
                        mMdnsDeviceScanner.setDeviceOffline(id);
                    else if (flintdevice.getFoundSource().equals(
                            FlintDevice.FOUND_SOURCE_SSDP))
                        mSsdpDeviceScanner.setDeviceOffline(id);
                }

                if (!helper.isValid || helper.isOffline) {
                    removeCriteria(flintdevice);
                    publishRoutes();
                }

                mFlintDeviceControllerMap.remove(id);
            }
        }
    }

    private void removeCriteria(FlintDevice flintdevice) {
        mDiscoveryCriteriaMap.remove(flintdevice.getDeviceId());
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
                        .equals("tv.matchstick.flint.CATEGORY_FLINT_REMOTE_PLAYBACK")
                        && !category
                                .startsWith("tv.matchstick.flint.CATEGORY_FLINT_REMOTE_PLAYBACK/")
                        && !category
                                .equals("tv.matchstick.flint.CATEGORY_FLINT")
                        && !category
                                .startsWith("tv.matchstick.flint.CATEGORY_FLINT/")) {
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
            mFlintDeviceFilter.reset(mDiscoveryCriterias);
        }

        if (isStartScan) {
            log.d("starting the scan");

            FlintDeviceService.stopScanFlintDevice(super.mContext,
                    mMdnsDeviceScanner);
            FlintDeviceService.startScanFlintDevice(super.mContext,
                    mMdnsDeviceScanner);

            FlintDeviceService.stopScanFlintDevice(super.mContext,
                    mSsdpDeviceScanner);
            FlintDeviceService.startScanFlintDevice(super.mContext,
                    mSsdpDeviceScanner);

            return;
        }

        log.d("stopping the scan");

        FlintDeviceService.stopScanFlintDevice(super.mContext,
                mMdnsDeviceScanner);
        FlintDeviceService.stopScanFlintDevice(super.mContext,
                mSsdpDeviceScanner);
    }

    public final RouteController getRouteController(String routeId) {
        DiscoveryCriteriaHelper helper = (DiscoveryCriteriaHelper) mDiscoveryCriteriaMap
                .get(routeId);

        if (helper == null) {
            return null;
        }

        return new FlintRouteController(this, helper.mFlintDevice);
    }

    public final void onDiscoveryRequestChanged(DiscoveryRequest request) {
        log.d("in onDiscoveryRequestChanged: request=%s", request);

        onDiscoveryRequestChanged();
    }

    final class DiscoveryCriteriaHelper {
        final FlintDevice mFlintDevice;
        final Set<DiscoveryCriteria> mDiscoveryCriteriaSet;

        public DiscoveryCriteriaHelper(FlintDevice flintdevice,
                Set<DiscoveryCriteria> set) {
            mFlintDevice = flintdevice;
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
                        .isEquals(mFlintDevice, helper.mFlintDevice)
                        || !ObjEqualChecker.isEquals(mDiscoveryCriteriaSet,
                                helper.mDiscoveryCriteriaSet))
                    return false;
            }
            return flag;
        }

        @Override
        public final int hashCode() {
            Object aobj[] = new Object[2];
            aobj[0] = mFlintDevice;
            aobj[1] = mDiscoveryCriteriaSet;
            return Arrays.hashCode(aobj);
        }
    }

    final class FlintDeviceControllerHelper {
        public boolean isValid;
        public boolean isConnecting;
        public boolean isOffline;
        public final List<FlintRouteController> mFlintRouteControllerList = new ArrayList<FlintRouteController>();

        public FlintDeviceControllerHelper() {
            super();
            isValid = true;
        }

        public final boolean isEmpty() {
            return mFlintRouteControllerList.isEmpty();
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
