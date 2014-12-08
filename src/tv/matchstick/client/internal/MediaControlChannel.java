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

import tv.matchstick.flint.MediaInfo;
import tv.matchstick.flint.MediaStatus;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

/**
 * Media control channel
 */
public class MediaControlChannel extends FlintChannel {
    // json.put("requestId", requestId);
    // json.put("type", "LOAD");
    // json.put("media", mediaInfo.buildJson());
    // json.put("autoplay", autoplay);
    // json.put("currentTime",
    // DoubleAndLongConverter.long2double(currentTime));
    // if (customData != null) {
    // json.put("customData", customData);
    // }
    private static final String MSG_KEY_REQUESTID = "requestId";
    private static final String MSG_KEY_TYPE = "type";
    private static final String MSG_KEY_MEDIA = "media";
    private static final String MSG_KEY_AUTOPLAY = "autoplay";
    private static final String MSG_KEY_CURRENT_TIME = "currentTime";
    private static final String MSG_KEY_CUSTOM_DATA = "customData";
    private static final String MSG_KEY_MEDIA_SESSION_ID = "mediaSessionId";
    private static final String MSG_KEY_RESUME_STATE = "resumeState";
    private static final String MSG_KEY_VOLUME = "volume";

    private static final String MSG_TYPE_LOAD = "LOAD";
    private static final String MSG_TYPE_PLAY = "PLAY";
    private static final String MSG_TYPE_PAUSE = "PAUSE";
    private static final String MSG_TYPE_STOP = "STOP";
    private static final String MSG_TYPE_SEEK = "SEEK";
    private static final String MSG_TYPE_SET_VOLUME = "SET_VOLUME";
    private static final String MSG_TYPE_GET_STATUS = "GET_STATUS";
    private static final String MSG_TYPE_MEDIA_STATUS = "MEDIA_STATUS";
    private static final String MSG_TYPE_INVALID_PLAYER_STATE = "INVALID_PLAYER_STATE";
    private static final String MSG_TYPE_LOAD_FAILED = "LOAD_FAILED";
    private static final String MSG_TYPE_LOAD_CANCELLED = "LOAD_CANCELLED";
    private static final String MSG_TYPE_INVALID_REQUEST = "INVALID_REQUEST";

