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

package tv.matchstick.flint.internal;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import tv.matchstick.client.common.Releasable;
import tv.matchstick.client.common.api.FlintManagerImpl;
import tv.matchstick.client.common.api.FlintManagerImpl.FlintApiClientTask;
import tv.matchstick.client.internal.LOG;
import tv.matchstick.client.internal.ValueChecker;
import tv.matchstick.flint.ConnectionResult;
import tv.matchstick.flint.PendingResult;
import tv.matchstick.flint.Result;
import tv.matchstick.flint.ResultCallback;
import tv.matchstick.flint.Status;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.util.Pair;

/**
 * Flint API class
 */
public class MatchStickApi {
    private static final LOG log = new LOG("MatchStickApi");
    /**
     * Flint api implementation
     *
     * @param <R>
     *            result
     * @param <A>
     *            connection api
     */
    public static abstract class MatchStickApiImpl<R extends Result, A extends Api.ConnectionApi>
            implements PendingResult<R>, ResultCallback<R>,
            FlintApiClientTask<A> {

        /**
         * Connection Builder
         */
        private final Api.ConnectionBuilder<A> mConnectionBuilder;

        /**
         * Object lock
         */
        private final Object mLock;

        /**
         * Counter
         */
        private final CountDownLatch mCounter;

        /**
         * Api handler
         */
        private FlintApiHandler<R> mHandler;

        /**
         * Result callback
         */
        private ResultCallback<R> mResultCallback;

        /**
         * Current result
         */
        private volatile R mCurrentResult;

        /**
         * Whether the result is consumed
         */
        private volatile boolean mIsResultConsumed;

        /**
         * Release flag
         */
        private boolean mReleaseFlag;

        /**
         * Whether it's interrupted
         */
        private boolean mIsInterrupted;

        /**
         * Release callback
         */
        private FlintManagerImpl.ReleaseCallback mReleaseCallback;

        /**
         * Internal used constructor
         */
        protected MatchStickApiImpl() {
            this(null);
        }

        /**
         * Create instance with the connection builder
         *
         * @param builder
         *            connection builder
         */
        protected MatchStickApiImpl(Api.ConnectionBuilder<A> builder) {
            this.mLock = new Object();
            this.mCounter = new CountDownLatch(1);
            this.mConnectionBuilder = builder;
        }

        /**
         * Get related connection builder
         */
        public final Api.ConnectionBuilder<A> getConnectionBuiler() {
            return this.mConnectionBuilder;
        }

        /**
         * Release resource
         */
        public void release() {
            releaseInternal();
            this.mReleaseFlag = true;
        }

        /**
         * Execute connection api
         */
        public final void exec(A connectionApi) throws DeadObjectException {
            this.mHandler = new FlintApiHandler<R>(connectionApi.getLooper());
            try {
                execute(connectionApi);
            } catch (DeadObjectException e) {
                notifyInternalError(e);
                throw e;
            } catch (RemoteException r) {
                notifyInternalError(r);
            }
        }

        /**
         * Set callback function which will be called when release
         */
        public void setReleaseCallback(FlintManagerImpl.ReleaseCallback callback) {
            this.mReleaseCallback = callback;
        }

        /**
         * Called when result returned
         */
        public void onResult(R result) {
            postResult(result);
        }

        /**
         * Execute api
         *
         * @param connectionApi
         * @throws RemoteException
         */
        protected abstract void execute(A connectionApi) throws RemoteException;

        /**
         * Create result
         * 
         * @param status
         * @return
         */
        protected abstract R createResult(Status status);

        /**
         * Whether it's ready
         *
         * @return
         */
        public final boolean isReady() {
            return (this.mCounter.getCount() == 0L);
        }

        /**
         * Get current result
         *
         * @return current result
         */
        private R getResult() {
            synchronized (this.mLock) {
                ValueChecker.checkTrueWithErrorMsg(!(this.mIsResultConsumed),
                        "Result has already been consumed.");
                ValueChecker.checkTrueWithErrorMsg(isReady(),
                        "Result is not ready.");
                consumeResult();
                return this.mCurrentResult;
            }
        }

        /**
         * Block wait until be waked up by others
         */
        public final R await() {
            ValueChecker.checkTrueWithErrorMsg(!(this.mIsResultConsumed),
                    "Results has already been consumed");
            ValueChecker.checkTrueWithErrorMsg(
                    (isReady())
                            || (Looper.myLooper() != Looper.getMainLooper()),
                    "await must not be called on the UI thread");
            try {
                this.mCounter.await();
            } catch (InterruptedException e) {
                synchronized (this.mLock) {
                    postResult(createResult(Status.InterruptedStatus));
                    this.mIsInterrupted = true;
                }
            }
            ValueChecker.checkTrueWithErrorMsg(isReady(),
                    "Result is not ready.");
            return getResult();
        }

        /**
         * Wait until be waked up by others or timeout
         */
        public final R await(long time, TimeUnit units) {
            ValueChecker.checkTrueWithErrorMsg(!(this.mIsResultConsumed),
                    "Result has already been consumed.");
            ValueChecker.checkTrueWithErrorMsg(
                    (isReady())
                            || (Looper.myLooper() != Looper.getMainLooper()),
                    "await must not be called on the UI thread");
            try {
                boolean bool = this.mCounter.await(time, units);
                if (!(bool))
                    synchronized (this.mLock) {
                        postResult(createResult(Status.TimeOutStatus));
                        this.mIsInterrupted = true;
                    }
            } catch (InterruptedException e) {
                synchronized (this.mLock) {
                    postResult(createResult(Status.InterruptedStatus));
                    this.mIsInterrupted = true;
                }
            }
            ValueChecker.checkTrueWithErrorMsg(isReady(),
                    "Result is not ready.");
            return getResult();
        }

        /**
         * Set result callback function
         */
        public final void setResultCallback(ResultCallback<R> callback) {
            ValueChecker.checkTrueWithErrorMsg(!(this.mIsResultConsumed),
                    "Result has already been consumed.");
            synchronized (this.mLock) {
                if (isReady())
                    this.mHandler.notifyResultCallback(callback, getResult());
                else
                    this.mResultCallback = callback;
            }
        }

        /**
         * Set result callback function and call something later
         *
         * @param time
         * @param units
         */
        public final void setResultCallback(ResultCallback<R> callback,
                long time, TimeUnit units) {
            ValueChecker.checkTrueWithErrorMsg(!(this.mIsResultConsumed),
                    "Result has already been consumed.");
            synchronized (this.mLock) {
                if (isReady()) {
                    this.mHandler.notifyResultCallback(callback, getResult());
                } else {
                    this.mResultCallback = callback;
                    this.mHandler.setTimeoutTimer(this, units.toMillis(time));
                }
            }
        }

        /**
         * Post Result
         *
         * @param result
         */
        public final void postResult(R result) {
            synchronized (this.mLock) {
                if (this.mIsInterrupted) {
                    if (result instanceof Releasable) {
                        ((Releasable) result).release();
                    }
                    return;
                }
                ValueChecker.checkTrueWithErrorMsg(!(isReady()),
                        "Results have already been set");
                ValueChecker.checkTrueWithErrorMsg(!(this.mIsResultConsumed),
                        "Result has already been consumed");
                this.mCurrentResult = result;
                if (this.mReleaseFlag) {
                    releaseInternal();
                    return;
                }
                this.mCounter.countDown();
                if (this.mResultCallback != null) {
                    this.mHandler.removeTimeoutMessage();
                    this.mHandler.notifyResultCallback(this.mResultCallback,
                            getResult());
                }
            }
        }

        /**
         * Notify internal error
         *
         * @param exception
         */
        private void notifyInternalError(RemoteException exception) {
            Status status = new Status(ConnectionResult.INTERNAL_ERROR,
                    exception.getLocalizedMessage(), null);
            postResult(createResult(status));
        }

        /**
         * Release result
         */
        void consumeResult() {
            this.mIsResultConsumed = true;
            if (this.mReleaseCallback == null) {
                return;
            }
            this.mReleaseCallback.onRelease(this);
        }

        /**
         * Release resource
         */
        private void releaseInternal() {
            if ((this.mCurrentResult == null)
                    || (!(this instanceof Releasable))) {
                return;
            }
            try {
                ((Releasable) this).release();
            } catch (Exception e) {
                log.w(e, "Unable to release " + this);
            }
        }
    }

