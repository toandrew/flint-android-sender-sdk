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

package tv.matchstick.client.common.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import tv.matchstick.client.common.Releasable;
import tv.matchstick.client.internal.FlintClientEvents;
import tv.matchstick.client.internal.ValueChecker;
import tv.matchstick.flint.ConnectionResult;
import tv.matchstick.flint.FlintManager;
import tv.matchstick.flint.Result;
import tv.matchstick.flint.internal.Api;
import tv.matchstick.flint.internal.MatchStickApi.MatchStickApiImpl;
import android.content.Context;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Implementation of FlintManager.
 * 
 * It's a concrete implementation of FlintManager interface.
 */
public final class FlintManagerImpl implements FlintManager {
    private static final int MSG_WHAT_DO_CONNECT = 1;

    private static final int CONNECT_STATE_CONNECTING = 1;
    private static final int CONNECT_STATE_CONNECTED = 2;
    private static final int CONNECT_STATE_DISCONNECTING = 3;
    private static final int CONNECT_STATE_DISCONNECTED = 4;

    private final FlintClientEvents mFlintClientEvents;

    private final Queue<FlintApiClientTask<?>> mPendingTaskQueue = new LinkedList<FlintApiClientTask<?>>();

    private final Handler mHander;

    private final Set<FlintApiClientTask<?>> mReleaseableTasks = new HashSet<FlintApiClientTask<?>>();

    private final Lock mLock = new ReentrantLock();

    private final Condition mCondition = mLock.newCondition();

    private final Bundle mBundle = new Bundle();

    private final Map<Api.ConnectionBuilder<?>, Api.ConnectionApi> mConnectionMap = new HashMap<Api.ConnectionBuilder<?>, Api.ConnectionApi>();

    private int mCurrentPrority;

    private int mConnectState = CONNECT_STATE_DISCONNECTED;

    private int mRetryConnectCounter = 0;

    private int mConnectionMapSize;

    private boolean mPending = false;

    private boolean mCanReceiveEvent;

    private int mMessageDelay = 5000;

    private ConnectionResult mConnectionFailedResult;

    /**
     * Called when release resource.
     */
    private final ReleaseCallback mReleaseCallback = new ReleaseCallback() {
        public void onRelease(FlintApiClientTask task) {
            mLock.lock();
            try {
                mReleaseableTasks.remove(task);
            } finally {
                mLock.unlock();
            }
        }
    };

    private final FlintClientEvents.ClientEventCallback mClientEventCallback = new FlintClientEvents.ClientEventCallback() {
        public boolean canReceiveEvent() {
            return mCanReceiveEvent;
        }

        public boolean isConnected() {
            return FlintManagerImpl.this.isConnected();
        }

        public Bundle getBundle() {
            return null;
        }
    };

