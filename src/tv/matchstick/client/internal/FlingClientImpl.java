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

package tv.matchstick.client.internal;

import java.util.HashMap;
import java.util.Map;

import tv.matchstick.client.common.api.StatusCodes;
import tv.matchstick.fling.ApplicationMetadata;
import tv.matchstick.fling.Fling;
import tv.matchstick.fling.Fling.ApplicationConnectionResult;
import tv.matchstick.fling.FlingDevice;
import tv.matchstick.fling.FlingManager;
import tv.matchstick.fling.FlingStatusCodes;
import tv.matchstick.fling.ResultCallback;
import tv.matchstick.fling.Status;
import tv.matchstick.server.fling.bridge.FlingConnectedClient;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.text.TextUtils;

/**
 * Fling client implementation.
 * 
 * This class will communicate with Fling service.
 */
public class FlingClientImpl extends FlingClient<IFlingDeviceController> {
    private static final LOG log = new LOG("FlingClientImpl");

    private static final Object mResultLock = new Object();
    private static final Object mStatusResultLock = new Object();

    private final FlingDevice mFlingDevice;

    private final Fling.Listener mFlingListener;

    private final Handler mHandler;

    private final IFlingDeviceControllerListener mIFlingDeviceControllerListener;

    private final Map<String, Fling.MessageReceivedCallback> mMessageReceivedCallbacksMap;

    private ApplicationMetadata mApplicationMetadata;

    private String mApplicationStatus;

    private boolean mIsMute;

    private boolean mFirstStatusUpdate;

    private boolean mIsConnectedDevice;

    private double mVolume;

    private String mApplicationId;

    private Bundle mExtraMessage;

    private ResultCallback<Fling.ApplicationConnectionResult> mResultCallback;

    private ResultCallback<Status> mStatusResultCallback;

    /**
     * Constructor.
     * 
     * @param context
     * @param looper
     * @param device
     * @param flingListener
     * @param callbacks
     */
    public FlingClientImpl(Context context, Looper looper, FlingDevice device,
            Fling.Listener flingListener,
            FlingManager.ConnectionCallbacks callbacks) {

        super(context, looper, callbacks, null);

        this.mFlingDevice = device;
        this.mFlingListener = flingListener;
        this.mHandler = new Handler(looper);
        this.mMessageReceivedCallbacksMap = new HashMap<String, Fling.MessageReceivedCallback>();
        this.mIsConnectedDevice = false;
        this.mApplicationMetadata = null;
        this.mApplicationStatus = null;
        this.mVolume = 0.0D;
        this.mIsMute = false;

        /**
         * Callback listener for device controller.
         * 
         * Those callback function include
         * onDisconnected,onApplicationConnected,
         * postApplicationConnectionResult,etc.
         */
        this.mIFlingDeviceControllerListener = new IFlingDeviceControllerListener.Stub() {
            @Override
            public void onDisconnected(int statusCode) {
                log.d("IFlingDeviceControllerListener.onDisconnected: %d",
                        statusCode);
                mIsConnectedDevice = false;
                mApplicationMetadata = null;
                if (statusCode != StatusCodes.SUCCESS) {
                    sendDisconnectedMessage(FlingManager.ConnectionCallbacks.CAUSE_NETWORK_LOST);
                }
            }

            /**
             * Application connected.
             */
            @Override
            public void onApplicationConnected(ApplicationMetadata data,
                    String statusText, boolean relaunched) {
                mApplicationMetadata = data;
                synchronized (mResultLock) {
                    if (mResultCallback != null) {
                        mResultCallback
                                .onResult(new ApplicationConnectionResultImpl(
                                        new Status(StatusCodes.SUCCESS), data,
                                        statusText, relaunched));
                        mResultCallback = null;
                    }
                }
            }

            @Override
            public void postApplicationConnectionResult(int statusCode) {
                synchronized (mResultLock) {
                    if (mResultCallback != null) {
                        mResultCallback
                                .onResult(new ApplicationConnectionResultImpl(
                                        new Status(statusCode)));
                        mResultCallback = null;
                    }
                }
            }

            @Override
            public void onRequestResult(int result) {
                notifyCallback(result);
            }

            @Override
            public void onRequestStatus(int status) {
                notifyCallback(status);
            }

            /**
             * Application disconnected.
             */
            @Override
            public void onApplicationDisconnected(final int statusCode) {
                mApplicationId = null;
                if (notifyCallback(statusCode)) {
                    return;
                }
                if (mFlingListener == null) {
                    return;
                }
                mHandler.post(new Runnable() {
                    public void run() {
                        if (mFlingListener != null) {
                            mFlingListener
                                    .onApplicationDisconnected(statusCode);
                        }
                    }
                });
            }

            /**
             * notify application status or volume changed event.
             */
            @Override
            public void notifyApplicationStatusOrVolumeChanged(
                    final String applicationStatus, final double volume,
                    final boolean isMute) {
                mHandler.post(new Runnable() {
                    public void run() {
                        notifyFlingListener(applicationStatus, volume, isMute);
                    }
                });
            }

            /**
             * Message received.
             */
            @Override
            public void onMessageReceived(final String namespace,
                    final String message) {
                mHandler.post(new Runnable() {
                    public void run() {
                        Fling.MessageReceivedCallback callback;
                        synchronized (mMessageReceivedCallbacksMap) {
                            callback = mMessageReceivedCallbacksMap
                                    .get(namespace);
                        }
                        if (callback != null) {
                            callback.onMessageReceived(mFlingDevice, namespace,
                                    message);
                        } else {
                            log.d("Discarded message for unknown namespace '%s'",
                                    namespace);
                        }
                    }
                });
            }

            private boolean notifyCallback(int statusCode) {
                synchronized (mStatusResultLock) {
                    if (mStatusResultCallback != null) {
                        mStatusResultCallback.onResult(new Status(statusCode));
                        mStatusResultCallback = null;
                        return true;
                    }
                }
                return false;
            }

        };
    }

