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

package tv.matchstick.server.flint.bridge;

import tv.matchstick.client.internal.IFlintCallbacks;
import tv.matchstick.client.internal.IFlintDeviceControllerListener;
import tv.matchstick.client.internal.LOG;
import tv.matchstick.client.internal.ValueChecker;
import tv.matchstick.flint.ApplicationMetadata;
import tv.matchstick.flint.ConnectionResult;
import tv.matchstick.flint.FlintDevice;
import tv.matchstick.flint.FlintStatusCodes;
import tv.matchstick.server.flint.FlintDialController;
import android.content.Context;
import android.os.IBinder.DeathRecipient;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;

/**
 * This class used to interact with app about current status(application,flint
 * device,etc)
 */
public final class FlintConnectedClient implements IFlintSrvController {
    private static final LOG log = new LOG("FlintConnectedClient");
    private FlintDeviceControllerStubImpl mStubImpl;
    private final IFlintDeviceControllerListener mFlintDeviceControllerListener;
    private final DeathRecipient mFlintCallbackDeathHandler;
    private final DeathRecipient mListenerDeathHandler;
    private final FlintDevice mFlintDevice;
    private String mLastAppId;
    private FlintDialController mFlintDialController;
    private final IFlintCallbacks mFlintCallbacks;

    /**
     * Flint Client which will be interacted with app side to do all media
     * control works
     * 
     * @param context
     * @param callbacks
     * @param device
     * @param lastApplicationId
     * @param listener
     */
    public FlintConnectedClient(Context context, IFlintCallbacks callbacks,
            FlintDevice device, String lastApplicationId,
            IFlintDeviceControllerListener listener) {
        super();

        mFlintCallbacks = (IFlintCallbacks) ValueChecker
                .checkNullPointer(callbacks);

        mFlintDevice = device;
        mLastAppId = lastApplicationId;
        mFlintDeviceControllerListener = listener;
        mStubImpl = null;

        mFlintCallbackDeathHandler = new DeathRecipient() {

            @Override
            public void binderDied() {
                handleBinderDeath(FlintConnectedClient.this);
            }

        };

        mListenerDeathHandler = new DeathRecipient() {

            @Override
            public void binderDied() {
                handleBinderDeath(FlintConnectedClient.this);
            }

        };

        /**
         * In case the device controller's listener(in app side) is dead
         */
        try {
            mFlintDeviceControllerListener.asBinder().linkToDeath(
                    mListenerDeathHandler, 0);
        } catch (RemoteException e) {
            log.e("client disconnected before listener was set");
            if (!mFlintDialController.isDisposed())
                mFlintDialController.release();
        }

        log.d("Create one flint device controller!");
        mFlintDialController = new FlintDialController(context, new Handler(
                Looper.getMainLooper()), mFlintDevice, this);
        mStubImpl = new FlintDeviceControllerStubImpl(mFlintDialController);

        /**
         * already connected?
         */
        if (mFlintDialController.isConnected()) {
            try {
                mFlintCallbacks.onPostInitComplete(ConnectionResult.SUCCESS,
                        mStubImpl.asBinder(), null);
            } catch (RemoteException e) {
                log.d("client died while brokering service");
            }
            return;
        }

        /**
         * is not busy on connecting to device?
         */
        if (!mFlintDialController.isConnecting()) {
            log.d(
                    "reconnecting to device with applicationId=%s", mLastAppId);
            if (mLastAppId != null)
                mFlintDialController.reconnectToDevice(mLastAppId);
            else
                mFlintDialController.connectDevice();
        }

        /**
         * in case, the flintcallback binder in app side is dead.
         */
        try {
            mFlintCallbacks.asBinder().linkToDeath(mFlintCallbackDeathHandler,
                    0);
        } catch (RemoteException e) {
            log.w("Unable to link listener reaper");
        }
    }

