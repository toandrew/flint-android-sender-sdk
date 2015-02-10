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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import tv.matchstick.client.internal.LOG;
import tv.matchstick.server.common.checker.MainThreadChecker;
import tv.matchstick.server.flint.media.RouteController;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;

public abstract class MediaRouteProviderSrv extends IntentService {
    private static final LOG log = new LOG("MediaRouteProviderSrv");

    private final ArrayList mFlintDeathRecipientList = new ArrayList();

    private final MediaRouteProviderSrvHandler mMessageTargetHandler = new MediaRouteProviderSrvHandler(
            this);

    private final Messenger mMessenger;

    private final Handler mBinderDiedHandler = new Handler() {
        public final void handleMessage(Message message) {
            switch (message.what) {
            default:
                return;

            case 1:
                onBinderDied((Messenger) message.obj);
                break;
            }
        }
    };

    private final DescriptorChangedListener mDescriptorChangedListener = new DescriptorChangedListener() {
        public final void onDescriptorChanged(
                MediaRouteProviderDescriptor descriptor) {

            sendDescriptorChangeEvent(descriptor);
        }
    };

    private MediaRouteProvider mMediaRouteProvider;

    private DiscoveryRequest mDiscoveryRequest;

    public MediaRouteProviderSrv() {
        super("MediaRouteProviderSrv");

        mMessenger = new Messenger(mMessageTargetHandler);
    }

    private static final int CLIENT_MSG_REGISTER = 1;
    private static final int CLIENT_MSG_UNREGISTER = 2;
    private static final int CLIENT_MSG_CREATE_ROUTE_CONTROLLER = 3;
    private static final int CLIENT_MSG_RELEASE_ROUTE_CONTROLLER = 4;
    private static final int CLIENT_MSG_SELECT_ROUTE = 5;
    private static final int CLIENT_MSG_UNSELECT_ROUTE = 6;
    private static final int CLIENT_MSG_SET_ROUTE_VOLUME = 7;
    private static final int CLIENT_MSG_UPDATE_ROUTE_VOLUME = 8;
    private static final int CLIENT_MSG_ROUTE_CONTROL_REQUEST = 9;
    private static final int CLIENT_MSG_SET_DISCOVERY_REQUEST = 10;

    private class MediaRouteProviderSrvHandler extends Handler {

        private final WeakReference mRouteProviderRef;

        public MediaRouteProviderSrvHandler(MediaRouteProviderSrv routeProvider) {
            mRouteProviderRef = new WeakReference(routeProvider);
        }

        @Override
        public final void handleMessage(Message message) {
            android.os.Messenger messenger = message.replyTo;
            if (!isValid(messenger)) {
                log.d("Ignoring message without valid reply messenger.");
                return;
            }
            boolean ok = false;
            int what = message.what;
            int requestId = message.arg1;
            int arg = message.arg2;
            Object obj = message.obj;
            Bundle data = message.peekData();
            MediaRouteProviderSrv routeProvider = (MediaRouteProviderSrv) mRouteProviderRef
                    .get();

            if (routeProvider != null) {
                switch (message.what) {
                case CLIENT_MSG_REGISTER:
                    ok = register(messenger, requestId, arg);
                    break;
                case CLIENT_MSG_UNREGISTER:
                    ok = unRegister(messenger, requestId);
                    break;
                case CLIENT_MSG_CREATE_ROUTE_CONTROLLER:
                    String routeId = data.getString("routeId");
                    if (routeId == null) {
                        ok = false;
                    } else {
                        ok = createRouteController(messenger, requestId, arg,
                                routeId);
                    }
                    break;
                case CLIENT_MSG_RELEASE_ROUTE_CONTROLLER:
                    ok = releaseRouteController(messenger, requestId, arg);
                    break;
                case CLIENT_MSG_SELECT_ROUTE:
                    ok = selectRoute(messenger, requestId, arg);
                    break;
                case CLIENT_MSG_UNSELECT_ROUTE:
                    ok = unselectRoute(messenger, requestId, arg);
                    break;
                case CLIENT_MSG_SET_ROUTE_VOLUME:
                    int volume = data.getInt("volume", -1);
                    if (volume < 0) {
                        ok = false;
                    } else {
                        // flag =
                        // MediaRouteProviderSrv.setRouteVolume(routeProvider,
                        // messenger,
                        // requestId, arg, volume);
                    }
                    break;
                case CLIENT_MSG_UPDATE_ROUTE_VOLUME:
                    volume = data.getInt("volume", 0);
                    if (volume == 0) {
                        ok = false;
                    } else {
                        // flag =
                        // MediaRouteProviderSrv.updateRouteVolume(routeProvider,
                        // messenger,
                        // requestId, arg, volume);
                    }
                    break;
                case CLIENT_MSG_ROUTE_CONTROL_REQUEST:
                    if (!(obj instanceof Intent)) {
                        ok = false;
                    } else {
                        // flag =
                        // MediaRouteProviderSrv.routeControlRequest(routeProvider,
                        // messenger,
                        // requestId, arg,
                        // (Intent) obj);
                    }

                    break;
                case CLIENT_MSG_SET_DISCOVERY_REQUEST:
                    if (obj != null && !(obj instanceof Bundle)) {
                        ok = false;
                    } else {
                        Bundle bundle1 = (Bundle) obj;
                        DiscoveryRequest request;
                        if (bundle1 != null)
                            request = new DiscoveryRequest(bundle1);
                        else
                            request = null;
                        if (request == null || !request.isValid())
                            request = null;
                        ok = setDiscoveryRequest(messenger, requestId, request);
                    }
                    break;
                }
            } else {
                ok = false;
            }

            if (!ok) {
                log.d(getClientConnectionInfo(messenger)
                        + ": Message failed, what=" + what + ", requestId="
                        + requestId + ", arg=" + arg + ", obj=" + obj
                        + ", data=" + data);

                sendFailureReplyMsg(messenger, requestId);
            }

            return;
        }