    @Override
    protected void onPostInitResult(int statusCode, IBinder binder,
            Bundle bundle) {
        if ((statusCode == FlingStatusCodes.SUCCESS)
                || (statusCode == FlingStatusCodes.CONNECTED_WITHOUT_APP)) {
            mIsConnectedDevice = true;
            mFirstStatusUpdate = true;
        } else {
            mIsConnectedDevice = false;
        }
        int status = statusCode;
        if (statusCode == FlingStatusCodes.CONNECTED_WITHOUT_APP) {
            mExtraMessage = new Bundle();
            mExtraMessage.putBoolean(
                    "tv.matchstick.fling.EXTRA_APP_NO_LONGER_RUNNING", true);
            status = 0;
        }
        super.onPostInitResult(status, binder, bundle);
    }

    @Override
    public void disconnect() {
        try {
            if (isConnected()) {
                synchronized (this.mMessageReceivedCallbacksMap) {
                    mMessageReceivedCallbacksMap.clear();
                }
                getService().disconnect();
            }
        } catch (RemoteException e) {
            log.d("Error while disconnecting the controller interface: %s",
                    e.getMessage());
        } finally {
            super.disconnect();
        }
    }

    @Override
    public Bundle getBundle() {
        if (mExtraMessage != null) {
            Bundle bundle = mExtraMessage;
            mExtraMessage = null;
            return bundle;
        }
        return super.getBundle();
    }

    /**
     * Send message to Fling device.
     * 
     * @param namespace
     * @param payloadMessage
     * @param callback
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws RemoteException
     */
    public void sendMessage(String namespace, String payloadMessage,
            ResultCallback<Status> callback) throws IllegalArgumentException,
            IllegalStateException, RemoteException {

        if (TextUtils.isEmpty(payloadMessage)) {
            throw new IllegalArgumentException(
                    "The message payload cannot be null or empty");
        }

        if ((namespace == null) || (namespace.length() > 128)) {
            throw new IllegalArgumentException("Invalid namespace length");
        }

        if (payloadMessage.length() > 65536) {
            throw new IllegalArgumentException("Message exceeds maximum size");
        }

        checkConnectedDeviceThrowable();
        getService().sendMessage(namespace, payloadMessage);
    }

    /**
     * Launch application.
     * 
     * @param applicationId
     * @param relaunchFlag
     * @param callback
     * @throws IllegalStateException
     * @throws RemoteException
     */
    public void launchApplication(String applicationId, boolean relaunchFlag,
            ResultCallback<Fling.ApplicationConnectionResult> callback)
            throws IllegalStateException, RemoteException {
        setApplicationConnectionResultCallback(callback);
        getService().launchApplication(applicationId, relaunchFlag);
    }