    /**
     * Implementation of FlintManager
     * 
     * @param context
     * @param looper
     * @param apiOptionsMap
     * @param connCallbacksSet
     */
    public FlintManagerImpl(Context context, Looper looper,
            Map<Api, ApiOptions> apiOptionsMap,
            Set<ConnectionCallbacks> connCallbacksSet) {
        mFlintClientEvents = new FlintClientEvents(context, looper,
                mClientEventCallback);
        mHander = new FlintApiClientHandler(looper);

        Iterator<ConnectionCallbacks> itCallbacks = connCallbacksSet.iterator();
        while (itCallbacks.hasNext()) {
            mFlintClientEvents.registerConnectionCallbacks(itCallbacks.next());
        }

        Iterator<Api> apis = apiOptionsMap.keySet().iterator();
        while (apis.hasNext()) {
            Api api = apis.next();
            final Api.ConnectionBuilder<?> builder = api.getConnectionBuilder();
            ApiOptions apiOption = apiOptionsMap.get(api);
            mConnectionMap.put(builder, builder.build(context, looper,
                    apiOption, new ConnectionCallbacks() {
                        /**
                         * Connected event
                         */
                        public void onConnected(Bundle connectionHint) {
                            mLock.lock();
                            try {
                                if (mConnectState == CONNECT_STATE_CONNECTING) {
                                    if (connectionHint != null) {
                                        mBundle.putAll(connectionHint);
                                    }
                                    notifyConnectionResult();
                                }
                            } finally {
                                mLock.unlock();
                            }
                        }

                        /**
                         * Connection suspended event
                         */
                        public void onConnectionSuspended(int cause) {
                            mLock.lock();
                            try {
                                onDisconnected(cause);
                                switch (cause) {
                                case CAUSE_NETWORK_LOST:
                                    connect();
                                    break;
                                case CAUSE_SERVICE_DISCONNECTED:
                                    if (!canRetryConnect()) {
                                        mRetryConnectCounter = 2;
                                        mHander.sendMessageDelayed(
                                                mHander.obtainMessage(MSG_WHAT_DO_CONNECT),
                                                mMessageDelay);
                                    }
                                }
                            } finally {
                                mLock.unlock();
                            }
                        }

                        @Override
                        public void onConnectionFailed(ConnectionResult result) {
                            mLock.lock();
                            try {
                                if ((mConnectionFailedResult == null)
                                        || (builder.getPriority() < mCurrentPrority)) {
                                    mConnectionFailedResult = result;
                                    mCurrentPrority = builder.getPriority();
                                }
                                notifyConnectionResult();
                            } finally {
                                mLock.unlock();
                            }
                        }
                    }));
        }
    }

    /*************************************************/
    // private methods
    /*************************************************/
    private void notifyConnectionResult() {
        mLock.lock();
        try {
            mConnectionMapSize--;
            if (mConnectionMapSize == 0) {
                if (mConnectionFailedResult != null) {
                    mPending = false;
                    onDisconnected(ConnectionCallbacks.CAUSE_CONNECTION_FAILED);
                    if (canRetryConnect()) {
                        mRetryConnectCounter -= 1;
                    }
                    if (canRetryConnect()) {
                        mHander.sendMessageDelayed(
                                mHander.obtainMessage(MSG_WHAT_DO_CONNECT),
                                mMessageDelay);
                    } else {
                        mFlintClientEvents
                                .notifyOnConnectionFailed(mConnectionFailedResult);
                    }
                    mCanReceiveEvent = false;
                } else {
                    mConnectState = CONNECT_STATE_CONNECTED;
                    cancelReconnect();
                    mCondition.signalAll();
                    flushQueue();
                    if (mPending) {
                        mPending = false;
                        onDisconnected(ConnectionCallbacks.CAUSE_CONNECTION_CANCEL);
                    } else {
                        mFlintClientEvents
                                .notifyOnConnected((mBundle.isEmpty()) ? null
                                        : mBundle);
                    }
                }
            }
        } finally {
            mLock.unlock();
        }
    }

    private <A extends Api.ConnectionApi> void execute(
            FlintApiClientTask<A> task) throws DeadObjectException {
        mLock.lock();
        try {
            ValueChecker.checkTrueWithErrorMsg(isConnected(),
                    "FlintManager is not connected yet.");
            ValueChecker
                    .checkTrueWithErrorMsg(
                            task.getConnectionBuiler() != null,
                            "This task can not be executed or enqueued (it's probably a Batch or malformed)");
            if (task instanceof Releasable) {
                mReleaseableTasks.add(task);
                task.setReleaseCallback(mReleaseCallback);
            }

            A connection = getConnectionApi(task.getConnectionBuiler());
            task.exec(connection);
        } finally {
            mLock.unlock();
        }
    }

    /**
     * Flush pending task queue.
     */
    private void flushQueue() {
        ValueChecker.checkTrueWithErrorMsg(isConnected(),
                "FlintManager is not connected yet.");
        mLock.lock();
        try {
            while (!(mPendingTaskQueue.isEmpty())) {
                try {
                    execute((FlintApiClientTask) mPendingTaskQueue.remove());
                } catch (DeadObjectException e) {
                    Log.w("FlintManagerImpl",
                            "Service died while flushing queue", e);
                }
            }
        } finally {
            mLock.unlock();
        }
    }

