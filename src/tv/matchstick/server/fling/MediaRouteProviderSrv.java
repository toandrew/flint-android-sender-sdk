package tv.matchstick.server.fling;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.IBinder.DeathRecipient;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;

import tv.matchstick.server.common.checker.MainThreadChecker;
import tv.matchstick.server.fling.media.RouteController;

public abstract class MediaRouteProviderSrv extends IntentService {
    private static final boolean DEBUG = Log.isLoggable(
            "MediaRouteProviderSrv", Log.DEBUG);
    private final ArrayList mFlingDeathRecipientList = new ArrayList();
    private final MediaRouteProviderSrvHandler mMessageTargetHandler = new MediaRouteProviderSrvHandler(
            this);
    private final Messenger mMessenger;

    private final Handler mBinderDiedHandler = new Handler() {
        public final void handleMessage(Message message) {
            switch (message.what) {
            default:
                return;

            case 1: // '\001'
                onBinderDied(MediaRouteProviderSrv.this,
                        (Messenger) message.obj);
                break;
            }
        }
    };

    private final DescriptorChangedListener mDescriptorChangedListener = new DescriptorChangedListener() {
        public final void onDescriptorChanged(
                MediaRouteProviderDescriptor descriptor) {
            sendDescriptorChangeEvent(MediaRouteProviderSrv.this, descriptor);
        }
    };

    private MediaRouteProvider mMediaRouteProvider;
    private DiscoveryRequest mDiscoveryRequest;

    public MediaRouteProviderSrv() {
        super("MediaRouteProviderSrv");
        mMessenger = new Messenger(mMessageTargetHandler);
    }

    static int getFlingDeathRecipitentListIndex(
            MediaRouteProviderSrv mediaRouteProvider, Messenger messenger) {
        return mediaRouteProvider.getFlingDeathRecipitentListIndex(messenger);
    }

    static String getClientConnectionInfo(Messenger messenger) {
        return getClientConnectionInfo_d(messenger);
    }

    static MediaRouteProvider getMediaRouteProvider(
            MediaRouteProviderSrv routePrivider) {
        return routePrivider.mMediaRouteProvider;
    }

    static void sendFailureReplyMsg(Messenger messenger, int requestId) {
        if (requestId != 0)
            sendReplyMsg(messenger, 0, requestId, 0, null, null); // 0:SERVICE_MSG_GENERIC_FAILURE
    }

    private static void sendReplyMsg(Messenger messenger, int what,
            int requestId, int arg, Object obj, Bundle data) {
        Message message = Message.obtain();
        message.what = what;
        message.arg1 = requestId;
        message.arg2 = arg;
        message.obj = obj;
        message.setData(data);
        try {
            messenger.send(message);
            return;
        } catch (DeadObjectException deadobjectexception) {
            return;
        } catch (RemoteException remoteexception) {
            Log.e("MediaRouteProviderSrv",
                    ("Could not send message to " + getClientConnectionInfo_d(messenger)),
                    remoteexception);
        }
    }

    static void sendReplyMsg(Messenger messenger, int what, int requestId,
            Object obj, Bundle data) {
        sendReplyMsg(messenger, what, requestId, 0, obj, data);
    }

    static void sendDescriptorChangeEvent(MediaRouteProviderSrv routeProvider,
            MediaRouteProviderDescriptor descriptor) {
        Bundle bundle;
        int size;
        if (descriptor != null)
            bundle = descriptor.mRoutes;
        else
            bundle = null;
        size = routeProvider.mFlingDeathRecipientList.size();
        for (int index = 0; index < size; index++) {
            FlingDeathRecipient deathRecipient = (FlingDeathRecipient) routeProvider.mFlingDeathRecipientList
                    .get(index);
            sendReplyMsg(deathRecipient.mMessenger, 5, 0, 0, bundle, null); // 5:SERVICE_MSG_DESCRIPTOR_CHANGED
            if (DEBUG)
                Log.d("MediaRouteProviderSrv",
                        (deathRecipient
                                + ": Sent descriptor change event, descriptor=" + descriptor));
        }

    }

