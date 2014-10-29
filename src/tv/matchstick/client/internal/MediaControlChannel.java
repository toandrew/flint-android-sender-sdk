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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tv.matchstick.fling.MediaInfo;
import tv.matchstick.fling.MediaStatus;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

/**
 * Media control channel
 */
public class MediaControlChannel extends FlingChannel {
    private static final long REQUEST_MAX_TIME_OUT = TimeUnit.HOURS
            .toMillis(24L);

    private static final long MillisPerSecond = TimeUnit.SECONDS.toMillis(1L);

    private long mMediaStartTime;

    private MediaStatus mMediaStatus;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private final RequestTracker mRequestTrackerLoad = new RequestTracker(
            REQUEST_MAX_TIME_OUT);
    private final RequestTracker mRequestTrackerPause = new RequestTracker(
            REQUEST_MAX_TIME_OUT);
    private final RequestTracker mRequestTrackerPlay = new RequestTracker(
            REQUEST_MAX_TIME_OUT);
    private final RequestTracker mRequestTrackerStop = new RequestTracker(
            REQUEST_MAX_TIME_OUT);
    private final RequestTracker mRequestTrackerSeed = new RequestTracker(
            REQUEST_MAX_TIME_OUT);
    private final RequestTracker mRequestTrackerVolume = new RequestTracker(
            REQUEST_MAX_TIME_OUT);
    private final RequestTracker mRequestTrackerMute = new RequestTracker(
            REQUEST_MAX_TIME_OUT);
    private final RequestTracker mRequestTrackerRequestStatus = new RequestTracker(
            REQUEST_MAX_TIME_OUT);

    private final Runnable mTrackerTask = new RequestTrackerTask();

    private boolean mTrackTaskStarting;

    public MediaControlChannel() {
        super("urn:x-cast:com.google.cast.media", "MediaControlChannel");

        cleanInternal();
    }

    /**
     * Load media
     * 
     * @param callback
     * @param mediaInfo
     * @param autoplay
     * @param currentTime
     * @param customData
     * @return
     * @throws IOException
     */
    public long load(RequestTrackerCallback callback, MediaInfo mediaInfo,
            boolean autoplay, long currentTime, JSONObject customData)
            throws IOException {

        JSONObject json = new JSONObject();
        long requestId = getRequestId();
        mRequestTrackerLoad.startTrack(requestId, callback);
        handlerTrackerTask(true);

        try {
            json.put("requestId", requestId);
            json.put("type", "LOAD");
            json.put("media", mediaInfo.buildJson());
            json.put("autoplay", autoplay);
            json.put("currentTime",
                    DoubleAndLongConverter.long2double(currentTime));
            if (customData != null) {
                json.put("customData", customData);
            }
        } catch (JSONException e) {
        }

        sendTextMessage(json.toString(), requestId, null);

        return requestId;
    }

    /**
     * Pause media
     * 
     * @param callback
     * @param customData
     * @return
     * @throws IOException
     */
    public long pause(RequestTrackerCallback callback, JSONObject customData)
            throws IOException {

        JSONObject json = new JSONObject();
        long requestId = getRequestId();
        mRequestTrackerPause.startTrack(requestId, callback);
        handlerTrackerTask(true);

        try {
            json.put("requestId", requestId);
            json.put("type", "PAUSE");
            json.put("mediaSessionId", getMediaSessionId());
            if (customData != null) {
                json.put("customData", customData);
            }
        } catch (JSONException e) {
        }

        sendTextMessage(json.toString(), requestId, null);

        return requestId;
    }

    /**
     * Stop media
     * 
     * @param callback
     * @param customData
     * @return
     * @throws IOException
     */
    public long stop(RequestTrackerCallback callback, JSONObject customData)
            throws IOException {

        JSONObject json = new JSONObject();
        long requestId = getRequestId();
        mRequestTrackerStop.startTrack(requestId, callback);
        handlerTrackerTask(true);

        try {
            json.put("requestId", requestId);
            json.put("type", "STOP");
            json.put("mediaSessionId", getMediaSessionId());
            if (customData != null) {
                json.put("customData", customData);
            }
        } catch (JSONException e) {
        }

        sendTextMessage(json.toString(), requestId, null);

        return requestId;
    }

    /**
     * Play media
     * 
     * @param callback
     * @param customData
     * @return
     * @throws IOException
     * @throws IllegalStateException
     */
    public long play(RequestTrackerCallback callback, JSONObject customData)
            throws IOException, IllegalStateException {

        JSONObject json = new JSONObject();
        long requestId = getRequestId();
        mRequestTrackerPlay.startTrack(requestId, callback);
        handlerTrackerTask(true);

        try {
            json.put("requestId", requestId);
            json.put("type", "PLAY");
            json.put("mediaSessionId", getMediaSessionId());
            if (customData != null) {
                json.put("customData", customData);
            }
        } catch (JSONException e) {
        }

        sendTextMessage(json.toString(), requestId, null);

        return requestId;
    }

