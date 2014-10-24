package tv.matchstick.server.fling;

import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tv.matchstick.fling.FlingDevice;
import tv.matchstick.fling.service.FlingDeviceService;
import tv.matchstick.server.common.checker.MainThreadChecker;
import tv.matchstick.server.common.checker.ObjEqualChecker;
import tv.matchstick.server.fling.mdns.DeviceScanner;
import tv.matchstick.server.fling.mdns.IDeviceScanListener;
import tv.matchstick.server.fling.mdns.MdnsDeviceScanner;
import tv.matchstick.server.fling.media.RouteController;
import tv.matchstick.server.utils.LOG;

public class FlingMediaRouteProvider extends MediaRouteProvider {
    private static final LOG mLogs = new LOG("FlingMediaRouteProvider");
    private static FlingMediaRouteProvider mInstance;

    private final DeviceScanner mMdnsDeviceScanner;

    private final IDeviceScanListener mDeviceScannerListener = new IDeviceScanListener() {

        @Override
        public void onAllDevicesOffline() {
            // TODO Auto-generated method stub
            mLogs.d("DeviceScanner.Listener#onAllDevicesOffline");
            for (Iterator iterator = mDiscoveryCriteriaMap.entrySet()
                    .iterator(); iterator.hasNext();) {
                DiscoveryCriteriaHelper awv1 = (DiscoveryCriteriaHelper) ((java.util.Map.Entry) iterator
                        .next()).getValue();
                if (awv1 != null) {
                    FlingDevice flingdevice = awv1.mFlingDevice;
                    FlingDeviceControllerHelper awu1 = (FlingDeviceControllerHelper) mFlingDeviceControllerMap
                            .get(flingdevice.getDeviceId());
                    if (awu1 != null) {
                        awu1.b = false;
                        mLogs.d("device %s is in use; not removing route",
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
            // TODO Auto-generated method stub
            mLogs.d("DeviceScanner.Listener#onDeviceOnline :%s", flingdevice);
            FlingDeviceControllerHelper awu1 = (FlingDeviceControllerHelper) mFlingDeviceControllerMap
                    .get(flingdevice.getDeviceId());
            if (awu1 != null) {
                awu1.b = true;
            }
            mFlingDeviceFilter.connectOrAcceptDevice(flingdevice);
        }

        @Override
        public void onDeviceOffline(FlingDevice flingdevice) {
            // TODO Auto-generated method stub
            mLogs.d("DeviceScanner.Listener#onDeviceOffline :%s",
                    new Object[] { flingdevice });
            FlingDeviceControllerHelper awu1 = (FlingDeviceControllerHelper) mFlingDeviceControllerMap
                    .get(flingdevice.getDeviceId());
            if (awu1 != null) {
                awu1.b = false;
            } else {
                b(flingdevice);
                publishRoutes();
            }
        }

    };

    private final Map<String, DiscoveryCriteriaHelper> mDiscoveryCriteriaMap = new HashMap<String, DiscoveryCriteriaHelper>();
    private final Map<String, FlingDeviceControllerHelper> mFlingDeviceControllerMap = new HashMap<String, FlingDeviceControllerHelper>();
    private boolean s;
    private final DeviceFilter mFlingDeviceFilter;
    private final Set<DiscoveryCriteria> mDiscoveryCriteriaHashSet = new HashSet<DiscoveryCriteria>();
    private final Map<Integer, String> mErrorMap = new HashMap<Integer, String>();

    private static final String[] mFlingMimeTypes = { "image/jpeg",
            "image/pjpeg", "image/jpg", "image/webp", "image/png", "image/gif",
            "image/bmp", "image/vnd.microsoft.icon", "image/x-icon",
            "image/x-xbitmap", "audio/wav", "audio/x-wav", "audio/mp3",
            "audio/x-mp3", "audio/x-m4a", "audio/mpeg", "audio/webm",
            "video/mp4", "video/x-m4v", "video/mp2t", "video/webm"

    };

    private FlingMediaRouteProvider(Context context) {
        super(context);
        mErrorMap.put(Integer.valueOf(1), "Request failed");
        mErrorMap.put(Integer.valueOf(2), "Failed to start a session");
        mErrorMap.put(Integer.valueOf(2), "Unknown or invalid session ID");
        mErrorMap.put(Integer.valueOf(3),
                "Disconnected from Fling Device but trying to reconnect");

        mFlingDeviceFilter = new DeviceFilter(context,
                mDiscoveryCriteriaHashSet, context.getPackageName()) {

            @Override
            protected void setDeviceOffline(FlingDevice flingdevice) {
                // TODO Auto-generated method stub
                mMdnsDeviceScanner.setDeviceOffline(flingdevice.getDeviceId());
            }

            @Override
            protected void onDeviceAccepted(FlingDevice flingdevice, Set set) {
                // TODO Auto-generated method stub

                getLogs_a().d("DeviceFilter#onDeviceAccepted: %s",
                        new Object[] { flingdevice });

                addFlingDevice(flingdevice, set);
                publishRoutes();
            }
        };

        mMdnsDeviceScanner = new MdnsDeviceScanner(context);
        mMdnsDeviceScanner.addListener(mDeviceScannerListener);
        publishRoutes();
    }

    public static LOG getLogs_a() {
        return mLogs;
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
        FlingDeviceControllerHelper awu1 = (FlingDeviceControllerHelper) mFlingDeviceControllerMap
                .get(id);
        if (awu1 == null) {
            awu1 = new FlingDeviceControllerHelper();
            mLogs.d("set FlingDeviceController Listener %s", flingdevice);
            mFlingDeviceControllerMap.put(id, awu1);
        }
        awu1.mFlingRouteControllerList.add(controller);
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
        String statusText;
        boolean isConnecting;
        int volumeHandling;
        int volume;
        String status;
        if (controllerHelper != null) {
            boolean flag1 = controllerHelper.isConnecting;
            FlingDeviceController controller = controllerHelper.getController();
            if (controller != null && controller.isConnected()) {
                int k1 = (int) Math.round(20D * controller.getVolume());
                statusText = controller.getStatusText();
                volume = k1;
                isConnecting = flag1;
                volumeHandling = 1;
            } else {
                isConnecting = flag1;
                statusText = null;
                volumeHandling = 0;
                volume = 0;
            }
        } else {
            statusText = null;
            isConnecting = false;
            volumeHandling = 0;
            volume = 0;
        }
        if (TextUtils.isEmpty(statusText))
            status = flingdevice.getModelName();
        else
            status = statusText;
        Bundle bundle = new Bundle();
        flingdevice.putInBundle(bundle);
        ArrayList arraylist = new ArrayList();
        IntentFilter intentfilter;
        for (Iterator iterator = set.iterator(); iterator.hasNext(); arraylist
                .add(intentfilter)) {
            DiscoveryCriteria criteria = (DiscoveryCriteria) iterator.next();
            intentfilter = new IntentFilter();
            String category = criteria.mCategory;
            intentfilter.addCategory(category);
            if (!isEquals(category,
                    "android.media.intent.category.REMOTE_PLAYBACK")
                    && !isEquals(category,
                            "tv.matchstick.fling.CATEGORY_FLING_REMOTE_PLAYBACK"))
                continue;
        }

        mLogs.d("buildRouteDescriptorForDevice: id=%s, description=%s, connecting=%b, volume=%d",
                flingdevice.getDeviceId(), flingdevice.getFriendlyName(),
                Boolean.valueOf(isConnecting), Integer.valueOf(volume));
        MediaRouteDescriptorPrivateData data = new MediaRouteDescriptorPrivateData(
                flingdevice.getDeviceId(), flingdevice.getFriendlyName());
        data.mBundle.putString("status", status);
        data.mBundle.putBoolean("connecting", isConnecting);
        data.mBundle.putInt("volumeHandling", volumeHandling);
        data.mBundle.putInt("volume", volume);
        data.mBundle.putInt("volumeMax", 20);
        data.mBundle.putInt("playbackType", 1);
        MediaRouteDescriptorPrivateData privateData = data
                .addIntentFilterList(arraylist);
        privateData.mBundle.putBundle("extras", bundle);
        if (privateData.mControlIntentFilterList != null)
            privateData.mBundle.putParcelableArrayList("controlFilters",
                    privateData.mControlIntentFilterList);
        return new MediaRouteDescriptor(privateData.mBundle,
                privateData.mControlIntentFilterList);
    }

    void addFlingDevice(FlingDevice flingdevice, Set set) {
        DiscoveryCriteriaHelper criteriaHelper = (DiscoveryCriteriaHelper) mDiscoveryCriteriaMap
                .get(flingdevice.getDeviceId());
        if (criteriaHelper != null) {
            mLogs.d("merging in criteria for existing device %s",
                    flingdevice.getFriendlyName());
            Iterator iterator = set.iterator();
            do {
                if (!iterator.hasNext())
                    break;
                DiscoveryCriteria criteria = (DiscoveryCriteria) iterator
                        .next();
                if (!criteriaHelper.mDiscoveryCriteriaSet.contains(criteria))
                    criteriaHelper.mDiscoveryCriteriaSet.add(criteria);
            } while (true);
        } else {
            mDiscoveryCriteriaMap.put(flingdevice.getDeviceId(),
                    new DiscoveryCriteriaHelper(flingdevice, set));
        }
    }

    private static boolean isEquals(String s1, String s2) {
        return s1.equals(s2) || s1.startsWith(s2 + "/");
    }

    private void publishRoutes() {
        ArrayList<MediaRouteDescriptor> routeList = new ArrayList<MediaRouteDescriptor>();
        for (Iterator iterator = mDiscoveryCriteriaMap.values().iterator(); iterator
                .hasNext(); routeList
                .add(buildRouteDescriptorForDevice((DiscoveryCriteriaHelper) iterator
                        .next())))
            ;
        MediaRouteProviderDescriptor providerDescriptor = buildMediaRouteProviderDescriptor(routeList);
        MainThreadChecker.isOnAppMainThread();
        if (super.mMediaRouteProviderDescriptor != providerDescriptor) {
            super.mMediaRouteProviderDescriptor = providerDescriptor;
            if (!super.mPendingDescriptorChange) {
                super.mPendingDescriptorChange = true;
                super.mHandler.sendEmptyMessage(1); // MSG_DELIVER_DESCRIPTOR_CHANGED
            }
        }

        mLogs.d("published %d routes", routeList.size());
    }

    private MediaRouteProviderDescriptor buildMediaRouteProviderDescriptor(
            ArrayList<MediaRouteDescriptor> routeList) {
        ArrayList<MediaRouteDescriptor> list = new ArrayList<MediaRouteDescriptor>();
        Bundle bundle = new Bundle();

        Iterator iterator;
        if (routeList == null)
            throw new IllegalArgumentException("routes must not be null");

        iterator = routeList.iterator();
        while (iterator.hasNext()) {
            MediaRouteDescriptor descriptor = (MediaRouteDescriptor) iterator
                    .next();
            if (descriptor == null)
                throw new IllegalArgumentException("route must not be null");
            if (list.contains(descriptor)) {
                throw new IllegalArgumentException(
                        "route descriptor already added");
            }

            list.add(descriptor);
        }
        if (list != null) {
            int size = list.size();
            ArrayList arraylist = new ArrayList(size);
            for (int i = 0; i < size; i++)
                arraylist.add(((MediaRouteDescriptor) list.get(i)).bundleData);

            bundle.putParcelableArrayList("routes", arraylist);
        }
        return new MediaRouteProviderDescriptor(bundle, list);
    }

    public void b(FlingRouteController awn1) {
        FlingDevice flingdevice = awn1.getFlingDevice();
        String id = flingdevice.getDeviceId();
        FlingDeviceControllerHelper awu1 = (FlingDeviceControllerHelper) mFlingDeviceControllerMap
                .get(id);
        if (awu1 != null) {
            awu1.mFlingRouteControllerList.remove(awn1);
            if (awu1.isEmpty()) {
                mLogs.d("disposing FlingDeviceController for %s", flingdevice);
                // awu1.mFlingDeviceController.releaseReference();
                if (awu1.isOffline)
                    mMdnsDeviceScanner.setDeviceOffline(id);
                if (!awu1.b || awu1.isOffline) {
                    b(flingdevice);
                    publishRoutes();
                }
                mFlingDeviceControllerMap.remove(id);
            }
        }
    }

    private void b(FlingDevice flingdevice) {
        mDiscoveryCriteriaMap.remove(flingdevice.getDeviceId());
    }

    private void onDiscoveryRequestChanged() {
        boolean isStartScan, flag1, flag2;
        HashSet hashset;
        DiscoveryRequest request;
        isStartScan = true;
        hashset = new HashSet(mDiscoveryCriteriaHashSet);
        mDiscoveryCriteriaHashSet.clear();
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
                        mDiscoveryCriteriaHashSet.add(criteria);
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
        if (!s) {
            isStartScan = flag1;
        }

        if (hashset.equals(mDiscoveryCriteriaHashSet)) {
            isStartScan = false;
        } else {
            mFlingDeviceFilter.reset(mDiscoveryCriteriaHashSet);
        }

        if (isStartScan) {
            mLogs.d("starting the scan");

            FlingDeviceService.stopScanFlingDevice(super.mContext,
                    mMdnsDeviceScanner);
            FlingDeviceService.startScanFlingDevice(super.mContext,
                    mMdnsDeviceScanner);
            return;
        } else {
            mLogs.d("stopping the scan");

            FlingDeviceService.stopScanFlingDevice(super.mContext,
                    mMdnsDeviceScanner);
            return;
        }
    }

    public final RouteController getRouteController(String routeId) {
        DiscoveryCriteriaHelper awv1 = (DiscoveryCriteriaHelper) mDiscoveryCriteriaMap
                .get(routeId);
        if (awv1 == null)
            return null;
        else
            return new FlingRouteController(this, awv1.mFlingDevice);
    }

    public final void onDiscoveryRequestChanged(DiscoveryRequest request) {
        mLogs.d("in onDiscoveryRequestChanged: request=%s", request);
        onDiscoveryRequestChanged();
    }

    public final void a(boolean flag) {
        if (s != flag) {
            s = flag;
            onDiscoveryRequestChanged();
        }
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
        public boolean b;
        public boolean isConnecting;
        public boolean isOffline;
        public final List<FlingRouteController> mFlingRouteControllerList = new ArrayList<FlingRouteController>();

        public FlingDeviceControllerHelper() {
            super();
            b = true;
        }

        public final boolean isEmpty() {
            return mFlingRouteControllerList.isEmpty();
        }

        public FlingDeviceController getController() {
            return FlingDeviceController.getCurrentController();
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
                    if (!iterator.hasNext())
                        break;
                    IntentFilter intentfilter = (IntentFilter) iterator.next();

                    if (intentfilter == null)
                        throw new IllegalArgumentException(
                                "filter must not be null");

                    if (mControlIntentFilterList == null)
                        mControlIntentFilterList = new ArrayList();
                    if (!mControlIntentFilterList.contains(intentfilter))
                        mControlIntentFilterList.add(intentfilter);
                } while (true);
            }
            return this;
        }
    }

}