        public boolean isValid(Messenger messenger) {
            boolean flag = false;
            if (messenger != null) {
                android.os.IBinder ibinder;
                try {
                    ibinder = messenger.getBinder();
                } catch (NullPointerException e) {
                    return false;
                }
                flag = false;
                if (ibinder != null)
                    flag = true;
            }
            return flag;
        }
    }

    private void sendFailureReplyMsg(Messenger messenger, int requestId) {
        if (requestId != 0) {
            sendReplyMsg(messenger, 0, requestId, 0, null, null); // 0:SERVICE_MSG_GENERIC_FAILURE
        }
    }

    private void sendReplyMsg(Messenger messenger, int what, int requestId,
            int arg, Object obj, Bundle data) {
        Message message = Message.obtain();
        message.what = what;
        message.arg1 = requestId;
        message.arg2 = arg;
        message.obj = obj;
        message.setData(data);

        try {
            messenger.send(message);
            return;
        } catch (DeadObjectException de) {
            return;
        } catch (RemoteException e) {
            log.e(e, "Could not send message to "
                    + getClientConnectionInfo(messenger));
        }
    }

    private void sendDescriptorChangeEvent(
            MediaRouteProviderDescriptor descriptor) {
        Bundle bundle = null;
        if (descriptor != null) {
            bundle = descriptor.mRoutes;
        }

        int size = mFlintDeathRecipientList.size();
        for (int i = 0; i < size; i++) {
            FlintDeathRecipient deathRecipient = (FlintDeathRecipient) mFlintDeathRecipientList
                    .get(i);
            sendReplyMsg(deathRecipient.mMessenger, 5, 0, 0, bundle, null); // 5:SERVICE_MSG_DESCRIPTOR_CHANGED
            log.d(deathRecipient
                    + ": Sent descriptor change event, descriptor="
                    + descriptor);
        }

    }

    private boolean register(Messenger messenger, int requestId, int version) {
        if (version > 0 && getFlintDeathRecipitentListIndex(messenger) < 0) {
            FlintDeathRecipient deathRecipient = new FlintDeathRecipient(this,
                    messenger, version);
            if (deathRecipient.linkToDeath()) {
                mFlintDeathRecipientList.add(deathRecipient);

                log.d("MediaRouteProviderSrv", deathRecipient
                        + ": Registered, version=" + version);

                if (requestId != 0) {
                    MediaRouteProviderDescriptor descriptor = mMediaRouteProvider.mMediaRouteProviderDescriptor;
                    Bundle bundle = null;
                    if (descriptor != null) {
                        bundle = descriptor.mRoutes;
                    }

                    sendReplyMsg(messenger, 2, requestId, 1, bundle, null); // 2:SERVICE_MSG_REGISTERED
                }

                return true;
            }
        }

        return false;
    }

    private boolean unRegister(Messenger messenger, int requestId) {
        int j = getFlintDeathRecipitentListIndex(messenger);
        if (j >= 0) {
            FlintDeathRecipient deathRecipient = (FlintDeathRecipient) mFlintDeathRecipientList
                    .remove(j);

            log.d(deathRecipient + ": Unregistered");

            deathRecipient.onBinderDied();
            sendReplyMsg(messenger, requestId);

            return true;
        }

        return false;
    }