    /**
     * Seek media
     * 
     * @param callback
     * @param currentTime
     * @param resumeState
     * @param customData
     * @return
     * @throws IOException
     * @throws IllegalStateException
     */
    public long seek(RequestTrackerCallback callback, long currentTime,
            int resumeState, JSONObject customData) throws IOException,
            IllegalStateException {

        JSONObject json = new JSONObject();
        long requestId = getRequestId();
        mRequestTrackerSeed.startTrack(requestId, callback);
        handlerTrackerTask(true);

        try {
            json.put("requestId", requestId);
            json.put("type", "SEEK");
            json.put("mediaSessionId", getMediaSessionId());
            json.put("currentTime",
                    DoubleAndLongConverter.long2double(currentTime));
            if (resumeState == 1) {
                json.put("resumeState", "PLAYBACK_START");
            } else if (resumeState == 2) {
                json.put("resumeState", "PLAYBACK_PAUSE");
            }
            if (customData != null) {
                json.put("customData", customData);
            }
        } catch (JSONException e) {
        }

        sendTextMessage(json.toString(), requestId, null);

        return requestId;
    }

    /**
     * Set stream volume
     * 
     * @param callback
     * @param level
     * @param customData
     * @return
     * @throws IOException
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     */
    public long setStreamVolume(RequestTrackerCallback callback, double level,
            JSONObject customData) throws IOException, IllegalStateException,
            IllegalArgumentException {

        if ((Double.isInfinite(level)) || (Double.isNaN(level))) {
            throw new IllegalArgumentException("Volume cannot be " + level);
        }

        JSONObject json = new JSONObject();
        long requestId = getRequestId();
        mRequestTrackerVolume.startTrack(requestId, callback);
        handlerTrackerTask(true);

        try {
            json.put("requestId", requestId);
            json.put("type", "SET_VOLUME");
            json.put("mediaSessionId", getMediaSessionId());
            JSONObject volumeJson = new JSONObject();
            volumeJson.put("level", level);
            json.put("volume", volumeJson);
            if (customData != null) {
                json.put("customData", customData);
            }
        } catch (JSONException e) {
        }

        sendTextMessage(json.toString(), requestId, null);

        return requestId;
    }

    /**
     * mute stream
     * 
     * @param callback
     * @param muted
     * @param customData
     * @return
     * @throws IOException
     * @throws IllegalStateException
     */
    public long setStreamMute(RequestTrackerCallback callback, boolean muted,
            JSONObject customData) throws IOException, IllegalStateException {

        JSONObject json = new JSONObject();
        long requestId = getRequestId();
        mRequestTrackerMute.startTrack(requestId, callback);
        handlerTrackerTask(true);

        try {
            json.put("requestId", requestId);
            json.put("type", "SET_VOLUME");
            json.put("mediaSessionId", getMediaSessionId());
            JSONObject volumeJson = new JSONObject();
            volumeJson.put("muted", muted);
            json.put("volume", volumeJson);
            if (customData != null) {
                json.put("customData", customData);
            }
        } catch (JSONException localJSONException) {
        }

        sendTextMessage(json.toString(), requestId, null);

        return requestId;
    }

    /**
     * Get status
     * 
     * @param callback
     * @return
     * @throws IOException
     */
    public long requestStatus(RequestTrackerCallback callback)
            throws IOException {

        JSONObject json = new JSONObject();
        long requestId = getRequestId();
        mRequestTrackerRequestStatus.startTrack(requestId, callback);
        handlerTrackerTask(true);

        try {
            json.put("requestId", requestId);
            json.put("type", "GET_STATUS");
            if (mMediaStatus != null) {
                json.put("mediaSessionId", mMediaStatus.getMediaSessionId());
            }
        } catch (JSONException e) {
        }

        sendTextMessage(json.toString(), requestId, null);

        return requestId;
    }

    /**
     * Get media approximate stream position
     * 
     * @return stream position
     */
    public long getApproximateStreamPosition() {
        MediaInfo mediaInfo = getMediaInfo();
        if (mediaInfo == null) {
            return 0L;
        }

        if (mMediaStartTime == 0L) {
            return 0L;
        }

        double playbackRate = mMediaStatus.getPlaybackRate();
        long streamPosition = mMediaStatus.getStreamPosition();
        int playState = mMediaStatus.getPlayerState();
        if ((playbackRate == 0.0D)
                || (playState != MediaStatus.PLAYER_STATE_PLAYING)) {
            return streamPosition;
        }
        long playedTime = SystemClock.elapsedRealtime() - mMediaStartTime;
        if (playedTime < 0L) {
            playedTime = 0L;
        }
        if (playedTime == 0L) {
            return streamPosition;
        }

        long duration = mediaInfo.getStreamDuration();
        long position = (long) (streamPosition + (playedTime * playbackRate));
        if (position > duration) {
            position = duration;
        } else if (position < 0L) {
            position = 0L;
        }

        return position;
    }

