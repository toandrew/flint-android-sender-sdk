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
import tv.matchstick.fling.FlingManager.ConnectionCallbacks;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * FlingClientEvents
 */
public class FlingClientEvents {
    final ArrayList<ConnectionCallbacks> mUnConnectionCallbacks = new ArrayList<ConnectionCallbacks>();
    private final ClientEventCallback mFlingClientEventCallback;
    private final Handler mHandler;

    private ArrayList<ConnectionCallbacks> mConnectionCallbacks = new ArrayList<ConnectionCallbacks>();

    private boolean mIsNotifingCallbacks = false;

    public FlingClientEvents(Context paramContext, Looper paramLooper,
            ClientEventCallback callback) {
        mFlingClientEventCallback = callback;
        mHandler = new ClientEventsHandler(paramLooper);
    }

    protected void notifyOnConnected() {
        synchronized (mConnectionCallbacks) {
            notifyOnConnected(mFlingClientEventCallback.getBundle());
        }
    }

    public void notifyOnConnected(Bundle bundle) {
        synchronized (mConnectionCallbacks) {
            ValueChecker.checkTrue(!mIsNotifingCallbacks);
            mHandler.removeMessages(1);
            mIsNotifingCallbacks = true;
            ValueChecker.checkTrue(mUnConnectionCallbacks.size() == 0);
            ArrayList<ConnectionCallbacks> localArrayList = mConnectionCallbacks;
            int i = 0;
            int j = localArrayList.size();
            while ((i < j) && (mFlingClientEventCallback.canReceiveEvent())) {
                if (!mFlingClientEventCallback.isConnected()) {
                    break;
                }
                if (!mUnConnectionCallbacks.contains(localArrayList.get(i))) {
                    localArrayList.get(i).onConnected(bundle);
                }
                ++i;
            }
            mUnConnectionCallbacks.clear();
            mIsNotifingCallbacks = false;
        }
    }

    public void notifyOnConnectionSuspended(int paramInt) {
        mHandler.removeMessages(1);
        synchronized (mConnectionCallbacks) {
            mIsNotifingCallbacks = true;
            ArrayList<ConnectionCallbacks> list = mConnectionCallbacks;
            int i = 0;
            int size = list.size();
            while (i < size) {
                if (!mFlingClientEventCallback.canReceiveEvent()) {
                    break;
                }
                if (mConnectionCallbacks.contains(list.get(i))) {
                    list.get(i).onConnectionSuspended(paramInt);
                }
                ++i;
            }
            mIsNotifingCallbacks = false;
        }
    }

    public void notifyOnConnectionFailed(ConnectionResult result) {
        mHandler.removeMessages(NOTIFY_CALLBACK);
        synchronized (mConnectionCallbacks) {
            ArrayList<ConnectionCallbacks> failedList = mConnectionCallbacks;
            for (ConnectionCallbacks failedCallback : failedList) {
                if (!mFlingClientEventCallback.canReceiveEvent()) {
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
                Log.w("FlingClientEvents",
                        "registerConnectionCallbacks(): listener " + listener
                                + " is already registered");
            } else {
                if (mIsNotifingCallbacks) {
                    mConnectionCallbacks = new ArrayList<ConnectionCallbacks>(
                            mConnectionCallbacks);
                }
                mConnectionCallbacks.add(listener);
            }
        }
        if (!mFlingClientEventCallback.isConnected()) {
            return;
        }
        mHandler.sendMessage(mHandler.obtainMessage(1, listener));
    }

    public boolean isConnectionCallbacksRegistered(
            FlingManager.ConnectionCallbacks listener) {
        ValueChecker.checkNullPointer(listener);
        synchronized (mConnectionCallbacks) {
            return mConnectionCallbacks.contains(listener);
        }
    }

    public void unregisterConnectionCallbacks(
            FlingManager.ConnectionCallbacks listener) {
        ValueChecker.checkNullPointer(listener);
        synchronized (mConnectionCallbacks) {
            if (mConnectionCallbacks != null) {
                if (mIsNotifingCallbacks)
                    mConnectionCallbacks = new ArrayList<ConnectionCallbacks>(
                            mConnectionCallbacks);
                boolean result = mConnectionCallbacks.remove(listener);
                if (!result) {
                    Log.w("FlingClientEvents",
                            "unregisterConnectionCallbacks(): listener "
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

    static final int NOTIFY_CALLBACK = 1;

    final class ClientEventsHandler extends Handler {
        public ClientEventsHandler(Looper paramLooper) {
            super(paramLooper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == NOTIFY_CALLBACK) {
                synchronized (mConnectionCallbacks) {
                    if (mFlingClientEventCallback.canReceiveEvent()
                            && mFlingClientEventCallback.isConnected()
                            && mConnectionCallbacks.contains(msg.obj)) {
                        Bundle bundle = mFlingClientEventCallback.getBundle();
                        FlingManager.ConnectionCallbacks cb = (FlingManager.ConnectionCallbacks) msg.obj;
                        cb.onConnected(bundle);
                    }
                }
                return;
            }
            Log.wtf("FlingClientEvents",
                    "Don't know how to handle this message.");
        }
    }
}