    private boolean register(Messenger messenger, int requestId, int version) {
        if (version > 0 && getFlingDeathRecipitentListIndex(messenger) < 0) {
            FlingDeathRecipient deathRecipient = new FlingDeathRecipient(this,
                    messenger, version);
            if (deathRecipient.linkToDeath()) {
                mFlingDeathRecipientList.add(deathRecipient);

                if (DEBUG)
                    Log.d("MediaRouteProviderSrv", deathRecipient
                            + ": Registered, version=" + version);

                if (requestId != 0) {
                    MediaRouteProviderDescriptor descriptor = mMediaRouteProvider.mMediaRouteProviderDescriptor;
                    Bundle bundle;
                    if (descriptor != null)
                        bundle = descriptor.mRoutes;
                    else
                        bundle = null;
                    sendReplyMsg(messenger, 2, requestId, 1, bundle, null); // 2:SERVICE_MSG_REGISTERED
                }
                return true;
            }
        }
        return false;
    }

    static boolean unRegister(MediaRouteProviderSrv routeProvider,
            Messenger messenger, int requestId) {
        int j = routeProvider.getFlingDeathRecipitentListIndex(messenger);
        if (j >= 0) {
            FlingDeathRecipient deathRecipient = (FlingDeathRecipient) routeProvider.mFlingDeathRecipientList
                    .remove(j);

            if (DEBUG)
                Log.d("MediaRouteProviderSrv", deathRecipient
                        + ": Unregistered");

            deathRecipient.onBinderDied();
            sendReplyMsg(messenger, requestId);
            return true;
        } else {
            return false;
        }
    }

    static boolean register(MediaRouteProviderSrv routeProvider,
            Messenger messenger, int requestId, int arg) {
        return routeProvider.register(messenger, requestId, arg);
    }

    static boolean createRouteController(MediaRouteProviderSrv routeProvider,
            Messenger messenger, int requestId, int controllerId, String routeId) {
        FlingDeathRecipient deathRecipient = routeProvider
                .getFlingDeathRecipient(messenger);
        if (deathRecipient != null
                && deathRecipient.checkRouteController(routeId, controllerId)) {
            if (DEBUG)
                Log.d("MediaRouteProviderSrv", deathRecipient
                        + ": Route controller created, controllerId="
                        + controllerId + ", routeId=" + routeId);
            sendReplyMsg(messenger, requestId);
            return true;
        } else {
            return false;
        }
    }

    static boolean setDiscoveryRequest(MediaRouteProviderSrv routeProvider,
            Messenger messenger, int requestId, DiscoveryRequest request) {
        FlingDeathRecipient deathRecipient = routeProvider
                .getFlingDeathRecipient(messenger);
        if (deathRecipient != null) {
            boolean actuallyChanged = deathRecipient
                    .setDiscoveryRequest(request);

            if (DEBUG)
                Log.d("MediaRouteProviderSrv", deathRecipient
                        + ": Set discovery request, request=" + request
                        + ", actuallyChanged=" + actuallyChanged
                        + ", compositeDiscoveryRequest="
                        + routeProvider.mDiscoveryRequest);

            sendReplyMsg(messenger, requestId);
            return true;
        } else {
            return false;
        }
    }

    private FlingDeathRecipient getFlingDeathRecipient(Messenger messenger) {
        int index = getFlingDeathRecipitentListIndex(messenger);
        if (index >= 0)
            return (FlingDeathRecipient) mFlingDeathRecipientList.get(index);
        else
            return null;
    }

    private static void sendReplyMsg(Messenger messenger, int requestId) {
        if (requestId != 0)
            sendReplyMsg(messenger, 1, requestId, 0, null, null); // 1:SERVICE_MSG_GENERIC_SUCCESS
    }

    static void onBinderDied(MediaRouteProviderSrv routeProvider,
            Messenger messenger) {
        int index = routeProvider.getFlingDeathRecipitentListIndex(messenger);
        if (index >= 0) {
            FlingDeathRecipient deathRecipient = (FlingDeathRecipient) routeProvider.mFlingDeathRecipientList
                    .remove(index);

            if (DEBUG)
                Log.d("MediaRouteProviderSrv", deathRecipient + ": Binder died");

            deathRecipient.onBinderDied();
        }
    }

    static boolean isDebugable() {
        return DEBUG;
    }