    private boolean createRouteController(Messenger messenger, int requestId,
            int controllerId, String routeId) {
        FlintDeathRecipient deathRecipient = getFlintDeathRecipient(messenger);
        if (deathRecipient != null
                && deathRecipient.checkRouteController(routeId, controllerId)) {
            log.d(deathRecipient + ": Route controller created, controllerId="
                    + controllerId + ", routeId=" + routeId);
            sendReplyMsg(messenger, requestId);

            return true;
        }

        return false;
    }

    private boolean setDiscoveryRequest(Messenger messenger, int requestId,
            DiscoveryRequest request) {
        FlintDeathRecipient deathRecipient = getFlintDeathRecipient(messenger);
        if (deathRecipient != null) {
            boolean actuallyChanged = deathRecipient
                    .setDiscoveryRequestInternal(request);

            log.d(deathRecipient + ": Set discovery request, request="
                    + request + ", actuallyChanged=" + actuallyChanged
                    + ", compositeDiscoveryRequest=" + mDiscoveryRequest);

            sendReplyMsg(messenger, requestId);

            return true;
        }

        return false;
    }

    private FlintDeathRecipient getFlintDeathRecipient(Messenger messenger) {
        int index = getFlintDeathRecipitentListIndex(messenger);
        if (index >= 0) {
            return (FlintDeathRecipient) mFlintDeathRecipientList.get(index);
        }

        return null;
    }

    private void sendReplyMsg(Messenger messenger, int requestId) {
        if (requestId != 0) {
            sendReplyMsg(messenger, 1, requestId, 0, null, null); // 1:SERVICE_MSG_GENERIC_SUCCESS
        }
    }

    private void onBinderDied(Messenger messenger) {
        int index = getFlintDeathRecipitentListIndex(messenger);
        if (index >= 0) {
            FlintDeathRecipient deathRecipient = (FlintDeathRecipient) mFlintDeathRecipientList
                    .remove(index);

            log.d(deathRecipient + ": Binder died");

            deathRecipient.onBinderDied();
        }
    }

    private boolean setDiscoveryRequest() {
        Object obj = null;
        int size = mFlintDeathRecipientList.size();
        int i = 0;
        boolean flag = false;
        DiscoveryRequest request = null;

        Bundle bundle;
        MediaRouteSelector anotherSelector;
        MediaRouteSelector selector;
        while (i < size) {
            DiscoveryRequest discoveryRequest = ((FlintDeathRecipient) mFlintDeathRecipientList
                    .get(i)).mDiscoveryRequest;
            boolean f;
            CategoriesData category;
            DiscoveryRequest reqeust;
            if (discoveryRequest != null
                    && (!discoveryRequest.getSelector().isEmpty() || discoveryRequest
                            .isActiveScan())) {
                f = flag | discoveryRequest.isActiveScan();
                if (request == null) {
                    category = (CategoriesData) obj;
                    reqeust = discoveryRequest;
                } else {

                    if (obj == null)
                        category = new CategoriesData(request.getSelector());
                    else
                        category = (CategoriesData) obj;
                    selector = discoveryRequest.getSelector();
                    if (selector == null) {
                        throw new IllegalArgumentException(
                                "selector must not be null");
                    }

                    category.addCategoryList(selector.getControlCategories());
                    reqeust = request;
                }
            } else {
                f = flag;
                category = (CategoriesData) obj;
                reqeust = request;
            }
            i++;
            request = reqeust;
            obj = category;
            flag = f;
        }
        if (obj != null) {
            if (((CategoriesData) (obj)).mControlCategories == null) {
                anotherSelector = MediaRouteSelector.EMPTY;
            } else {
                bundle = new Bundle();
                bundle.putStringArrayList("controlCategories",
                        ((CategoriesData) (obj)).mControlCategories);
                anotherSelector = new MediaRouteSelector(bundle,
                        ((CategoriesData) (obj)).mControlCategories);
            }
            request = new DiscoveryRequest(anotherSelector, flag);
        }
        if (mDiscoveryRequest == request || mDiscoveryRequest != null
                && mDiscoveryRequest.equals(request)) {
            return false;
        }

        mDiscoveryRequest = request;

        MainThreadChecker.isOnAppMainThread();

        if (mMediaRouteProvider.mDiscoveryRequest != request
                && (mMediaRouteProvider.mDiscoveryRequest == null || !mMediaRouteProvider.mDiscoveryRequest
                        .equals(request))) {
            mMediaRouteProvider.mDiscoveryRequest = request;
            if (!mMediaRouteProvider.mPendingDiscoveryRequestChange) {
                mMediaRouteProvider.mPendingDiscoveryRequestChange = true;
                mMediaRouteProvider.mHandler.sendEmptyMessage(2); // device
                                                                  // discovery
                                                                  // request
            }
        }

        return true;
    }