    /**
     * Flint Api Handler
     */
    public static class FlintApiHandler<R extends Result> extends Handler {
        public FlintApiHandler() {
            this(Looper.getMainLooper());
        }

        public FlintApiHandler(Looper looper) {
            super(looper);
        }

        /**
         * Notify result callback
         *
         * @param callback
         * @param result
         */
        public void notifyResultCallback(ResultCallback<R> callback, R result) {
            sendMessage(obtainMessage(1, new Pair(callback, result)));
        }

        /**
         * Set timeout message
         * 
         * @param obj
         * @param delayMillis
         *            delay time
         */
        public void setTimeoutTimer(MatchStickApiImpl<R, ?> obj,
                long delayMillis) {
            sendMessageDelayed(obtainMessage(2, obj), delayMillis);
        }

        /**
         * Remove timeout messages
         */
        public void removeTimeoutMessage() {
            removeMessages(2);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case 1: // post result
                Pair pair = (Pair) msg.obj;
                notifyCallback((ResultCallback<R>) pair.first, (R) pair.second);
                return;
            case 2: // time out
                MatchStickApiImpl flintApi = (MatchStickApiImpl) msg.obj;
                flintApi.postResult(flintApi.createResult(Status.TimeOutStatus));
                return;
            }
            log.wtf("Don't know how to handle this message.");
        }

        /**
         * Notify callback function
         * 
         * @param callback
         * @param result
         */
        protected void notifyCallback(ResultCallback<R> callback, R result) {
            callback.onResult(result);
        }
    }
}
