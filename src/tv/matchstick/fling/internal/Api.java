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

package tv.matchstick.fling.internal;

import tv.matchstick.client.internal.AccountInfo;
import tv.matchstick.fling.FlingManager;
import tv.matchstick.fling.FlingManager.ApiOptions;
import android.content.Context;
import android.os.Looper;

/**
 * Api class
 */
public final class Api {
    private final ConnectionBuilder<?> mConnectionBuilder;

    /**
     * Create api with connection builder
     * 
     * @param builder
     *            the specific builder
     */
    public Api(ConnectionBuilder<?> builder) {
        mConnectionBuilder = builder;
    }

    /**
     * Get connection builder
     * 
     * @return builder
     */
    public ConnectionBuilder<?> getConnectionBuilder() {
        return mConnectionBuilder;
    }

    /**
     * Connection interface for client API
     */
    public interface ConnectionApi {
        /**
         * connect to device
         */
        public void connect();

        /**
         * disconnect to device
         */
        public void disconnect();

        /**
         * Get current connected status with device
         * 
         * @return current connect status
         */
        public boolean isConnected();

        /**
         * Get related Looper object
         * 
         * @return looper
         */
        public Looper getLooper();
    }

    /**
     * Connection builder
     * 
     * @param <T>
     */
    public interface ConnectionBuilder<T extends ConnectionApi> {

        /**
         * Build the specific fling object
         * 
         * @param context
         *            application context
         * @param looper
         *            looper
         * @param account
         *            account information
         * @param options
         *            Api options
         * @param callbacks
         *            callback function
         * @param failedListener
         *            failed listener
         * @return the specific class instance
         */
        public T build(Context context, Looper looper, AccountInfo account,
                ApiOptions options, FlingManager.ConnectionCallbacks callbacks,
                FlingManager.OnConnectionFailedListener failedListener);

        /**
         * Get related priority
         * 
         * @return priority
         */
        public int getPriority();
    }

}
