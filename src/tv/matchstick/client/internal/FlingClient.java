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

import java.util.ArrayList;

import tv.matchstick.client.common.IFlingClient;
import tv.matchstick.fling.ConnectionResult;
import tv.matchstick.fling.FlingManager;
import tv.matchstick.fling.internal.Api;
import tv.matchstick.server.fling.bridge.FlingConnectedClient;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

/**
 * Fling client interface with Fling service.
 * 
 * @author jim
 * 
 * @param <T>
 */
public abstract class FlingClient<T extends IInterface> implements
        IFlingClient, Api.ConnectionApi, FlingClientEvents.ClientEventCallback {

    static final int CONNECTION_STATUS_DISCONNECTED = 1;
    static final int CONNECTION_STATUS_CONNECTING = 2;
    static final int CONNECTION_STATUS_CONNECTED = 3;

    private final Context mContext;

    private final Looper mLooper;

    final Handler mFlingClientHandler;

    private T mService;

    private final ArrayList<CallbackProxy<?>> mCallbackProxyList = new ArrayList<CallbackProxy<?>>();

    private volatile int mConnectedState = CONNECTION_STATUS_DISCONNECTED;

    boolean mConnected = false;

    private final FlingClientEvents mClientEvent;

    private FlingConnectedClient mFlingConnectedClient;

    /**
     * constructor.
     * 
     * @param context
     * @param looper
     * @param callbacks
     * @param failedListener
     * @param strArray
     */
    protected FlingClient(Context context, Looper looper,
            FlingManager.ConnectionCallbacks callbacks,
            FlingManager.OnConnectionFailedListener failedListener,
            String[] strArray) {

        ValueChecker.checkNullPointer(context);
        mContext = context;
        ValueChecker.checkNullPointer(looper, "Looper must not be null");
        mLooper = looper;
        mClientEvent = new FlingClientEvents(context, looper, this);
        mFlingClientHandler = new FlingClientHandler(looper);

        ValueChecker.checkNullPointer(callbacks);
        registerConnectionCallbacks(callbacks);

        ValueChecker.checkNullPointer(failedListener);
        registerConnectionFailedListener(failedListener);
    }

    /**
     * Constructor.
     * 
     * @param context
     * @param callbacks
     * @param failedListener
     * @param strArray
     */
    protected FlingClient(Context context,
            IFlingClient.ConnectionCallbacks callbacks,
            IFlingClient.OnConnectionFailedListener failedListener,
            String[] strArray) {

        this(context, context.getMainLooper(), new ClientConnectionCallbacks(
                callbacks),
                new ClientOnConnectionFailedListener(failedListener), strArray);
    }

    /**
     * Service intent's action name
     * 
     * @return service intent's action name
     */
    protected abstract String getServiceName();

    /**
     * Fling service's interface descriptor.
     * 
     * @return
     */
    protected abstract String getInterfaceDescriptor();

    /**
     * Get service.
     * 
     * @param binder
     * @return
     */
    protected abstract T getService(IBinder binder);

    protected abstract FlingConnectedClient createFlingConnectedClient(
            IFlingCallbackImpl flingCallback) throws RemoteException;

    /**
     * Connect with Fling service. It's the most important API exported to Fling
     * application.
     * 
     * By Calling this function, Fling application will try to connect with
     * Fling Service.
     * 
     * Please see more info about
     * {@link tv.matchstick.client.internal.FlingClient.FlingClientServiceConnection}
     */
    public void connect() {
        if (mConnected) {
            return;
        }

        mConnected = true;
        mConnectedState = CONNECTION_STATUS_CONNECTING;

        try {
            mFlingConnectedClient = createFlingConnectedClient(new IFlingCallbackImpl(
                    this));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Is connected?
     */
    public boolean isConnected() {
        return this.mConnectedState == CONNECTION_STATUS_CONNECTED;
    }

    /**
     * Is connecting?
     */
    public boolean isConnecting() {
        return this.mConnectedState == CONNECTION_STATUS_CONNECTING;
    }

    /**
     * Disconnect with Fling service.
     * 
     * Do some clean up work.
     */
    public void disconnect() {
        mConnected = false;
        synchronized (mCallbackProxyList) {
            int size = this.mCallbackProxyList.size();
            for (int i = 0; i < size; i++) {
                this.mCallbackProxyList.get(i).reset();
            }
            this.mCallbackProxyList.clear();
        }
        mFlingConnectedClient = null;
        mConnectedState = CONNECTION_STATUS_DISCONNECTED;
        mService = null;

        mFlingClientHandler.sendMessage(mFlingClientHandler.obtainMessage(
                MSG_WHAT_DISCONNECTED, Integer.valueOf(1)));
    }

    /**
     * Called when disconnected from Fling service.
     * 
     * @param reason
     */
    public void sendDisconnectedMessage(int reason) {
        this.mFlingClientHandler.sendMessage(this.mFlingClientHandler
                .obtainMessage(MSG_WHAT_DISCONNECTED, Integer.valueOf(reason)));
    }

    /**
     * Get context.
     * 
     * @return
     */
    public final Context getContext() {
        return this.mContext;
    }

    /**
     * Get looper
     */
    public final Looper getLooper() {
        return this.mLooper;
    }

    /**
     * After connect with Fling service, called to show current connect status
     * 
     * @param statusCode
     * @param binder
     * @param bundle
     */
    protected void onPostInitResult(int statusCode, IBinder binder,
            Bundle bundle) {
        this.mFlingClientHandler.sendMessage(this.mFlingClientHandler
                .obtainMessage(1, new FlingClientCallbackProxy(statusCode,
                        binder, bundle)));
    }

    /**
     * Check connected status.
     */
    protected final void checkConnected() {
        if (!isConnected()) {
            throw new IllegalStateException(
                    "Not connected. Call connect() and wait for onConnected() to be called.");
        }
    }

    public Bundle getBundle() {
        return null;
    }

    /**
     * Get current connected Fling service.
     * 
     * @return current connected Fling service.
     */
    protected final T getService() {
        checkConnected();
        return this.mService;
    }

    /**
     * Add connection call back function. Useless.Not called by others?!!!!
     * 
     * @param callback
     */
    public final void addCallback(CallbackProxy<?> callback) {
        synchronized (this.mCallbackProxyList) {
            this.mCallbackProxyList.add(callback);
        }
        this.mFlingClientHandler.sendMessage(this.mFlingClientHandler
                .obtainMessage(2, callback));
    }

    /**
     * Whether client can receive event.
     */
    public boolean canReceiveEvent() {
        return this.mConnected;
    }

    /**
     * register connection callback function.
     * 
     * @param callbacks
     *            connection callback
     *            function(onConnected/onConnectionSuspended)
     */
    public void registerConnectionCallbacks(
            FlingManager.ConnectionCallbacks callbacks) {
        this.mClientEvent.registerConnectionCallbacks(callbacks);
    }

    /**
     * Register connection failed listener.
     */
    public void registerConnectionFailedListener(
            FlingManager.OnConnectionFailedListener listener) {
        this.mClientEvent.registerConnectionFailedListener(listener);
    }

    /**
     * register connection callback.
     */
    public void registerConnectionCallbacks(
            IFlingClient.ConnectionCallbacks callbacks) {
        this.mClientEvent
                .registerConnectionCallbacks(new ClientConnectionCallbacks(
                        callbacks));
    }

    /**
     * is connection callback register.
     */
    public boolean isConnectionCallbacksRegistered(
            IFlingClient.ConnectionCallbacks callbacks) {
        return this.mClientEvent
                .isConnectionCallbacksRegistered(new ClientConnectionCallbacks(
                        callbacks));
    }

    /**
     * unregister callback function.
     */
    public void unregisterConnectionCallbacks(
            IFlingClient.ConnectionCallbacks callbacks) {
        this.mClientEvent
                .unregisterConnectionCallbacks(new ClientConnectionCallbacks(
                        callbacks));
    }

    /**
     * register connection failed listener.
     */
    public void registerConnectionFailedListener(
            IFlingClient.OnConnectionFailedListener listener) {
        this.mClientEvent.registerConnectionFailedListener(listener);
    }

    /**
     * check whether connection failed listener is registered.
     */
    public boolean isConnectionFailedListenerRegistered(
            IFlingClient.OnConnectionFailedListener listener) {
        return this.mClientEvent.isConnectionFailedListenerRegistered(listener);
    }

    /**
     * unregister connection failed listener.
     */
    public void unregisterConnectionFailedListener(
            IFlingClient.OnConnectionFailedListener listener) {
        this.mClientEvent.unregisterConnectionFailedListener(listener);
    }

    /**
     * Fling callback implementation. which will be called when
     * connect/disconnect (ok/failed) with Fling service.
     * 
     * @author jim
     * 
     */
    public static final class IFlingCallbackImpl extends IFlingCallbacks.Stub {

        private FlingClient flingClient;

        /**
         * constructor.
         * 
         * @param client
         *            Fling client.
         */
        public IFlingCallbackImpl(FlingClient client) {
            flingClient = client;
        }

        /**
         * Called when client connected(ok/failed) with Fling service.
         */
        public void onPostInitComplete(int statusCode, IBinder binder,
                Bundle bundle) {

            ValueChecker
                    .checkNullPointer(flingClient,
                            "onPostInitComplete can be called only once per call to getServiceFromBroker");

            // return current connect status.
            flingClient.onPostInitResult(statusCode, binder, bundle);
            flingClient = null;
        }
    }

    public static final class ClientOnConnectionFailedListener implements
            FlingManager.OnConnectionFailedListener {
        private final IFlingClient.OnConnectionFailedListener failedListener;

        public ClientOnConnectionFailedListener(
                IFlingClient.OnConnectionFailedListener listener) {
            this.failedListener = listener;
        }

        public void onConnectionFailed(ConnectionResult result) {
            this.failedListener.onConnectionFailed(result);
        }

        public boolean equals(Object other) {
            if ((other instanceof ClientOnConnectionFailedListener)) {
                return this.failedListener
                        .equals(((ClientOnConnectionFailedListener) other).failedListener);
            }
            return this.failedListener.equals(other);
        }
    }

    public static final class ClientConnectionCallbacks implements
            FlingManager.ConnectionCallbacks {
        private final IFlingClient.ConnectionCallbacks callback;

        public ClientConnectionCallbacks(
                IFlingClient.ConnectionCallbacks callback) {
            this.callback = callback;
        }

        public void onConnected(Bundle connectionHint) {
            this.callback.onConnected(connectionHint);
        }

        public void onConnectionSuspended(int cause) {
            this.callback.onDisconnected();
        }

        public boolean equals(Object other) {
            if ((other instanceof ClientConnectionCallbacks)) {
                return this.callback
                        .equals(((ClientConnectionCallbacks) other).callback);
            }
            return this.callback.equals(other);
        }
    }

    protected abstract class CallbackProxy<TListener> {
        private TListener mListener;
        private boolean isUsed;

        public CallbackProxy(TListener listener) {
            this.mListener = listener;
            this.isUsed = false;
        }

        protected abstract void onCallback(TListener listener);

        protected abstract void onFailed();

        public void call() {
            TListener listener;
            synchronized (this) {
                listener = this.mListener;
                if (this.isUsed) {
                    Log.w("FlingClient", "Callback proxy " + this
                            + " being reused. This is not safe.");
                }
            }
            if (listener != null) {
                try {
                    onCallback(listener);
                } catch (RuntimeException re) {
                    onFailed();
                    throw re;
                }
            } else {
                onFailed();
            }
            synchronized (this) {
                this.isUsed = true;
            }
            unregister();
        }

        public void unregister() {
            reset();
            synchronized (mCallbackProxyList) {
                mCallbackProxyList.remove(this);
            }
        }

        public void reset() {
            synchronized (this) {
                mListener = null;
            }
        }
    }

    public final class FlingClientCallbackProxy extends CallbackProxy<Boolean> {
        public final int statusCode;
        public final Bundle bundle;
        public final IBinder binder;

        public FlingClientCallbackProxy(int statusCode, IBinder binder,
                Bundle bundle) {
            super(Boolean.valueOf(true));
            this.statusCode = statusCode;
            this.binder = binder;
            this.bundle = bundle;
        }

        protected void onFailed() {
        }

        protected void onCallback(Boolean listener) {
            if (listener == null) {
                mConnectedState = CONNECTION_STATUS_DISCONNECTED;
                return;
            }
            switch (this.statusCode) {
            case 0:
                String desc = null;
                try {
                    desc = this.binder.getInterfaceDescriptor();
                    if (getInterfaceDescriptor().equals(desc)) {
                        mService = getService(this.binder);
                        if (mService != null) {
                            mConnectedState = CONNECTION_STATUS_CONNECTED;
                            mClientEvent.notifyOnConnected();
                            return;
                        }
                    }
                } catch (RemoteException e) {
                }
                mConnected = false;
                mFlingClientHandler.sendMessage(mFlingClientHandler
                        .obtainMessage(MSG_WHAT_DISCONNECTED,
                                Integer.valueOf(1)));
                mConnectedState = CONNECTION_STATUS_DISCONNECTED;
                mService = null;
                mClientEvent.notifyOnConnectionFailed(new ConnectionResult(8,
                        null));
                break;
            case 10:
                mConnectedState = CONNECTION_STATUS_DISCONNECTED;
                throw new IllegalStateException(
                        "A fatal developer error has occurred. Check the logs for further information.");
            default:
                PendingIntent pendingIntent = null;
                if (this.bundle != null) {
                    pendingIntent = (PendingIntent) this.bundle
                            .getParcelable("pendingIntent");
                }
                mConnected = false;
                mFlingClientHandler.sendMessage(mFlingClientHandler
                        .obtainMessage(MSG_WHAT_DISCONNECTED,
                                Integer.valueOf(1)));
                mConnectedState = CONNECTION_STATUS_DISCONNECTED;
                mService = null;
                mClientEvent.notifyOnConnectionFailed(new ConnectionResult(
                        this.statusCode, pendingIntent));
            }

        }
    }

    static final int MSG_WHAT_CONNECTION_FAILED = 3;
    static final int MSG_WHAT_DISCONNECTED = 4;

    /**
     * This handler will process all client connected/disconnected related
     * messages from Fling service.
     * 
     * Used when: 1. connect/disconnect success result with Fling service 2.
     * connect/disconnect failed result.
     */
    final class FlingClientHandler extends Handler {
        public FlingClientHandler(Looper looper) {
            super();
        }

        @Override
        public void handleMessage(Message msg) {
            if ((msg.what == 2) || (msg.what == 1)) {
                if (!isConnecting()) {
                    CallbackProxy cb = (CallbackProxy) msg.obj;
                    cb.onFailed();
                    cb.unregister();
                    return;
                } else {
                    CallbackProxy cb = (CallbackProxy) msg.obj;
                    cb.call(); // when connected with Fling service(device),
                               // come to here.
                    return;
                }
            }

            // when connect failed.
            if (msg.what == MSG_WHAT_CONNECTION_FAILED) {
                mClientEvent.notifyOnConnectionFailed(new ConnectionResult(
                        ((Integer) msg.obj).intValue(), null));
                return;
            }

            // when disconnect from service.
            if (msg.what == MSG_WHAT_DISCONNECTED) {
                mConnectedState = CONNECTION_STATUS_DISCONNECTED;
                mService = null;
                mClientEvent.notifyOnConnectionSuspended(((Integer) msg.obj)
                        .intValue());
                return;
            }
            Log.wtf("FlingClient", "Don't know how to handle this message.");
        }
    }
}