    /**
     * Join application
     * 
     * @param applicationId
     * @param callback
     * @throws IllegalStateException
     * @throws RemoteException
     */
    public void joinApplication(String applicationId,
            ResultCallback<Fling.ApplicationConnectionResult> callback)
            throws IllegalStateException, RemoteException {
        setApplicationConnectionResultCallback(callback);
        getService().joinApplication(applicationId);
    }

    /**
     * Set application connection result callback.
     * 
     * @param callback
     */
    private void setApplicationConnectionResultCallback(
            ResultCallback<Fling.ApplicationConnectionResult> callback) {
        synchronized (mResultLock) {
            if (mResultCallback != null) {
                mResultCallback.onResult(new ApplicationConnectionResultImpl(
                        new Status(FlingStatusCodes.CANCELED)));
            }
            mResultCallback = callback;
        }
    }

    /**
     * leave application.
     * 
     * @param callback
     * @throws IllegalStateException
     * @throws RemoteException
     */
    public void leaveApplication(ResultCallback<Status> callback)
            throws IllegalStateException, RemoteException {
        setStatusCallback(callback);
        getService().leaveApplication();
    }

    /**
     * Stop application.
     * 
     * @param paramc
     * @throws IllegalStateException
     * @throws RemoteException
     */
    public void stopApplication(ResultCallback<Status> paramc)
            throws IllegalStateException, RemoteException {
        setStatusCallback(paramc);
        getService().stopApplication();
    }

    /**
     * Set status callback.
     * 
     * @param callback
     */
    private void setStatusCallback(ResultCallback<Status> callback) {
        synchronized (mStatusResultLock) {
            if (mStatusResultCallback != null) {
                callback.onResult(new Status(FlingStatusCodes.INVALID_REQUEST));
            } else {
                mStatusResultCallback = callback;
            }
        }
    }

    /**
     * requestStatus.
     * 
     * @throws IllegalStateException
     * @throws RemoteException
     */
    public void requestStatus() throws IllegalStateException, RemoteException {
        getService().requestStatus();
    }

    /**
     * Set volume.
     * 
     * @param volume
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws RemoteException
     */
    public void setVolume(double volume) throws IllegalArgumentException,
            IllegalStateException, RemoteException {
        if ((Double.isInfinite(volume)) || Double.isNaN(volume)) {
            throw new IllegalArgumentException("Volume cannot be " + volume);
        }
        getService().setVolume(volume, mIsMute);
    }

    /**
     * Set mute or not.
     * 
     * @param mute
     * @throws IllegalStateException
     * @throws RemoteException
     */
    public void setMute(boolean mute) throws IllegalStateException,
            RemoteException {
        getService().setVolume(mVolume, mute);
    }

    /**
     * Get volume.
     * 
     * @return
     * @throws IllegalStateException
     */
    public double getVolume() throws IllegalStateException {
        checkConnectedDeviceThrowable();
        return mVolume;
    }

    /**
     * Get current mute status.
     * 
     * @return
     * @throws IllegalStateException
     */
    public boolean isMute() throws IllegalStateException {
        checkConnectedDeviceThrowable();
        return mIsMute;
    }

    /**
     * Set message received callback.
     * 
     * @param channelNameSpace
     * @param callback
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws RemoteException
     */
    public void setMessageReceivedCallbacks(String channelNameSpace,
            Fling.MessageReceivedCallback callback)
            throws IllegalArgumentException, IllegalStateException,
            RemoteException {

        if (TextUtils.isEmpty(channelNameSpace)) {
            throw new IllegalArgumentException(
                    "Channel namespace cannot be null or empty");
        }

        removeMessageReceivedCallbacks(channelNameSpace);
        if (callback == null) {
            return;
        }

        synchronized (mMessageReceivedCallbacksMap) {
            mMessageReceivedCallbacksMap.put(channelNameSpace, callback);
        }
        getService().setMessageReceivedCallbacks(channelNameSpace);
    }

