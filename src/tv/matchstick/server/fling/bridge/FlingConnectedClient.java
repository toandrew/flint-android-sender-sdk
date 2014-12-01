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

package tv.matchstick.server.fling.bridge;

import tv.matchstick.client.internal.IFlingCallbacks;
import tv.matchstick.client.internal.IFlingDeviceControllerListener;
import tv.matchstick.client.internal.LOG;
import tv.matchstick.client.internal.ValueChecker;
import tv.matchstick.fling.ApplicationMetadata;
import tv.matchstick.fling.FlingDevice;
import tv.matchstick.fling.FlingStatusCodes;
import tv.matchstick.server.fling.FlingDialController;
import android.content.Context;
import android.os.IBinder.DeathRecipient;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;

/**
 * This class used to interact with app about current status(application,fling
 * device,etc)
 */
public final class FlingConnectedClient implements IFlingSrvController {
    private static final LOG log = new LOG("FlingConnectedClient");
    private FlingDeviceControllerStubImpl mStubImpl;
    private final IFlingDeviceControllerListener mFlingDeviceControllerListener;
    private final DeathRecipient mFlingCallbackDeathHandler;
    private final DeathRecipient mListenerDeathHandler;
    private final FlingDevice mFlingDevice;
    private String mLastAppId;
    private FlingDialController mFlingDialController;
    private final IFlingCallbacks mFlingCallbacks;

    /**
     * Fling Client which will be interacted with app side to do all media
     * control works
     * 
     * @param context
     * @param callbacks
     * @param device
     * @param lastApplicationId
     * @param listener
     */
    public FlingConnectedClient(Context context, IFlingCallbacks callbacks,
            FlingDevice device, String lastApplicationId,
            IFlingDeviceControllerListener listener) {
        super();

        mFlingCallbacks = (IFlingCallbacks) ValueChecker
                .checkNullPointer(callbacks);

        mFlingDevice = device;
        mLastAppId = lastApplicationId;
        mFlingDeviceControllerListener = listener;
        mStubImpl = null;

        mFlingCallbackDeathHandler = new DeathRecipient() {

            @Override
            public void binderDied() {
                handleBinderDeath(FlingConnectedClient.this);
            }

        };

        mListenerDeathHandler = new DeathRecipient() {

            @Override
            public void binderDied() {
                handleBinderDeath(FlingConnectedClient.this);
            }

        };

        /**
         * In case the device controller's listener(in app side) is dead
         */
        try {
            mFlingDeviceControllerListener.asBinder().linkToDeath(
                    mListenerDeathHandler, 0);
        } catch (RemoteException e) {
            log.e("client disconnected before listener was set");
            if (!mFlingDialController.isDisposed())
                mFlingDialController.release();
        }

        log.d("Create one fling device controller!");
        mFlingDialController = new FlingDialController(context, new Handler(
                Looper.getMainLooper()), mFlingDevice, this);
        mStubImpl = new FlingDeviceControllerStubImpl(mFlingDialController);

        /**
         * already connected?
         */
        if (mFlingDialController.isConnected()) {
            try {
                mFlingCallbacks.onPostInitComplete(FlingStatusCodes.SUCCESS,
                        mStubImpl.asBinder(), null);
            } catch (RemoteException e) {
                log.d("client died while brokering service");
            }
            return;
        }

        /**
         * is not busy on connecting to device?
         */
        if (!mFlingDialController.isConnecting()) {
            log.d(
                    "reconnecting to device with applicationId=%s", mLastAppId);
            if (mLastAppId != null)
                mFlingDialController.reconnectToDevice(mLastAppId);
            else
                mFlingDialController.connectDevice();
        }

        /**
         * in case, the flingcallback binder in app side is dead.
         */
        try {
            mFlingCallbacks.asBinder().linkToDeath(mFlingCallbackDeathHandler,
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
    static void handleBinderDeath(FlingConnectedClient client) {
        if (client.mFlingDialController != null
                && !client.mFlingDialController.isDisposed()) {
            log.w(
                    "calling releaseReference from handleBinderDeath()");
            client.mFlingDialController.release();
            log.d("Released controller.");
        }
        log.d("Removing ConnectedClient.");
        
    }

    /**
     * Invoked when connected with fling device. The app side will be notified
     * the current connected status(0)
     * 
     */
    @Override
    public final void onConnected() {
        try {
            mFlingCallbacks.onPostInitComplete(FlingStatusCodes.SUCCESS,
                    mStubImpl.asBinder(), null);
            log.d("Connected to device.");
        } catch (RemoteException remoteexception) {
            log.w(remoteexception,
                    "client died while brokering service");
        }
    }

    /**
     * Invoked when disconnected with fling device. The app side will be
     * notified the current disconnected status(0)
     * 
     * @status the disconnected reason
     */
    @Override
    public final void onDisconnected(int status) {
        log.d("onDisconnected: status=%d", status);

        try {
            mFlingDeviceControllerListener.onDisconnected(status);
        } catch (RemoteException e) {
            log.d(e.toString(),
                    "client died while brokering service");
        }

        /**
         * release resources in controller instance
         */
        if (!mFlingDialController.isDisposed()) {
            log.w("calling releaseReference from ConnectedClient.onDisconnected");
            mFlingDialController.release();
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
            mFlingDeviceControllerListener.onApplicationConnected(
                    applicationmetadata, statusText, relaunched);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when fling device's volume changed
     */
    @Override
    public final void onVolumeChanged(String status, double volume,
            boolean muteState) {
        try {
            mFlingDeviceControllerListener
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

            mFlingDeviceControllerListener
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
            mFlingCallbacks
                    .onPostInitComplete(1001, mStubImpl.asBinder(), null);
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
            mFlingDeviceControllerListener
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
            mFlingCallbacks.onPostInitComplete(FlingStatusCodes.NETWORK_ERROR,
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
            mFlingDeviceControllerListener.onRequestStatus(result);
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
            mFlingDeviceControllerListener
                    .onRequestResult(FlingStatusCodes.INVALID_REQUEST); // 2001
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
            mFlingDeviceControllerListener.onApplicationDisconnected(reason);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
