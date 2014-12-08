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

import tv.matchstick.client.common.IFlintClient;
import tv.matchstick.flint.ConnectionResult;
import tv.matchstick.flint.FlintManager;
import tv.matchstick.flint.internal.Api;
import tv.matchstick.server.flint.bridge.FlintConnectedClient;
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
 * Flint client interface with Flint service.
 * 
 * @author jim
 * 
 * @param <T>
 */
public abstract class FlintClient<T extends IInterface> implements
        IFlintClient, Api.ConnectionApi, FlintClientEvents.ClientEventCallback {

    private static final int MSG_WHAT_CONNECTION_CALLBACK = 1;
    private static final int MSG_WHAT_CONNECTION_FAILED = 2;
    private static final int MSG_WHAT_DISCONNECTED = 3;

    private static final int CONNECTION_STATUS_DISCONNECTED = 1;
    private static final int CONNECTION_STATUS_CONNECTING = 2;
    private static final int CONNECTION_STATUS_CONNECTED = 3;

    private final Context mContext;

    private final Looper mLooper;

    private final Handler mFlintClientHandler;

    private T mService;

    private final ArrayList<CallbackProxy<?>> mCallbackProxyList = new ArrayList<CallbackProxy<?>>();

    private volatile int mConnectedState = CONNECTION_STATUS_DISCONNECTED;

    private boolean mConnected = false;

    private final FlintClientEvents mClientEvent;

    private FlintConnectedClient mFlintConnectedClient;

    /**
     * constructor.
     * 
     * @param context
     * @param looper
     * @param callbacks
     * @param strArray
     */
    protected FlintClient(Context context, Looper looper,
            FlintManager.ConnectionCallbacks callbacks, String[] strArray) {

        ValueChecker.checkNullPointer(context);
        mContext = context;
        ValueChecker.checkNullPointer(looper, "Looper must not be null");
        mLooper = looper;
        mClientEvent = new FlintClientEvents(context, looper, this);
        mFlintClientHandler = new FlintClientHandler(looper);

        ValueChecker.checkNullPointer(callbacks);
        registerConnectionCallbacks(callbacks);
    }

    /**
     * Constructor.
     * 
     * @param context
     * @param callbacks
     * @param failedListener
     * @param strArray
     */
    protected FlintClient(Context context,
            IFlintClient.ConnectionCallbacks callbacks, String[] strArray) {

        this(context, context.getMainLooper(), new ClientConnectionCallbacks(
                callbacks), strArray);
    }

    /**
     * Flint service's interface descriptor.
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

    protected abstract FlintConnectedClient createFlintConnectedClient(
            IFlintCallbackImpl flintCallback) throws RemoteException;

    /**
     * Connect with Flint service. It's the most important API exported to Flint
     * application.
     * 
     * By Calling this function, Flint application will try to connect with
     * Flint Service.
     * 
     */
    public void connect() {
        if (mFlintConnectedClient != null) {
            return;
        }
        mConnected = true;
        mConnectedState = CONNECTION_STATUS_CONNECTING;

        try {
            mFlintConnectedClient = createFlintConnectedClient(new IFlintCallbackImpl(
                    this));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Is connected?
     */
    public boolean isConnected() {
        return mConnectedState == CONNECTION_STATUS_CONNECTED;
    }

    /**
     * Is connecting?
     */
    public boolean isConnecting() {
        return mConnectedState == CONNECTION_STATUS_CONNECTING;
    }

    /**
     * Disconnect with Flint service.
     * 
     * Do some clean up work.
     */
    public void disconnect() {
        mConnected = false;
        synchronized (mCallbackProxyList) {
            int size = mCallbackProxyList.size();
            for (int i = 0; i < size; i++) {
                mCallbackProxyList.get(i).reset();
            }
            mCallbackProxyList.clear();
        }
        mFlintConnectedClient = null;
        mConnectedState = CONNECTION_STATUS_DISCONNECTED;
        mService = null;
        mFlintClientHandler.sendMessage(mFlintClientHandler.obtainMessage(
                MSG_WHAT_DISCONNECTED, Integer.valueOf(1)));
    }

    /**
     * Called when disconnected from Flint service.
     * 
     * @param reason
     */
    public void sendDisconnectedMessage(int reason) {
        mFlintClientHandler.sendMessage(mFlintClientHandler.obtainMessage(
                MSG_WHAT_DISCONNECTED, Integer.valueOf(reason)));
    }

    /**
     * Get context.
     * 
     * @return
     */
    public final Context getContext() {
        return mContext;
    }

    /**
     * Get looper
     */
    public final Looper getLooper() {
        return mLooper;
    }

    /**
     * After connect with Flint service, called to show current connect status
     * 
     * @param statusCode
     * @param binder
     * @param bundle
     */
    protected void onPostInitResult(int statusCode, IBinder binder,
            Bundle bundle) {
        mFlintClientHandler.sendMessage(mFlintClientHandler.obtainMessage(
                MSG_WHAT_CONNECTION_CALLBACK, new FlintClientCallbackProxy(
                        statusCode, binder, bundle)));
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
     * Get current connected Flint service.
     * 
     * @return current connected Flint service.
     */
    protected final T getService() {
        checkConnected();
        return mService;
    }

    /**
     * Add connection call back function. Useless.Not called by others?!!!!
     * 
     * @param callback
     */
    public final void addCallback(CallbackProxy<?> callback) {
        synchronized (mCallbackProxyList) {
            mCallbackProxyList.add(callback);
        }
        mFlintClientHandler.sendMessage(mFlintClientHandler.obtainMessage(
                MSG_WHAT_CONNECTION_CALLBACK, callback));
    }

    /**
     * Whether client can receive event.
     */
    public boolean canReceiveEvent() {
        return mConnected;
    }

    /**
     * register connection callback function.
     * 
     * @param callbacks
     *            connection callback
     *            function(onConnected/onConnectionSuspended)
     */
    public void registerConnectionCallbacks(
            FlintManager.ConnectionCallbacks callbacks) {
        mClientEvent.registerConnectionCallbacks(callbacks);
    }

    /**
     * register connection callback.
     */
    public void registerConnectionCallbacks(
            IFlintClient.ConnectionCallbacks callbacks) {
        mClientEvent.registerConnectionCallbacks(new ClientConnectionCallbacks(
                callbacks));
    }

    /**
     * is connection callback register.
     */
    public boolean isConnectionCallbacksRegistered(
            IFlintClient.ConnectionCallbacks callbacks) {
        return mClientEvent
                .isConnectionCallbacksRegistered(new ClientConnectionCallbacks(
                        callbacks));
    }

    /**
     * unregister callback function.
     */
    public void unregisterConnectionCallbacks(
            IFlintClient.ConnectionCallbacks callbacks) {
        mClientEvent
                .unregisterConnectionCallbacks(new ClientConnectionCallbacks(
                        callbacks));
    }

    /**
     * Flint callback implementation. which will be called when
     * connect/disconnect (ok/failed) with Flint service.
     * 
     * @author jim
     * 
     */
    public static final class IFlintCallbackImpl extends IFlintCallbacks.Stub {

        private FlintClient flintClient;

        /**
         * constructor.
         * 
         * @param client
         *            Flint client.
         */
        public IFlintCallbackImpl(FlintClient client) {
            flintClient = client;
        }

        /**
         * Called when client connected(ok/failed) with Flint service.
         */
        public void onPostInitComplete(int statusCode, IBinder binder,
                Bundle bundle) {

            ValueChecker
                    .checkNullPointer(flintClient,
                            "onPostInitComplete can be called only once per call to getServiceFromBroker");
            // return current connect status.
            flintClient.onPostInitResult(statusCode, binder, bundle);
            flintClient = null;
        }
    }

    public static final class ClientConnectionCallbacks implements
            FlintManager.ConnectionCallbacks {
        private final IFlintClient.ConnectionCallbacks callback;

        public ClientConnectionCallbacks(
                IFlintClient.ConnectionCallbacks callback) {
            this.callback = callback;
        }

        public void onConnected(Bundle connectionHint) {
            callback.onConnected(connectionHint);
        }

        public void onConnectionSuspended(int cause) {
            callback.onDisconnected();
        }

        public boolean equals(Object other) {
            if ((other instanceof ClientConnectionCallbacks)) {
                return callback
                        .equals(((ClientConnectionCallbacks) other).callback);
            }
            return callback.equals(other);
        }

        @Override
        public void onConnectionFailed(ConnectionResult result) {
            callback.onConnectionFailed(result);
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
                    Log.w("FlintClient", "Callback proxy " + this
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
                isUsed = true;
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

    public final class FlintClientCallbackProxy extends CallbackProxy<Boolean> {
        public final int statusCode;
        public final Bundle bundle;
        public final IBinder binder;

        public FlintClientCallbackProxy(int statusCode, IBinder binder,
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
            switch (statusCode) {
            case ConnectionResult.SUCCESS:
                try {
                    String desc = binder.getInterfaceDescriptor();
                    if (getInterfaceDescriptor().equals(desc)) {
                        mService = getService(binder);
                        if (mService != null) {
                            mConnectedState = CONNECTION_STATUS_CONNECTED;
                            mClientEvent.notifyOnConnected();
                            return;
                        }
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                mFlintConnectedClient = null;
                mConnectedState = CONNECTION_STATUS_DISCONNECTED;
                mService = null;
                mClientEvent.notifyOnConnectionFailed(new ConnectionResult(ConnectionResult.INTERNAL_ERROR,
                        null));
                break;
            case ConnectionResult.DEVELOPER_ERROR:
                mConnectedState = CONNECTION_STATUS_DISCONNECTED;
                throw new IllegalStateException(
                        "A fatal developer error has occurred. Check the logs for further information.");
            default:
                PendingIntent pendingIntent = null;
                if (bundle != null) {
                    pendingIntent = (PendingIntent) bundle
                            .getParcelable("pendingIntent");
                }
                mFlintConnectedClient = null;
                mConnectedState = CONNECTION_STATUS_DISCONNECTED;
                mService = null;
                mClientEvent.notifyOnConnectionFailed(new ConnectionResult(
                        statusCode, pendingIntent));
            }

        }
    }

    /**
     * This handler will process all client connected/disconnected related
     * messages from Flint service.
     * 
     * Used when: 1. connect/disconnect success result with Flint service 2.
     * connect/disconnect failed result.
     */
    final class FlintClientHandler extends Handler {
        public FlintClientHandler(Looper looper) {
            super();
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_WHAT_CONNECTION_CALLBACK:
                if (!isConnecting()) {
                    CallbackProxy cb = (CallbackProxy) msg.obj;
                    cb.onFailed();
                    cb.unregister();
                } else {
                    CallbackProxy cb = (CallbackProxy) msg.obj;
                    cb.call(); // when connected with Flint service(device),
                               // come to here.
                }
                break;
            case MSG_WHAT_CONNECTION_FAILED:
                mClientEvent.notifyOnConnectionFailed(new ConnectionResult(
                        ((Integer) msg.obj).intValue(), null));
                break;
            case MSG_WHAT_DISCONNECTED:
                mConnectedState = CONNECTION_STATUS_DISCONNECTED;
                mService = null;

                mClientEvent.notifyOnConnectionSuspended(((Integer) msg.obj)
                        .intValue());
                break;
            default:
                Log.wtf("FlintClient", "Don't know how to handle this message.");
                break;
            }
        }
    }
}