    static boolean setDiscoveryRequest(MediaRouteProviderSrv mediaRouteProvider) {
        Object obj = null;
        int size = mediaRouteProvider.mFlingDeathRecipientList.size();
        int j = 0;
        boolean flag = false;
        DiscoveryRequest request = null;

        Bundle bundle;
        MediaRouteSelector oj1;
        MediaRouteSelector selector;
        while (j < size) {
            DiscoveryRequest discoveryRequest = ((FlingDeathRecipient) mediaRouteProvider.mFlingDeathRecipientList
                    .get(j)).mDiscoveryRequest;
            boolean flag1;
            CategoriesData ok1;
            DiscoveryRequest nu3;
            if (discoveryRequest != null
                    && (!discoveryRequest.getSelector().isEmpty() || discoveryRequest
                            .isActiveScan())) {
                flag1 = flag | discoveryRequest.isActiveScan();
                if (request == null) {
                    ok1 = (CategoriesData) obj;
                    nu3 = discoveryRequest;
                } else {

                    if (obj == null)
                        ok1 = new CategoriesData(request.getSelector());
                    else
                        ok1 = (CategoriesData) obj;
                    selector = discoveryRequest.getSelector();
                    if (selector == null)
                        throw new IllegalArgumentException(
                                "selector must not be null");
                    ok1.addCategoryList(selector.getControlCategories());
                    nu3 = request;
                }
            } else {
                flag1 = flag;
                ok1 = (CategoriesData) obj;
                nu3 = request;
            }
            j++;
            request = nu3;
            obj = ok1;
            flag = flag1;
        }
        if (obj != null) {
            if (((CategoriesData) (obj)).mControlCategories == null) {
                oj1 = MediaRouteSelector.EMPTY;
            } else {
                bundle = new Bundle();
                bundle.putStringArrayList("controlCategories",
                        ((CategoriesData) (obj)).mControlCategories);
                oj1 = new MediaRouteSelector(bundle,
                        ((CategoriesData) (obj)).mControlCategories, (byte) 0);
            }
            request = new DiscoveryRequest(oj1, flag);
        }
        if (mediaRouteProvider.mDiscoveryRequest == request
                || mediaRouteProvider.mDiscoveryRequest != null
                && mediaRouteProvider.mDiscoveryRequest.equals(request)) {
            return false;
        }

        mediaRouteProvider.mDiscoveryRequest = request;
        // nv1 = od1.g;
        MainThreadChecker.isOnAppMainThread();
        if (mediaRouteProvider.mMediaRouteProvider.mDiscoveryRequest != request
                && (mediaRouteProvider.mMediaRouteProvider.mDiscoveryRequest == null || !mediaRouteProvider.mMediaRouteProvider.mDiscoveryRequest
                        .equals(request))) {
            mediaRouteProvider.mMediaRouteProvider.mDiscoveryRequest = request;
            if (!mediaRouteProvider.mMediaRouteProvider.mPendingDiscoveryRequestChange) {
                mediaRouteProvider.mMediaRouteProvider.mPendingDiscoveryRequestChange = true;
                mediaRouteProvider.mMediaRouteProvider.mHandler
                        .sendEmptyMessage(2); // device
                                              // discovery
                                              // request
            }
        }

        return true;
    }

    static boolean releaseRouteController(MediaRouteProviderSrv routeProvider,
            Messenger messenger, int requestId, int controllerId) {
        FlingDeathRecipient deathRecipient = routeProvider
                .getFlingDeathRecipient(messenger);
        if (deathRecipient != null
                && deathRecipient.releaseRouteController(controllerId)) {
            if (DEBUG)
                Log.d("MediaRouteProviderSrv", deathRecipient
                        + ": Route controller released, controllerId="
                        + controllerId);

            sendReplyMsg(messenger, requestId);
            return true;
        } else {
            return false;
        }
    }

    private int getFlingDeathRecipitentListIndex(Messenger messenger) {
        int size = mFlingDeathRecipientList.size();
        for (int j = 0; j < size; j++)
            if (((FlingDeathRecipient) mFlingDeathRecipientList.get(j))
                    .isBinderEquals(messenger))
                return j;

        return -1;
    }

    static Handler getHandler(MediaRouteProviderSrv routeProvider) {
        return routeProvider.mBinderDiedHandler;
    }

    static boolean selectRoute(MediaRouteProviderSrv mediaRouteProvider,
            Messenger messenger, int requestId, int controllerId) {
        FlingDeathRecipient deathRecipient = mediaRouteProvider
                .getFlingDeathRecipient(messenger);
        if (deathRecipient != null) {
            RouteController routeController = deathRecipient
                    .getRouteController(controllerId);
            if (routeController != null) {
                routeController.onSelect();

                if (DEBUG)
                    Log.d("MediaRouteProviderSrv", deathRecipient
                            + ": Route selected, controllerId=" + controllerId);

                sendReplyMsg(messenger, requestId);
                return true;
            }
        }
        return false;
    }

    private static String getClientConnectionInfo_d(Messenger messenger) {
        return ("Client connection" + messenger.getBinder().toString());
    }