    /**
     * Get media stream duration
     * 
     * @return media duration, in milliseconds.
     */
    public long getStreamDuration() {
        MediaInfo mediaInfo = getMediaInfo();
        return ((mediaInfo != null) ? mediaInfo.getStreamDuration() : 0L);
    }

    /**
     * Get current media status
     * 
     * @return media status
     */
    public MediaStatus getMediaStatus() {
        return this.mMediaStatus;
    }

    /**
     * Get current media info
     * 
     * @return media info
     */
    public MediaInfo getMediaInfo() {
        return ((this.mMediaStatus == null) ? null : this.mMediaStatus
                .getMediaInfo());
    }

    /**
     * Called when string message received
     * 
     * @param message
     */
    @Override
    public final void onMessageReceived(String message) {
        log.d("message received: %s", message);

        try {
            JSONObject jsonMessage = new JSONObject(message);
            String type = jsonMessage.getString("type");
            long requestId = jsonMessage.optLong("requestId", -1L);
            if (type.equals("MEDIA_STATUS")) {
                JSONArray status = jsonMessage.getJSONArray("status");
                if (status.length() > 0) {
                    updateMediaStatus(requestId, status.getJSONObject(0));
                } else {
                    this.mMediaStatus = null;
                    onStatusUpdated();
                    onMetadataUpdated();
                    mRequestTrackerRequestStatus.trackRequest(requestId, 0);
                }
            } else if (type.equals("INVALID_PLAYER_STATE")) {
                this.log.w("received unexpected error: Invalid Player State.",
                        new Object[0]);
                JSONObject customData = jsonMessage.optJSONObject("customData");
                this.mRequestTrackerLoad.trackRequest(requestId, 1, customData);
                this.mRequestTrackerPause
                        .trackRequest(requestId, 1, customData);
                this.mRequestTrackerPlay.trackRequest(requestId, 1, customData);
                this.mRequestTrackerStop.trackRequest(requestId, 1, customData);
                this.mRequestTrackerSeed.trackRequest(requestId, 1, customData);
                this.mRequestTrackerVolume.trackRequest(requestId, 1,
                        customData);
                this.mRequestTrackerMute.trackRequest(requestId, 1, customData);
                this.mRequestTrackerRequestStatus.trackRequest(requestId, 1,
                        customData);
            } else if (type.equals("LOAD_FAILED")) {
                JSONObject customData = jsonMessage.optJSONObject("customData");
                this.mRequestTrackerLoad.trackRequest(requestId, 1,
                        (JSONObject) customData);
            } else if (type.equals("LOAD_CANCELLED")) {
                JSONObject customData = jsonMessage.optJSONObject("customData");
                this.mRequestTrackerLoad.trackRequest(requestId, 2,
                        (JSONObject) customData);
            } else if (type.equals("INVALID_REQUEST")) {
                this.log.w("received unexpected error: Invalid Request.",
                        new Object[0]);
                JSONObject customData = jsonMessage.optJSONObject("customData");
                this.mRequestTrackerLoad.trackRequest(requestId, 1, customData);
                this.mRequestTrackerPause
                        .trackRequest(requestId, 1, customData);
                this.mRequestTrackerPlay.trackRequest(requestId, 1, customData);
                this.mRequestTrackerStop.trackRequest(requestId, 1, customData);
                this.mRequestTrackerSeed.trackRequest(requestId, 1, customData);
                this.mRequestTrackerVolume.trackRequest(requestId, 1,
                        customData);
                this.mRequestTrackerMute.trackRequest(requestId, 1, customData);
                this.mRequestTrackerRequestStatus.trackRequest(requestId, 1,
                        customData);
            }
        } catch (JSONException e) {
            this.log.w("Message is malformed (%s); ignoring: %s",
                    e.getMessage(), message);
        }
    }

    /**
     * Track un-success request
     * 
     * @param requestId
     * @param statusCode
     */
    @Override
    public void trackFailedRequests(long requestId, int statusCode) {
        this.mRequestTrackerLoad.trackRequest(requestId, statusCode);
        this.mRequestTrackerPause.trackRequest(requestId, statusCode);
        this.mRequestTrackerPlay.trackRequest(requestId, statusCode);
        this.mRequestTrackerStop.trackRequest(requestId, statusCode);
        this.mRequestTrackerSeed.trackRequest(requestId, statusCode);
        this.mRequestTrackerVolume.trackRequest(requestId, statusCode);
        this.mRequestTrackerMute.trackRequest(requestId, statusCode);
        this.mRequestTrackerRequestStatus.trackRequest(requestId, statusCode);
    }