    /**
     * Disconnected event
     * 
     */
    private void onDisconnected(int cause) {
        mLock.lock();
        try {
            if (mConnectState != CONNECT_STATE_DISCONNECTING) {
                if (cause == ConnectionCallbacks.CAUSE_CONNECTION_CANCEL) {
                    if (isConnecting()) {
                        Iterator tasks = mPendingTaskQueue.iterator();
                        while (tasks.hasNext()) {
                            FlintApiClientTask task = (FlintApiClientTask) tasks
                                    .next();
                            tasks.remove();
                        }
                    } else {
                        mPendingTaskQueue.clear();
                    }
                    if ((mConnectionFailedResult == null)
                            && (!(mPendingTaskQueue.isEmpty()))) {
                        mPending = true;
                        return;
                    }
                }
                boolean isConnecting = isConnecting();
                boolean isConnected = isConnected();
                mConnectState = CONNECT_STATE_DISCONNECTING;
                if (isConnecting) {
                    if (cause == ConnectionCallbacks.CAUSE_CONNECTION_CANCEL) { // canceled
                        mConnectionFailedResult = null;
                    }
                    mCondition.signalAll();
                }
                Iterator tasks = mReleaseableTasks.iterator();
                while (tasks.hasNext()) {
                    FlintApiClientTask task = (FlintApiClientTask) tasks.next();
                    task.release();
                }
                mReleaseableTasks.clear();
                mCanReceiveEvent = false;
                Iterator connections = mConnectionMap.values().iterator();
                while (connections.hasNext()) {
                    Api.ConnectionApi api = (Api.ConnectionApi) connections
                            .next();
                    if (api.isConnected()) {
                        api.disconnect();
                    }
                }
                mCanReceiveEvent = true;
                mConnectState = CONNECT_STATE_DISCONNECTED;
                if (isConnected) {
                    if (cause != ConnectionCallbacks.CAUSE_CONNECTION_CANCEL) {
                        mFlintClientEvents.notifyOnConnectionSuspended(cause);
                    }
                    mCanReceiveEvent = false;
                }
            }
        } finally {
            this.mLock.unlock();
        }
    }

    /**
     * Whether we can retry connect.
     * 
     * @return
     */
    private boolean canRetryConnect() {
        mLock.lock();
        try {
            return (mRetryConnectCounter != 0) ? true : false;
        } finally {
            mLock.unlock();
        }
    }

    /**
     * Cancel reconnect action.
     */
    private void cancelReconnect() {
        mLock.lock();
        try {
            mRetryConnectCounter = 0;
            mHander.removeMessages(MSG_WHAT_DO_CONNECT);
        } finally {
            mLock.unlock();
        }
    }

    /**
     * Do Flint actions with Flint service.
     */
    public <A extends Api.ConnectionApi, T extends MatchStickApiImpl<? extends Result, A>> T executeTask(
            T task) {
        ValueChecker.checkTrueWithErrorMsg(isConnected(),
                "FlintManager is not connected yet.");
        flushQueue();
        try {
            execute(task);
        } catch (DeadObjectException e) {
            e.printStackTrace();
        }
        return task;
    }

    /**
     * Get connection api in connection map by builder
     */
    public <C extends Api.ConnectionApi> C getConnectionApi(
            Api.ConnectionBuilder<C> builder) {
        C connection = (C) mConnectionMap.get(builder);
        ValueChecker.checkNullPointer(connection,
                "Appropriate Api was not requested.");
        return connection;
    }