    static boolean unselectRoute(MediaRouteProviderSrv mediaRouteProvider,
            Messenger messenger, int requestId, int controllerId) {
        FlingDeathRecipient deathRecipient = mediaRouteProvider
                .getFlingDeathRecipient(messenger);
        if (deathRecipient != null) {
            RouteController routeController = deathRecipient
                    .getRouteController(controllerId);
            if (routeController != null) {
                routeController.onUnselect();

                if (DEBUG)
                    Log.d("MediaRouteProviderSrv", deathRecipient
                            + ": Route unselected, controllerId="
                            + controllerId);

                sendReplyMsg(messenger, requestId);
                return true;
            }
        }
        return false;
    }

    public abstract MediaRouteProvider getInstance();

    public IBinder onBind(Intent intent) {
        if (intent.getAction()
                .equals("android.media.MediaRouteProviderService")) {
            if (mMediaRouteProvider == null) {
                MediaRouteProvider mediaRouteProvider = getInstance();
                if (mediaRouteProvider != null) {
                    String packageName = mediaRouteProvider.mProviderComponentName
                            .getPackageName();
                    if (!packageName.equals(getPackageName()))
                        throw new IllegalStateException(
                                "onCreateMediaRouteProvider() returned a provider whose package name does not match the package name of the service.  A media route provider service can only export its own media route providers.  Provider package name: "
                                        + packageName
                                        + ".  Service package name: "
                                        + getPackageName());

                    mMediaRouteProvider = mediaRouteProvider;
                    MainThreadChecker.isOnAppMainThread();
                    mMediaRouteProvider.mDescriptorChangedListener = mDescriptorChangedListener;
                }
            }
            if (mMediaRouteProvider != null)
                return mMessenger.getBinder();
        }
        return null;
    }

    final class FlingDeathRecipient implements DeathRecipient {
        public final Messenger mMessenger;
        public final int mVersion;
        public DiscoveryRequest mDiscoveryRequest;
        final MediaRouteProviderSrv mMediaRouteProviderSrv;
        private final SparseArray mRouteControllerList = new SparseArray();

        public FlingDeathRecipient(MediaRouteProviderSrv routeProvider,
                Messenger messenger, int version) {
            super();
            mMediaRouteProviderSrv = routeProvider;
            mMessenger = messenger;
            mVersion = version;
        }

        public final boolean linkToDeath() {
            try {
                mMessenger.getBinder().linkToDeath(this, 0);
            } catch (RemoteException remoteexception) {
                binderDied();
                return false;
            }
            return true;
        }

        public final boolean releaseRouteController(int controllerId) {
            RouteController routeController = (RouteController) mRouteControllerList
                    .get(controllerId);
            if (routeController != null) {
                mRouteControllerList.remove(controllerId);
                routeController.onRelease();
                return true;
            } else {
                return false;
            }
        }

        public final boolean isBinderEquals(Messenger messenger) {
            return mMessenger.getBinder() == messenger.getBinder();
        }

        public final boolean checkRouteController(String routeId,
                int controllerId) {
            if (mRouteControllerList.indexOfKey(controllerId) < 0) {
                RouteController routeController = MediaRouteProviderSrv
                        .getMediaRouteProvider(mMediaRouteProviderSrv)
                        .getRouteController(routeId);
                if (routeController != null) {
                    mRouteControllerList.put(controllerId, routeController);
                    return true;
                }
            }
            return false;
        }

        public final boolean setDiscoveryRequest(DiscoveryRequest request) {
            if (mDiscoveryRequest != request
                    && (mDiscoveryRequest == null || !mDiscoveryRequest
                            .equals(request))) {
                mDiscoveryRequest = request;
                return MediaRouteProviderSrv
                        .setDiscoveryRequest(mMediaRouteProviderSrv);
            } else {
                return false;
            }
        }

        public final RouteController getRouteController(int i) {
            return (RouteController) mRouteControllerList.get(i);
        }

        public final void onBinderDied() {
            int size = mRouteControllerList.size();
            for (int j = 0; j < size; j++)
                ((RouteController) mRouteControllerList.valueAt(j)).onRelease();

            mRouteControllerList.clear();
            mMessenger.getBinder().unlinkToDeath(this, 0);
            setDiscoveryRequest(((DiscoveryRequest) (null)));
        }

        public final void binderDied() {
            MediaRouteProviderSrv.getHandler(mMediaRouteProviderSrv)
                    .obtainMessage(1, mMessenger).sendToTarget();
        }

        public final String toString() {
            return MediaRouteProviderSrv.getClientConnectionInfo(mMessenger);
        }
    }

}