    /**
     * remove message received callback.
     * 
     * @param channelNameSpace
     * @throws IllegalArgumentException
     * @throws RemoteException
     */
    public void removeMessageReceivedCallbacks(String channelNameSpace)
            throws IllegalArgumentException, RemoteException {

        if (TextUtils.isEmpty(channelNameSpace)) {
            throw new IllegalArgumentException(
                    "Channel namespace cannot be null or empty");
        }

        Fling.MessageReceivedCallback callback;
        synchronized (mMessageReceivedCallbacksMap) {
            callback = mMessageReceivedCallbacksMap.remove(channelNameSpace);
        }

        if (callback == null)
            return;

        try {
            getService().removeMessageReceivedCallbacks(channelNameSpace);
        } catch (IllegalStateException e) {
            log.dd(e, "Error unregistering namespace (%s): %s",
                    channelNameSpace, e.getMessage());
        }
    }

    /**
     * Get Application meta data.
     * 
     * @return
     * @throws IllegalStateException
     */
    public ApplicationMetadata getApplicationMetadata()
            throws IllegalStateException {
        checkConnectedDeviceThrowable();
        return mApplicationMetadata;
    }

    /**
     * Get application status.
     * 
     * @return
     * @throws IllegalStateException
     */
    public String getApplicationStatus() throws IllegalStateException {
        checkConnectedDeviceThrowable();
        return mApplicationStatus;
    }

    /**
     * Called when application status or volume changed.
     * 
     * @param applicationStatus
     * @param volume
     * @param isMute
     */
    private void notifyFlingListener(String applicationStatus, double volume,
            boolean isMute) {
        boolean hasChange = false;
        if (!(DoubleAndLongConverter.compare(applicationStatus,
                mApplicationStatus))) {
            mApplicationStatus = applicationStatus;
            hasChange = true;
        }
        if ((mFlingListener != null) && (hasChange || mFirstStatusUpdate)) {
            mFlingListener.onApplicationStatusChanged();
        }

        hasChange = false;
        if (volume != mVolume) {
            mVolume = volume;
            hasChange = true;
        }
        if (isMute != mIsMute) {
            mIsMute = isMute;
            hasChange = true;
        }

        log.d("hasChange=%b, mFirstStatusUpdate=%b", hasChange,
                mFirstStatusUpdate);
        if ((mFlingListener != null) && (hasChange || mFirstStatusUpdate)) {
            mFlingListener.onVolumeChanged();
        }
        mFirstStatusUpdate = false;
    }

    /**
     * Check whether it's connected.
     * 
     * @throws IllegalStateException
     */
    private void checkConnectedDeviceThrowable() throws IllegalStateException {
        if (!mIsConnectedDevice) {
            throw new IllegalStateException("not connected to a device");
        }
    }

    /**
     * Application connection result implementation.
     * 
     * @author jim
     * 
     */
    private static final class ApplicationConnectionResultImpl implements
            ApplicationConnectionResult {
        private final Status status;
        private final ApplicationMetadata data;
        private final String applicationStatus;
        private final boolean wasLaunched;

        public ApplicationConnectionResultImpl(Status status,
                ApplicationMetadata applicationMetadata,
                String applicationStatus, boolean wasLaunched) {
            this.status = status;
            this.data = applicationMetadata;
            this.applicationStatus = applicationStatus;
            this.wasLaunched = wasLaunched;
        }

        public ApplicationConnectionResultImpl(Status status) {
            this(status, null, null, false);
        }

        public Status getStatus() {
            return this.status;
        }

        public ApplicationMetadata getApplicationMetadata() {
            return this.data;
        }

        public String getApplicationStatus() {
            return this.applicationStatus;
        }

        public boolean getWasLaunched() {
            return this.wasLaunched;
        }
    }

    @Override
    protected String getInterfaceDescriptor() {
        return "tv.matchstick.client.internal.IFlingDeviceController";
    }

    @Override
    protected IFlingDeviceController getService(IBinder binder) {
        return IFlingDeviceController.Stub.asInterface(binder);
    }

    /**
     * Ready to init Fling service.
     * 
     * After this, the Fling service will create one client session which will
     * communicate with this Fling client.
     */
    @Override
    protected synchronized FlingConnectedClient createFlingConnectedClient(
            IFlingCallbackImpl flingCallback) throws RemoteException {
        log.d("getServiceFromBroker(): mLastApplicationId=%s", mApplicationId);

        /**
         * Init fling service.
         */
        return new FlingConnectedClient(getContext(),
                (IFlingCallbacks) flingCallback, mFlingDevice, mApplicationId,
                mIFlingDeviceControllerListener);
    }
}