    /**
     * Do some cleanup work when the app is dead
     * 
     * @param client
     */
    static void handleBinderDeath(FlintConnectedClient client) {
        if (client.mFlintDialController != null
                && !client.mFlintDialController.isDisposed()) {
            log.w(
                    "calling releaseReference from handleBinderDeath()");
            client.mFlintDialController.release();
            log.d("Released controller.");
        }
        log.d("Removing ConnectedClient.");
        
    }

    /**
     * Invoked when connected with flint device. The app side will be notified
     * the current connected status(0)
     * 
     */
    @Override
    public final void onConnected() {
        try {
            mFlintCallbacks.onPostInitComplete(ConnectionResult.SUCCESS,
                    mStubImpl.asBinder(), null);
            log.d("Connected to device.");
        } catch (RemoteException remoteexception) {
            log.w(remoteexception,
                    "client died while brokering service");
        }
    }

    /**
     * Invoked when disconnected with flint device. The app side will be
     * notified the current disconnected status(0)
     * 
     * @status the disconnected reason
     */
    @Override
    public final void onDisconnected(int status) {
        log.d("onDisconnected: status=%d", status);

        try {
            mFlintDeviceControllerListener.onDisconnected(status);
        } catch (RemoteException e) {
            log.d(e.toString(),
                    "client died while brokering service");
        }

        /**
         * release resources in controller instance
         */
        if (!mFlintDialController.isDisposed()) {
            log.w("calling releaseReference from ConnectedClient.onDisconnected");
            mFlintDialController.release();
        }
    }

    /**
     * Called when disconnected with application
     */
    @Override
    public final void onApplicationConnected(
            ApplicationMetadata applicationmetadata, String statusText,
            String sessionId, boolean relaunched) {
        // mLastAppId = applicationmetadata.getApplicationId();
        try {
            mFlintDeviceControllerListener.onApplicationConnected(
                    applicationmetadata, statusText, relaunched);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when flint device's volume changed
     */
    @Override
    public final void onVolumeChanged(String status, double volume,
            boolean muteState) {
        try {
            mFlintDeviceControllerListener
                    .notifyApplicationStatusOrVolumeChanged(status, volume,
                            muteState);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * notified app when message received
     */
    @Override
    public final void notifyOnMessageReceived(String namespace, String message) {
        try {

            mFlintDeviceControllerListener
                    .onMessageReceived(namespace, message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * notify app when connected to device without app
     */
    @Override
    public final void onConnectedWithoutApp() {
        try {
            mFlintCallbacks
                    .onPostInitComplete(FlintStatusCodes.CONNECTED_WITHOUT_APP, mStubImpl.asBinder(), null);
            log.d("Connected to device without app.");
        } catch (RemoteException e) {
            e.printStackTrace();
            log.d(e.toString(),
                    "client died while brokering service");
        }
    }

    /**
     * notify app when failed connected with application
     */
    @Override
    public final void onApplicationConnectionFailed(int reason) {
        try {
            mFlintDeviceControllerListener
                    .postApplicationConnectionResult(reason);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * notify app when failed to connect to device
     */
    @Override
    public final void onConnectionFailed() {
        try {
            mFlintCallbacks.onPostInitComplete(ConnectionResult.NETWORK_ERROR,
                    null, null);
        } catch (RemoteException e) {
            e.printStackTrace();
            log.d(e.toString(),
                    "client died while brokering service");
        }
    }

    /**
     * notify app the current request status
     */
    @Override
    public final void onRequestStatus(int result) {
        try {
            mFlintDeviceControllerListener.onRequestStatus(result);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * notify app that the request is invalid
     */
    @Override
    public final void onInvalidRequest() {
        try {
            mFlintDeviceControllerListener
                    .onRequestResult(FlintStatusCodes.INVALID_REQUEST); // 2001
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * notify app that application is disconnected for some reason
     */
    @Override
    public final void onApplicationDisconnected(int reason) {
        try {
            mFlintDeviceControllerListener.onApplicationDisconnected(reason);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