    private boolean releaseRouteController(Messenger messenger, int requestId,
            int controllerId) {
        FlintDeathRecipient deathRecipient = getFlintDeathRecipient(messenger);
        if (deathRecipient != null
                && deathRecipient.releaseRouteController(controllerId)) {
            log.d(deathRecipient + ": Route controller released, controllerId="
                    + controllerId);

            sendReplyMsg(messenger, requestId);
            return true;
        }

        return false;
    }

    private int getFlintDeathRecipitentListIndex(Messenger messenger) {
        int size = mFlintDeathRecipientList.size();
        for (int i = 0; i < size; i++)
            if (((FlintDeathRecipient) mFlintDeathRecipientList.get(i))
                    .isBinderEquals(messenger))
                return i;

        return -1;
    }

    private boolean selectRoute(Messenger messenger, int requestId,
            int controllerId) {
        FlintDeathRecipient deathRecipient = getFlintDeathRecipient(messenger);
        if (deathRecipient != null) {
            RouteController routeController = deathRecipient
                    .getRouteController(controllerId);
            if (routeController != null) {
                routeController.onSelect();

                log.d(deathRecipient + ": Route selected, controllerId="
                        + controllerId);

                sendReplyMsg(messenger, requestId);
                return true;
            }
        }

        return false;
    }

    private String getClientConnectionInfo(Messenger messenger) {
        return ("Client connection" + messenger.getBinder().toString());
    }

    private boolean unselectRoute(Messenger messenger, int requestId,
            int controllerId) {
        FlintDeathRecipient deathRecipient = getFlintDeathRecipient(messenger);
        if (deathRecipient != null) {
            RouteController routeController = deathRecipient
                    .getRouteController(controllerId);
            if (routeController != null) {
                routeController.onUnselect();

                log.d(deathRecipient + ": Route unselected, controllerId="
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
                    String packageName = mediaRouteProvider.mComponentName
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

            if (mMediaRouteProvider != null) {
                return mMessenger.getBinder();
            }
        }
        return null;
    }

    final class FlintDeathRecipient implements DeathRecipient {
        public final Messenger mMessenger;
        public final int mVersion;
        public DiscoveryRequest mDiscoveryRequest;
        final MediaRouteProviderSrv mMediaRouteProviderSrv;
        private final SparseArray mRouteControllerList = new SparseArray();

        public FlintDeathRecipient(MediaRouteProviderSrv routeProvider,
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
                RouteController routeController = mMediaRouteProvider
                        .getRouteController(routeId);
                if (routeController != null) {
                    mRouteControllerList.put(controllerId, routeController);
                    return true;
                }
            }
            return false;
        }

        public final boolean setDiscoveryRequestInternal(
                DiscoveryRequest request) {
            if (mDiscoveryRequest != request
                    && (mDiscoveryRequest == null || !mDiscoveryRequest
                            .equals(request))) {
                mDiscoveryRequest = request;
                return setDiscoveryRequest();
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
            setDiscoveryRequestInternal(((DiscoveryRequest) (null)));
        }

        public final void binderDied() {
            mBinderDiedHandler.obtainMessage(1, mMessenger).sendToTarget();
        }

        public final String toString() {
            return getClientConnectionInfo(mMessenger);
        }
    }

    public final class CategoriesData {
        ArrayList mControlCategories;

        public CategoriesData() {
        }

        public CategoriesData(MediaRouteSelector selector) {
            if (selector == null) {
                throw new IllegalArgumentException("selector must not be null");
            }

            MediaRouteSelector.ensureControlCategories(selector);
            if (!MediaRouteSelector.getControlCategories(selector).isEmpty()) {
                mControlCategories = new ArrayList(
                        MediaRouteSelector.getControlCategories(selector));
            }
        }

        public final CategoriesData addCategoryList(Collection categories) {
            if (categories == null) {
                throw new IllegalArgumentException(
                        "categories must not be null");
            }

            if (!categories.isEmpty()) {
                Iterator iterator = categories.iterator();
                do {
                    if (!iterator.hasNext()) {
                        break;
                    }

                    String category = (String) iterator.next();
                    if (category == null) {
                        throw new IllegalArgumentException(
                                "category must not be null");
                    }

                    if (mControlCategories == null) {
                        mControlCategories = new ArrayList();
                    }

                    if (!mControlCategories.contains(category)) {
                        mControlCategories.add(category);
                    }
                } while (true);
            }
            return this;
        }
    }
}
