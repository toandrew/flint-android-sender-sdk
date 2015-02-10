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

import tv.matchstick.flint.ConnectionResult;
import tv.matchstick.flint.FlintManager;
import tv.matchstick.flint.FlintManager.ConnectionCallbacks;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * FlintClientEvents
 */
public class FlintClientEvents {
    private static final LOG log = new LOG("FlintClientEvents");
    private static final int NOTIFY_CALLBACK = 1;

    private final ArrayList<ConnectionCallbacks> mUnConnectionCallbacks = new ArrayList<ConnectionCallbacks>();
    private final ClientEventCallback mFlintClientEventCallback;
    private final Handler mHandler;

    private ArrayList<ConnectionCallbacks> mConnectionCallbacks = new ArrayList<ConnectionCallbacks>();

    private boolean mIsNotifingCallbacks = false;

    public FlintClientEvents(Context context, Looper looper,
            ClientEventCallback callback) {
        mFlintClientEventCallback = callback;
        mHandler = new ClientEventsHandler(looper);
    }

    protected void notifyOnConnected() {
        synchronized (mConnectionCallbacks) {
            notifyOnConnected(mFlintClientEventCallback.getBundle());
        }
    }

    public void notifyOnConnected(Bundle bundle) {
        synchronized (mConnectionCallbacks) {
            ValueChecker.checkTrue(!mIsNotifingCallbacks);
            mHandler.removeMessages(NOTIFY_CALLBACK);
            mIsNotifingCallbacks = true;
            ValueChecker.checkTrue(mUnConnectionCallbacks.size() == 0);
            ArrayList<ConnectionCallbacks> connectionList = mConnectionCallbacks;
            for (ConnectionCallbacks callback : connectionList) {
                if (!mFlintClientEventCallback.canReceiveEvent()) {
                    break;
                }
                if (mFlintClientEventCallback.isConnected()
                        && !mUnConnectionCallbacks.contains(callback)) {
                    callback.onConnected(bundle);
                }
            }

            mUnConnectionCallbacks.clear();
            mIsNotifingCallbacks = false;
        }
    }

    public void notifyOnConnectionSuspended(int cause) {
        mHandler.removeMessages(NOTIFY_CALLBACK);
        synchronized (mConnectionCallbacks) {
            mIsNotifingCallbacks = true;
            ArrayList<ConnectionCallbacks> connectionList = mConnectionCallbacks;
            for (ConnectionCallbacks callback : connectionList) {
                if (!mFlintClientEventCallback.canReceiveEvent()) {
                    break;
                }
                if (mConnectionCallbacks.contains(callback)) {
                    callback.onConnectionSuspended(cause);
                }
            }
            mIsNotifingCallbacks = false;
        }
    }

    public void notifyOnConnectionFailed(ConnectionResult result) {
        mHandler.removeMessages(NOTIFY_CALLBACK);
        synchronized (mConnectionCallbacks) {
            ArrayList<ConnectionCallbacks> failedList = mConnectionCallbacks;
            for (ConnectionCallbacks failedCallback : failedList) {
                if (!mFlintClientEventCallback.canReceiveEvent()) {
                    return;
                }
                failedCallback.onConnectionFailed(result);
            }
        }
    }

    public void registerConnectionCallbacks(ConnectionCallbacks listener) {
        ValueChecker.checkNullPointer(listener);
        synchronized (mConnectionCallbacks) {
            if (mConnectionCallbacks.contains(listener)) {
                log.w("registerConnectionCallbacks(): listener " + listener
                                + " is already registered");
            } else {
                if (mIsNotifingCallbacks) {
                    mConnectionCallbacks = new ArrayList<ConnectionCallbacks>(
                            mConnectionCallbacks);
                }
                mConnectionCallbacks.add(listener);
            }
        }
        if (mFlintClientEventCallback.isConnected()) {
            mHandler.sendMessage(mHandler.obtainMessage(NOTIFY_CALLBACK,
                    listener));
        }

    }

    public boolean isConnectionCallbacksRegistered(
            FlintManager.ConnectionCallbacks listener) {
        ValueChecker.checkNullPointer(listener);
        synchronized (mConnectionCallbacks) {
            return mConnectionCallbacks.contains(listener);
        }
    }

    public void unregisterConnectionCallbacks(
            FlintManager.ConnectionCallbacks listener) {
        ValueChecker.checkNullPointer(listener);
        synchronized (mConnectionCallbacks) {
            if (mConnectionCallbacks != null) {
                if (mIsNotifingCallbacks)
                    mConnectionCallbacks = new ArrayList<ConnectionCallbacks>(
                            mConnectionCallbacks);
                boolean result = mConnectionCallbacks.remove(listener);
                if (!result) {
                    log.w("unregisterConnectionCallbacks(): listener "
                                    + listener + " not found");
                } else if (mIsNotifingCallbacks
                        && !(mUnConnectionCallbacks.contains(listener))) {
                    mUnConnectionCallbacks.add(listener);
                }
            }
        }
    }

    public interface ClientEventCallback {
        public boolean canReceiveEvent();

        public boolean isConnected();

        public Bundle getBundle();
    }

    final class ClientEventsHandler extends Handler {
        public ClientEventsHandler(Looper paramLooper) {
            super(paramLooper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == NOTIFY_CALLBACK) {
                synchronized (mConnectionCallbacks) {
                    if (mFlintClientEventCallback.canReceiveEvent()
                            && mFlintClientEventCallback.isConnected()
                            && mConnectionCallbacks.contains(msg.obj)) {
                        Bundle bundle = mFlintClientEventCallback.getBundle();
                        FlintManager.ConnectionCallbacks cb = (FlintManager.ConnectionCallbacks) msg.obj;
                        cb.onConnected(bundle);
                    }
                }
            } else {
                log.wtf("Don't know how to handle this message.");
            }
        }
    }
}
