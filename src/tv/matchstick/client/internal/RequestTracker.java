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

import org.json.JSONObject;

import android.os.SystemClock;

public final class RequestTracker {
    private long mTimeout;
    private long mRequestId;
    private long mCurrentTime;
    private RequestTrackerCallback mCallback;
    private static final LOG log = new LOG("RequestTracker");
    public static final Object mLock = new Object();

    public RequestTracker(long timeOut) {
        this.mTimeout = timeOut;
        this.mRequestId = -1L;
        this.mCurrentTime = 0L;
    }

    public void startTrack(long requestId, RequestTrackerCallback cb) {
        RequestTrackerCallback callback = null;
        long reqId = -1L;
        synchronized (mLock) {
            callback = mCallback;
            reqId = mRequestId;
            mRequestId = requestId;
            mCallback = cb;
            mCurrentTime = SystemClock.elapsedRealtime();
        }
        if (callback == null) {
            return;
        }
        callback.onSignInRequired(reqId);
    }

    public void clear() {
        synchronized (mLock) {
            if (this.mRequestId != -1L)
                doClear();
        }
    }

    private void doClear() {
        mRequestId = -1L;
        mCallback = null;
        mCurrentTime = 0L;
    }

    // isRunning
    public boolean isRequestIdAvailable() {
        synchronized (mLock) {
            return (mRequestId != -1L);
        }
    }

    public boolean isCurrentRequestId(long requestId) {
        synchronized (mLock) {
            return ((mRequestId != -1L) && (mRequestId == requestId));
        }
    }

    public boolean trackRequest(long reqeustId, int statusCode) {
        return trackRequest(reqeustId, statusCode, null);
    }

    public boolean trackRequest(long requestId, int statusCode,
            JSONObject customData) {
        boolean flag = false;
        RequestTrackerCallback callback = null;
        synchronized (mLock) {
            if ((mRequestId != -1L) && (mRequestId == requestId)) {
                log.d("request %d completed", mRequestId);
                callback = mCallback;
                doClear();
                flag = true;
            }
        }
        if (callback != null) {
            callback.onTrackRequest(requestId, statusCode, customData);
        }
        return flag;
    }

    /*
     * checkTimeout
     */
    public boolean trackRequestTimeout(long timeOut, int statusCode) {
        boolean isTimeout = false;
        RequestTrackerCallback callback = null;
        long requestId = 0L;
        synchronized (mLock) {
            if ((mRequestId != -1L) && (timeOut - mCurrentTime >= mTimeout)) {
                log.d("request %d timed out", mRequestId);
                requestId = mRequestId;
                callback = mCallback;
                doClear();
                isTimeout = true;
            }
        }
        if (callback != null) {
            callback.onTrackRequest(requestId, statusCode, null);
        }
        return isTimeout;
    }

}