    /**
     * Ready to connect to Flint device.
     */
    public void connect() {
        mLock.lock();
        try {
            mPending = false;
            if ((isConnected()) || (isConnecting())) {
                return;
            }
            mCanReceiveEvent = true;
            mConnectionFailedResult = null;
            mConnectState = CONNECT_STATE_CONNECTING;
            mBundle.clear();
            mConnectionMapSize = mConnectionMap.size();
            Iterator<Api.ConnectionApi> connections = mConnectionMap.values()
                    .iterator();
            while (connections.hasNext()) {
                Api.ConnectionApi api = connections.next();
                api.connect();
            }
        } finally {
            mLock.unlock();
        }
    }

    /**
     * Blocking connect.
     */
    public ConnectionResult blockingConnect(long timeout, TimeUnit unit) {
        ValueChecker.checkTrueWithErrorMsg(
                Looper.myLooper() != Looper.getMainLooper(),
                "blockingConnect must not be called on the UI thread");
        mLock.lock();
        try {
            connect();
            long nanos = unit.toNanos(timeout);
            while (isConnecting()) {
                try {
                    nanos = mCondition.awaitNanos(nanos);
                    if (nanos <= 0L) {
                        ConnectionResult timeoutResult = new ConnectionResult(
                                ConnectionResult.TIMEOUT, null);
                        mLock.unlock();
                        return timeoutResult;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    ConnectionResult interruptResult = new ConnectionResult(
                            ConnectionResult.INTERRUPTED, null);
                    mLock.unlock();
                    return interruptResult;
                }
            }
            if (isConnected()) {
                return ConnectionResult.connectResult;
            }
            if (this.mConnectionFailedResult != null) {
                return mConnectionFailedResult;
            }
            ConnectionResult conceledResult = new ConnectionResult(
                    ConnectionResult.CANCELED, null);
            return conceledResult;
        } finally {
            mLock.unlock();
        }
    }

    /**
     * Disconnect
     */
    public void disconnect() {
        /**
         * cancel reconnect action
         */
        cancelReconnect();

        /**
         * call disconnect callback
         */
        onDisconnected(ConnectionCallbacks.CAUSE_CONNECTION_CANCEL);
    }

    /**
     * Do reconnect
     */
    public void reconnect() {
        /**
         * disconnect first
         */
        disconnect();

        /**
         * connect
         */
        connect();
    }

    /**
     * Connected?!
     */
    public boolean isConnected() {
        mLock.lock();
        try {
            return (mConnectState == CONNECT_STATE_CONNECTED) ? true : false;
        } finally {
            mLock.unlock();
        }
    }

    /**
     * Is connecting?
     */
    public boolean isConnecting() {
        mLock.lock();
        try {
            return (mConnectState == CONNECT_STATE_CONNECTING) ? true : false;
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void registerConnectionCallbacks(ConnectionCallbacks listener) {
        mFlintClientEvents.registerConnectionCallbacks(listener);
    }

    @Override
    public boolean isConnectionCallbacksRegistered(ConnectionCallbacks listener) {
        return mFlintClientEvents.isConnectionCallbacksRegistered(listener);
    }

    @Override
    public void unregisterConnectionCallbacks(ConnectionCallbacks listener) {
        mFlintClientEvents.unregisterConnectionCallbacks(listener);
    }

    /**
     * Release callback interface
     */
    public interface ReleaseCallback {
        public void onRelease(FlintApiClientTask task);
    }

    /**
     * Handler
     */
    class FlintApiClientHandler extends Handler {
        FlintApiClientHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == MSG_WHAT_DO_CONNECT) {
                mLock.lock();
                try {
                    if (!isConnected() && !isConnecting()) {
                        connect();
                    }
                } finally {
                    mLock.unlock();
                }
            }
            Log.wtf("FlintManagerImpl",
                    "Don't know how to handle this message.");
        }
    }

    /**
     * Flint tasks
     * 
     * @param <A>
     */
    public interface FlintApiClientTask<A extends Api.ConnectionApi> {
        public Api.ConnectionBuilder<A> getConnectionBuiler();

        public void exec(A connection) throws DeadObjectException;

        public void setReleaseCallback(ReleaseCallback callback);

        public void release();

    }
}