    private void updateMediaStatus(long requestId, JSONObject json)
            throws JSONException {
        boolean bool = this.mRequestTrackerLoad.isCurrentRequestId(requestId);
        int i = ((this.mRequestTrackerSeed.isRequestIdAvailable()) && (!(this.mRequestTrackerSeed
                .isCurrentRequestId(requestId)))) ? 1 : 0;
        int j = (((this.mRequestTrackerVolume.isRequestIdAvailable()) && (!(this.mRequestTrackerVolume
                .isCurrentRequestId(requestId)))) || ((this.mRequestTrackerMute
                .isRequestIdAvailable()) && (!(this.mRequestTrackerMute
                .isCurrentRequestId(requestId))))) ? 1 : 0;
        int k = 0;
        if (i != 0) {
            k |= 2;
        }
        if (j != 0) {
            k |= 1;
        }
        int updateBits = 0;
        if ((bool) || (this.mMediaStatus == null)) {
            this.mMediaStatus = new MediaStatus(json);
            this.mMediaStartTime = SystemClock.elapsedRealtime();
            updateBits = 7;
        } else {
            updateBits = this.mMediaStatus.setMediaStatusWithJson(json, k);
        }
        if ((updateBits & MediaStatus.UPDATE_SESSION_MASK) != 0) {
            this.mMediaStartTime = SystemClock.elapsedRealtime();
            onStatusUpdated();
        }
        if ((updateBits & MediaStatus.UPDATE_MEDIA_STATUS_MASK) != 0) {
            this.mMediaStartTime = SystemClock.elapsedRealtime();
            onStatusUpdated();
        }
        if ((updateBits & MediaStatus.UPDATE_METADATA_MASK) != 0) {
            onMetadataUpdated();
        }
        this.mRequestTrackerLoad.trackRequest(requestId, 0);
        this.mRequestTrackerPause.trackRequest(requestId, 0);
        this.mRequestTrackerPlay.trackRequest(requestId, 0);
        this.mRequestTrackerStop.trackRequest(requestId, 0);
        this.mRequestTrackerSeed.trackRequest(requestId, 0);
        this.mRequestTrackerVolume.trackRequest(requestId, 0);
        this.mRequestTrackerMute.trackRequest(requestId, 0);
        this.mRequestTrackerRequestStatus.trackRequest(requestId, 0);
    }

    public long getMediaSessionId() throws IllegalStateException {
        if (mMediaStatus == null) {
            throw new IllegalStateException("No current media session");
        }
        return mMediaStatus.getMediaSessionId();
    }

    protected void onStatusUpdated() {
    }

    protected void onMetadataUpdated() {
    }

    private void cleanInternal() {
        handlerTrackerTask(false);
        mMediaStartTime = 0L;
        mMediaStatus = null;
        this.mRequestTrackerLoad.clear();
        this.mRequestTrackerSeed.clear();
        this.mRequestTrackerVolume.clear();
    }

    public void clean() {
        cleanInternal();
    }

    private void handlerTrackerTask(boolean start) {
        if (this.mTrackTaskStarting == start) {
            return;
        }

        this.mTrackTaskStarting = start;

        if (start) {
            mHandler.postDelayed(this.mTrackerTask, MillisPerSecond);
        } else {
            mHandler.removeCallbacks(this.mTrackerTask);
        }
    }

    private class RequestTrackerTask implements Runnable {
        @Override
        public void run() {
            mTrackTaskStarting = false;

            long timestamp = SystemClock.elapsedRealtime();
            mRequestTrackerLoad.trackRequestTimeout(timestamp, 3);
            mRequestTrackerPause.trackRequestTimeout(timestamp, 3);
            mRequestTrackerPlay.trackRequestTimeout(timestamp, 3);
            mRequestTrackerStop.trackRequestTimeout(timestamp, 3);
            mRequestTrackerSeed.trackRequestTimeout(timestamp, 3);
            mRequestTrackerVolume.trackRequestTimeout(timestamp, 3);
            mRequestTrackerMute.trackRequestTimeout(timestamp, 3);
            mRequestTrackerRequestStatus.trackRequestTimeout(timestamp, 3);

            boolean needStart = false;
            synchronized (RequestTracker.mLock) {
                needStart = (mRequestTrackerLoad.isRequestIdAvailable())
                        || (mRequestTrackerSeed.isRequestIdAvailable())
                        || (mRequestTrackerVolume.isRequestIdAvailable())
                        || (mRequestTrackerMute.isRequestIdAvailable())
                        || (mRequestTrackerRequestStatus.isRequestIdAvailable());
            }
            handlerTrackerTask(needStart);
        }
    }

}