    private static final String MSG_RESUME_STATE_START = "PLAYBACK_START";
    private static final String MSG_RESUME_STATE_PAUSE = "PLAYBACK_PAUSE";

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
    private final RequestTracker mRequestTrackerSeek = new RequestTracker(
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
        super("urn:flint:org.openflint.fling.media", "MediaControlChannel");

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
            json.put(MSG_KEY_REQUESTID, requestId);
            json.put(MSG_KEY_TYPE, MSG_TYPE_LOAD);
            json.put(MSG_KEY_MEDIA, mediaInfo.buildJson());
            json.put(MSG_KEY_AUTOPLAY, autoplay);
            json.put(MSG_KEY_CURRENT_TIME,
                    DoubleAndLongConverter.long2double(currentTime));
            if (customData != null) {
                json.put(MSG_KEY_CUSTOM_DATA, customData);
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
            json.put(MSG_KEY_REQUESTID, requestId);
            json.put(MSG_KEY_TYPE, MSG_TYPE_PAUSE);
            json.put(MSG_KEY_MEDIA_SESSION_ID, getMediaSessionId());
            if (customData != null) {
                json.put(MSG_KEY_CUSTOM_DATA, customData);
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
            json.put(MSG_KEY_REQUESTID, requestId);
            json.put(MSG_KEY_TYPE, MSG_TYPE_STOP);
            json.put(MSG_KEY_MEDIA_SESSION_ID, getMediaSessionId());
            if (customData != null) {
                json.put(MSG_KEY_CUSTOM_DATA, customData);
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
            json.put(MSG_KEY_REQUESTID, requestId);
            json.put(MSG_KEY_TYPE, MSG_TYPE_PLAY);
            json.put(MSG_KEY_MEDIA_SESSION_ID, getMediaSessionId());
            if (customData != null) {
                json.put(MSG_KEY_CUSTOM_DATA, customData);
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
        mRequestTrackerSeek.startTrack(requestId, callback);
        handlerTrackerTask(true);

        try {
            json.put(MSG_KEY_REQUESTID, requestId);
            json.put(MSG_KEY_TYPE, MSG_TYPE_SEEK);
            json.put(MSG_KEY_MEDIA_SESSION_ID, getMediaSessionId());
            json.put(MSG_KEY_CURRENT_TIME,
                    DoubleAndLongConverter.long2double(currentTime));
            if (resumeState == 1) {
                json.put(MSG_KEY_RESUME_STATE, MSG_RESUME_STATE_START);
            } else if (resumeState == 2) {
                json.put(MSG_KEY_RESUME_STATE, MSG_RESUME_STATE_PAUSE);
            }
            if (customData != null) {
                json.put(MSG_KEY_CUSTOM_DATA, customData);
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
            json.put(MSG_KEY_REQUESTID, requestId);
            json.put(MSG_KEY_TYPE, MSG_TYPE_SET_VOLUME);
            json.put(MSG_KEY_MEDIA_SESSION_ID, getMediaSessionId());
            JSONObject volumeJson = new JSONObject();
            volumeJson.put("level", level);
            json.put(MSG_KEY_VOLUME, volumeJson);
            if (customData != null) {
                json.put(MSG_KEY_CUSTOM_DATA, customData);
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
            json.put(MSG_KEY_REQUESTID, requestId);
            json.put(MSG_KEY_TYPE, MSG_TYPE_SET_VOLUME);
            json.put(MSG_KEY_MEDIA_SESSION_ID, getMediaSessionId());
            JSONObject volumeJson = new JSONObject();
            volumeJson.put("muted", muted);
            json.put(MSG_KEY_VOLUME, volumeJson);
            if (customData != null) {
                json.put(MSG_KEY_CUSTOM_DATA, customData);
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
            json.put(MSG_KEY_REQUESTID, requestId);
            json.put(MSG_KEY_TYPE, MSG_TYPE_GET_STATUS);
            if (mMediaStatus != null) {
                json.put(MSG_KEY_MEDIA_SESSION_ID,
                        mMediaStatus.getMediaSessionId());
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
        return mMediaStatus;
    }

    /**
     * Get current media info
     * 
     * @return media info
     */
    public MediaInfo getMediaInfo() {
        return ((this.mMediaStatus == null) ? null : mMediaStatus
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
            String type = jsonMessage.getString(MSG_KEY_TYPE);
            long requestId = jsonMessage.optLong(MSG_KEY_REQUESTID, -1L);
            if (type.equals(MSG_TYPE_MEDIA_STATUS)) {
                JSONArray status = jsonMessage.getJSONArray("status");
                if (status.length() > 0) {
                    updateMediaStatus(requestId, status.getJSONObject(0));
                } else {
                    mMediaStatus = null;
                    onStatusUpdated();
                    onMetadataUpdated();
                    mRequestTrackerRequestStatus.trackRequest(requestId, 0);
                }
            } else if (type.equals(MSG_TYPE_INVALID_PLAYER_STATE)) {
                log.w("received unexpected error: Invalid Player State.",
                        new Object[0]);
                JSONObject customData = jsonMessage
                        .optJSONObject(MSG_KEY_CUSTOM_DATA);
                mRequestTrackerLoad.trackRequest(requestId, 1, customData);
                mRequestTrackerPause.trackRequest(requestId, 1, customData);
                mRequestTrackerPlay.trackRequest(requestId, 1, customData);
                mRequestTrackerStop.trackRequest(requestId, 1, customData);
                mRequestTrackerSeek.trackRequest(requestId, 1, customData);
                mRequestTrackerVolume.trackRequest(requestId, 1, customData);
                mRequestTrackerMute.trackRequest(requestId, 1, customData);
                mRequestTrackerRequestStatus.trackRequest(requestId, 1,
                        customData);
            } else if (type.equals(MSG_TYPE_LOAD_FAILED)) {
                JSONObject customData = jsonMessage
                        .optJSONObject(MSG_KEY_CUSTOM_DATA);
                mRequestTrackerLoad.trackRequest(requestId, 1,
                        (JSONObject) customData);
            } else if (type.equals(MSG_TYPE_LOAD_CANCELLED)) {
                JSONObject customData = jsonMessage
                        .optJSONObject(MSG_KEY_CUSTOM_DATA);
                mRequestTrackerLoad.trackRequest(requestId, 2,
                        (JSONObject) customData);
            } else if (type.equals(MSG_TYPE_INVALID_REQUEST)) {
                log.w("received unexpected error: Invalid Request.",
                        new Object[0]);
                JSONObject customData = jsonMessage
                        .optJSONObject(MSG_KEY_CUSTOM_DATA);
                mRequestTrackerLoad.trackRequest(requestId, 1, customData);
                mRequestTrackerPause.trackRequest(requestId, 1, customData);
                mRequestTrackerPlay.trackRequest(requestId, 1, customData);
                mRequestTrackerStop.trackRequest(requestId, 1, customData);
                mRequestTrackerSeek.trackRequest(requestId, 1, customData);
                mRequestTrackerVolume.trackRequest(requestId, 1, customData);
                mRequestTrackerMute.trackRequest(requestId, 1, customData);
                mRequestTrackerRequestStatus.trackRequest(requestId, 1,
                        customData);
            }
        } catch (JSONException e) {
            log.w("Message is malformed (%s); ignoring: %s", e.getMessage(),
                    message);
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
        mRequestTrackerLoad.trackRequest(requestId, statusCode);
        mRequestTrackerPause.trackRequest(requestId, statusCode);
        mRequestTrackerPlay.trackRequest(requestId, statusCode);
        mRequestTrackerStop.trackRequest(requestId, statusCode);
        mRequestTrackerSeek.trackRequest(requestId, statusCode);
        mRequestTrackerVolume.trackRequest(requestId, statusCode);
        mRequestTrackerMute.trackRequest(requestId, statusCode);
        mRequestTrackerRequestStatus.trackRequest(requestId, statusCode);
    }

    private void updateMediaStatus(long requestId, JSONObject json)
            throws JSONException {
        boolean isCurrentRequestId = mRequestTrackerLoad
                .isCurrentRequestId(requestId);
        int isSeek = ((mRequestTrackerSeek.isRequestIdAvailable()) && (!(mRequestTrackerSeek
                .isCurrentRequestId(requestId)))) ? 1 : 0;
        int isSetVolume = (((mRequestTrackerVolume.isRequestIdAvailable()) && (!(mRequestTrackerVolume
                .isCurrentRequestId(requestId)))) || ((mRequestTrackerMute
                .isRequestIdAvailable()) && (!(mRequestTrackerMute
                .isCurrentRequestId(requestId))))) ? 1 : 0;
        int updateMask = 0;
        if (isSeek != 0) {
            updateMask |= 2;
        }
        if (isSetVolume != 0) {
            updateMask |= 1;
        }
        int updateBits = 0;
        if ((isCurrentRequestId) || (this.mMediaStatus == null)) {
            mMediaStatus = new MediaStatus(json);
            mMediaStartTime = SystemClock.elapsedRealtime();
            updateBits = 7;
        } else {
            updateBits = mMediaStatus.setMediaStatusWithJson(json, updateMask);
        }
        if ((updateBits & MediaStatus.UPDATE_SESSION_MASK) != 0) {
            mMediaStartTime = SystemClock.elapsedRealtime();
            onStatusUpdated();
        }
        if ((updateBits & MediaStatus.UPDATE_MEDIA_STATUS_MASK) != 0) {
            mMediaStartTime = SystemClock.elapsedRealtime();
            onStatusUpdated();
        }
        if ((updateBits & MediaStatus.UPDATE_METADATA_MASK) != 0) {
            onMetadataUpdated();
        }
        mRequestTrackerLoad.trackRequest(requestId, 0);
        mRequestTrackerPause.trackRequest(requestId, 0);
        mRequestTrackerPlay.trackRequest(requestId, 0);
        mRequestTrackerStop.trackRequest(requestId, 0);
        mRequestTrackerSeek.trackRequest(requestId, 0);
        mRequestTrackerVolume.trackRequest(requestId, 0);
        mRequestTrackerMute.trackRequest(requestId, 0);
        mRequestTrackerRequestStatus.trackRequest(requestId, 0);
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
        mRequestTrackerLoad.clear();
        mRequestTrackerSeek.clear();
        mRequestTrackerVolume.clear();
    }

    public void clean() {
        cleanInternal();
    }

    private void handlerTrackerTask(boolean start) {
        if (mTrackTaskStarting == start) {
            return;
        }

        mTrackTaskStarting = start;

        if (start) {
            mHandler.postDelayed(mTrackerTask, MillisPerSecond);
        } else {
            mHandler.removeCallbacks(mTrackerTask);
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
            mRequestTrackerSeek.trackRequestTimeout(timestamp, 3);
            mRequestTrackerVolume.trackRequestTimeout(timestamp, 3);
            mRequestTrackerMute.trackRequestTimeout(timestamp, 3);
            mRequestTrackerRequestStatus.trackRequestTimeout(timestamp, 3);

            boolean needStart = false;
            synchronized (RequestTracker.mLock) {
                needStart = (mRequestTrackerLoad.isRequestIdAvailable())
                        || (mRequestTrackerSeek.isRequestIdAvailable())
                        || (mRequestTrackerVolume.isRequestIdAvailable())
                        || (mRequestTrackerMute.isRequestIdAvailable())
                        || (mRequestTrackerRequestStatus.isRequestIdAvailable());
            }
            handlerTrackerTask(needStart);
        }
    }

}
