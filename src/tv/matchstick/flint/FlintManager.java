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

package tv.matchstick.flint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import tv.matchstick.client.common.api.FlintManagerImpl;
import tv.matchstick.client.internal.ValueChecker;
import tv.matchstick.flint.internal.Api;
import tv.matchstick.flint.internal.MatchStickApi.MatchStickApiImpl;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

/**
 * Flint Manager interface.
 * 
 * Before any operation is executed, the FlintManager must be connected using
 * the connect() method. The device is not considered connected until the
 * onConnected(Bundle) callback has been called. When your app is done using
 * this connection, call disconnect(), even if the async result from connect()
 * has not yet been delivered. You should instantiate a manager object in your
 * Activity's onCreate(Bundle) method and then call connect() in onStart() and
 * disconnect() in onStop(), regardless of the state.
 */
public interface FlintManager {
    /**
     * Execute task.
     * 
     * @param flintApi
     * @return
     */
    public <A extends Api.ConnectionApi, T extends MatchStickApiImpl<? extends Result, A>> T executeTask(
            T flintApi);

    /**
     * Get connection api.
     * 
     * @param builder
     * @return
     */
    public <C extends Api.ConnectionApi> C getConnectionApi(
            Api.ConnectionBuilder<C> builder);

    /**
     * Connect to Flint service.
     */
    public void connect();

    /**
     * Disconnect from Flint service.
     */
    public void disconnect();

    /**
     * Reconnect to Flint service.
     * 
     * Closes the current connection to Flint service and creates a new
     * connection.
     */
    public void reconnect();

    /**
     * Whether it's connected with Flint service
     * 
     * @return true for connected, false for others
     */
    public boolean isConnected();

    /**
     * Whether it's connecting to Flint service.
     * 
     * @return true for connecting, false for others
     */
    public boolean isConnecting();

    /**
     * Block connect to Flint service.
     * 
     * @param timeout
     *            the maximum time to wait
     * @param paramTimeUnit
     *            the time unit of the timeout argument
     * @return one ConnectionResult object
     */
    public ConnectionResult blockingConnect(long timeout, TimeUnit paramTimeUnit);

    /**
     * Register Connection callback.
     * 
     * Register a listener to receive connection events from this FlintManager.
     * 
     * @param callbacks
     */
    public void registerConnectionCallbacks(ConnectionCallbacks callbacks);

    /**
     * Check whether the specific callback is already registered.
     * 
     * If the specified listener is currently registered to receive connection
     * events, return true
     * 
     * @param callbacks
     * @return
     */
    public boolean isConnectionCallbacksRegistered(ConnectionCallbacks callbacks);

    /**
     * Unregister Connection Callback.
     * 
     * Removes a connection listener from this FlintManager.
     * 
     * @param callbacks
     */
    public void unregisterConnectionCallbacks(ConnectionCallbacks callbacks);

    /**
     * Helper class for FlintApi class
     */
    public final class Builder {

        /**
         * Used context
         */
        private final Context mContext;

        /**
         * Api option maps
         */
        private final Map<Api, ApiOptions> mApiOptionMap;

        /**
         * ConnectionCallbacks set
         */
        private final Set<ConnectionCallbacks> mCallbacksSet;

        /**
         * Looper
         */
        private Looper mLooper;

        /**
         * Builder Constructor
         * 
         * @param context
         *            used context
         */
        public Builder(Context context) {
            this.mApiOptionMap = new HashMap<Api, ApiOptions>();
            this.mCallbacksSet = new HashSet<ConnectionCallbacks>();
            this.mContext = context;
            this.mLooper = context.getMainLooper();
        }

        /**
         * Build Constructor.
         * 
         * @param context
         *            The context to use for the connection
         * @param connectedListener
         *            The listener where the results of the asynchronous
         *            connect() call are delivered.
         */
        public Builder(Context context,
                FlintManager.ConnectionCallbacks connectedListener) {
            this(context);

            ValueChecker.checkNullPointer(connectedListener,
                    "Must provide a connected listener");
            mCallbacksSet.add(connectedListener);
        }

        /**
         * Set handler.
         * 
         * Sets a Handler to indicate which thread to use when invoking
         * callbacks.
         * 
         * @param handler
         * @return
         */
        public Builder setHandler(Handler handler) {
            ValueChecker.checkNullPointer(handler, "Handler must not be null");
            mLooper = handler.getLooper();
            return this;
        }

        /**
         * Add connection callback function.
         * 
         * Registers a listener to receive connection events from FlintManager.
         * 
         * @param callback
         * @return
         */
        public Builder addConnectionCallbacks(
                FlintManager.ConnectionCallbacks callback) {
            mCallbacksSet.add(callback);
            return this;
        }

        /**
         * Add Api.
         * 
         * @param api
         * @return
         */
        public Builder addApi(Api api) {
            return addApi(api, null);
        }

        /**
         * Add Api.
         * 
         * @param api
         * @param options
         * @return
         */
        public Builder addApi(Api api, FlintManager.ApiOptions options) {
            mApiOptionMap.put(api, options);
            return this;
        }

        /**
         * Create FlintManager object according to current data
         * 
         * @return
         */
        public FlintManager build() {
            return new FlintManagerImpl(mContext, mLooper, mApiOptionMap,
                    mCallbacksSet);
        }
    }

    /**
     * Flint Api options
     */
    public interface ApiOptions {
    }

    /**
     * Connection callback.
     * 
     * Provides callbacks that are called when the client is connected or
     * disconnected from the service.
     * <p>
     * Most applications implement onConnected(Bundle) to start making requests.
     */
    public interface ConnectionCallbacks {

        /**
         * Connection cancel
         * 
         */
        public static final int CAUSE_CONNECTION_CANCEL = -1;

        /**
         * Service disconnected.
         * 
         * A suspension cause informing that the service has been killed.
         */
        public static final int CAUSE_SERVICE_DISCONNECTED = 1;

        /**
         * Network lost.
         * 
         * A suspension cause informing you that a peer device connection was
         * lost.
         */
        public static final int CAUSE_NETWORK_LOST = 2;

        /**
         * Connection failed
         * 
         */
        public static final int CAUSE_CONNECTION_FAILED = 3;

        /**
         * Called when connected.
         * 
         * After calling connect(), this method will be invoked asynchronously
         * when the connect request has successfully completed.
         * 
         * @param connectionHint
         */
        public void onConnected(Bundle connectionHint);

        /**
         * Called when suspended.
         * 
         * Called when the client is temporarily in a disconnected state.
         * 
         * @param cause
         */
        public void onConnectionSuspended(int cause);

        /**
         * Called when there was an error connecting the client to the service.
         * 
         * @param result
         */
        public void onConnectionFailed(ConnectionResult result);
    }

}
