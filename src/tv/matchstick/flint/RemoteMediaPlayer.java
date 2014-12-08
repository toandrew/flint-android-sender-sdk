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

import java.io.IOException;

import org.json.JSONObject;

import tv.matchstick.client.internal.FlintClientImpl;
import tv.matchstick.client.internal.MediaControlChannel;
import tv.matchstick.client.internal.MessageSender;
import tv.matchstick.client.internal.RequestTrackerCallback;

/**
 * Class for controlling a media player application running on a receiver.
 * 
 * Some operations, like loading of media or adjusting volume, can be tracked.
 * The corresponding methods return a PendingResult for this purpose.
 */
public class RemoteMediaPlayer implements Flint.MessageReceivedCallback {
    /**
     * A resume state indicating that the player state should be left unchanged.
     */
    public static final int RESUME_STATE_UNCHANGED = 0;

    /**
     * A resume state indicating that the player should be playing, regardless
     * of its current state.
     */
    public static final int RESUME_STATE_PLAY = 1;

    /**
     * A resume state indicating that the player should be paused, regardless of
     * its current state.
     */
    public static final int RESUME_STATE_PAUSE = 2;

    /**
     * A status indicating that a request completed successfully.
     */
    public static final int STATUS_SUCCEEDED = 0;

    /**
     * A status indicating that a request failed.
     */
    public static final int STATUS_FAILED = 1;

    /**
     * A status indicating that a request was canceled.
     */
    public static final int STATUS_CANCELED = 2;

    /**
     * A status indicating that a request has timed out.
     */
    public static final int STATUS_TIMED_OUT = 3;

    /**
     * A status indicating that the request's progress is no longer being
     * tracked because another request of the same type has been made before the
     * first request completed.
     */
    public static final int STATUS_REPLACED = 4;

    /**
     * Media player lock object
     */
    private final Object mLock = new Object();

    /**
     * Media control channel implementation
     */
    private final MediaControlChannel mMediaControlChannel = new MediaControlChannel() {
        /**
         * Called when status updated
         */
        protected void onStatusUpdated() {
            RemoteMediaPlayer.this.onStatusUpdated();
        }

        /**
         * Called when media meta data is updated
         */
        protected void onMetadataUpdated() {
            RemoteMediaPlayer.this.onMetadataUpdated();
        }
    };

    /**
     * Message sender instance
     */
    private final MessageSenderImpl mMessageSender = new MessageSenderImpl();

    /**
     * Meta data updated listener
     */
    private OnMetadataUpdatedListener mMetadataUpdatedListener;

    /**
     * Status updated listener
     */
    private OnStatusUpdatedListener mStatusUpdatedListener;

    /**
     * Init media player
     */
    public RemoteMediaPlayer() {
        this.mMediaControlChannel.setMessageSender(this.mMessageSender);
    }

    /**
     * Loads and automatically starts playback of a new media item.
     * 
     * @param manager
     *            with which to perform the operation.
     * @param mediaInfo
     *            describing the media item to load.
     * @return A PendingResult which can be used to track the progress of the
     *         request.
     */
    public PendingResult<MediaChannelResult> load(FlintManager manager,
            MediaInfo mediaInfo) {
        return load(manager, mediaInfo, true, 0L, null);
    }

    /**
     * Loads and optionally starts playback of a new media item.
     * 
     * @param manager
     *            with which to perform the operation.
     * @param mediaInfo
     *            describing the media item to load.
     * @param autoplay
     *            Whether playback should start immediately.
     * @return A PendingResult which can be used to track the progress of the
     *         request.
     */
    public PendingResult<MediaChannelResult> load(FlintManager manager,
            MediaInfo mediaInfo, boolean autoplay) {
        return load(manager, mediaInfo, autoplay, 0L, null);
    }

    /**
     * Loads and optionally starts playback of a new media item.
     * 
     * @param manager
     *            with which to perform the operation.
     * @param mediaInfo
     *            describing the media item to load.
     * @param autoplay
     *            Whether playback should start immediately.
     * @param playPosition
     *            Whether playback should start immediately.
     * @return A PendingResult which can be used to track the progress of the
     *         request.
     */
    public PendingResult<MediaChannelResult> load(FlintManager manager,
            MediaInfo mediaInfo, boolean autoplay, long playPosition) {
        return load(manager, mediaInfo, autoplay, playPosition, null);
    }

    /**
     * Loads and optionally starts playback of a new media item.
     * 
     * This method optionally sends custom data as a JSONObject with the load
     * request.
     * 
     * @param manager
     *            with which to perform the operation.
     * @param mediaInfo
     *            An object describing the media item to load.
     * @param autoplay
     *            Whether playback should start immediately.
     * @param playPosition
     *            The initial playback position, in milliseconds from the
     *            beginning of the stream.
     * @param customData
     *            Custom application-specific data to pass along with the
     *            request.
     * @return A PendingResult which can be used to track the progress of the
     *         request.
     */
    public PendingResult<MediaChannelResult> load(final FlintManager manager,
            final MediaInfo mediaInfo, final boolean autoplay,
            final long playPosition, final JSONObject customData) {
        return manager.executeTask(new MediaChannelResultHandler() {
            protected void execute(FlintClientImpl client) {
                synchronized (mLock) {
                    mMessageSender.setApiClient(manager);
                    try {
                        mMediaControlChannel.load(this.requestTrackerCallback,
                                mediaInfo, autoplay, playPosition, customData);
                    } catch (IOException e) {
                        postResult(createMediaChannelResult(new Status(
                                STATUS_FAILED)));
                    } finally {
                        mMessageSender.setApiClient(null);
                    }
                }
            }
        });
    }

    /**
     * Pause media
     * 
     * @param manager
     *            with which to perform the operation.
     * @return A PendingResult which can be used to track the progress of the
     *         request.
     */
    public PendingResult<MediaChannelResult> pause(FlintManager manager) {
        return pause(manager, null);
    }

    /**
     * Pause media
     * 
     * @param manager
     *            with which to perform the operation.
     * @param customData
     *            Custom application-specific data to pass along with the
     *            request.
     * @return A PendingResult which can be used to track the progress of the
     *         request.
     */
    public PendingResult<MediaChannelResult> pause(final FlintManager manager,
            final JSONObject customData) {
        return manager.executeTask(new MediaChannelResultHandler() {
            protected void execute(FlintClientImpl client) {
                synchronized (mLock) {
                    mMessageSender.setApiClient(manager);
                    try {
                        mMediaControlChannel.pause(this.requestTrackerCallback,
                                customData);
                    } catch (IOException e) {
                        postResult(createMediaChannelResult(new Status(
                                STATUS_FAILED)));
                    } finally {
                        mMessageSender.setApiClient(null);
                    }
                }
            }
        });
    }

    /**
     * Stop media
     * 
     * @param manager
     *            with which to perform the operation.
     * @return A PendingResult which can be used to track the progress of the
     *         request.
     */
    public PendingResult<MediaChannelResult> stop(FlintManager manager) {
        return stop(manager, null);
    }

    /**
     * Stop media
     * 
     * @param manager
     *            with which to perform the operation.
     * @param customData
     *            Custom application-specific data to pass along with the
     *            request.
     * @return A PendingResult which can be used to track the progress of the
     *         request.
     */
    public PendingResult<MediaChannelResult> stop(final FlintManager manager,
            final JSONObject customData) {
        return manager.executeTask(new MediaChannelResultHandler() {
            protected void execute(FlintClientImpl client) {
                synchronized (mLock) {
                    mMessageSender.setApiClient(manager);
                    try {
                        mMediaControlChannel.stop(this.requestTrackerCallback,
                                customData);
                    } catch (IOException e) {
                        postResult(createMediaChannelResult(new Status(
                                STATUS_FAILED)));
                    } finally {
                        mMessageSender.setApiClient(null);
                    }
                }
            }
        });
    }

    /**
     * Play media
     * 
     * @param manager
     *            with which to perform the operation.
     * @return A PendingResult which can be used to track the progress of the
     *         request.
     */
    public PendingResult<MediaChannelResult> play(FlintManager manager) {
        return play(manager, null);
    }

    /**
     * Play media
     * 
     * @param manager
     *            with which to perform the operation.
     * @param customData
     *            Custom application-specific data to pass along with the
     *            request.
     * @return A PendingResult which can be used to track the progress of the
     *         request.
     */
    public PendingResult<MediaChannelResult> play(final FlintManager manager,
            final JSONObject customData) {
        return manager.executeTask(new MediaChannelResultHandler() {
            protected void execute(FlintClientImpl client) {
                synchronized (mLock) {
                    mMessageSender.setApiClient(manager);
                    try {
                        mMediaControlChannel.play(this.requestTrackerCallback,
                                customData);
                    } catch (IOException e) {
                        postResult(createMediaChannelResult(new Status(
                                STATUS_FAILED)));
                    } finally {
                        mMessageSender.setApiClient(null);
                    }
                }
            }
        });
    }

    /**
     * Seek media
     * 
     * @param manager
     *            with which to perform the operation.
     * @param position
     *            The new position, in milliseconds from the beginning of the
     *            stream.
     * @return A PendingResult which can be used to track the progress of the
     *         request.
     */
    public PendingResult<MediaChannelResult> seek(FlintManager manager,
            long position) {
        return seek(manager, position, 0, null);
    }

    /**
     * Seek media
     * 
     * @param manager
     *            with which to perform the operation.
     * @param position
     *            The new position, in milliseconds from the beginning of the
     *            stream.
     * @param resumeState
     *            The action to take after the seek operation has finished.
     * @return A PendingResult which can be used to track the progress of the
     *         request.
     */
    public PendingResult<MediaChannelResult> seek(FlintManager manager,
            long position, int resumeState) {
        return seek(manager, position, resumeState, null);
    }

    /**
     * Seek media
     * 
     * @param manager
     *            with which to perform the operation.
     * @param position
     *            The new position, in milliseconds from the beginning of the
     *            stream.
     * @param resumeState
     *            The action to take after the seek operation has finished.
     * @param customData
     *            Custom application-specific data to pass along with the
     *            request.
     * @return A PendingResult which can be used to track the progress of the
     *         request.
     */
    public PendingResult<MediaChannelResult> seek(final FlintManager manager,
            final long position, final int resumeState,
            final JSONObject customData) {
        return manager.executeTask(new MediaChannelResultHandler() {
            protected void execute(FlintClientImpl client) {
                synchronized (mLock) {
                    mMessageSender.setApiClient(manager);
                    try {
                        mMediaControlChannel.seek(this.requestTrackerCallback,
                                position, resumeState, customData);
                    } catch (IOException e) {
                        postResult(createMediaChannelResult(new Status(
                                STATUS_FAILED)));
                    } finally {
                        mMessageSender.setApiClient(null);
                    }
                }
            }
        });
    }

    /**
     * Set media stream volume
     * 
     * If volume is outside of the range [0.0, 1.0], then the value will be
     * clipped.
     * 
     * @param manager
     *            with which to perform the operation.
     * @param volume
     *            The new volume, in the range [0.0 - 1.0].
     * @return A PendingResult which can be used to track the progress of the
     *         request.
     * @throws IllegalArgumentException
     *             If the volume is infinity or NaN.
     */
    public PendingResult<MediaChannelResult> setStreamVolume(
            FlintManager manager, double volume)
            throws IllegalArgumentException {
        return setStreamVolume(manager, volume, null);
    }

    /**
     * Set media stream volume
     * 
     * @param manager
     *            with which to perform the operation.
     * @param volume
     *            The new volume, in the range [0.0 - 1.0].
     * @param customData
     *            Custom application-specific data to pass along with the
     *            request.
     * @return A PendingResult which can be used to track the progress of the
     *         request.
     * @throws IllegalArgumentException
     *             If the volume is infinity or NaN.
     */
    public PendingResult<MediaChannelResult> setStreamVolume(
            final FlintManager manager, final double volume,
            final JSONObject customData) throws IllegalArgumentException {
        if ((Double.isInfinite(volume)) || (Double.isNaN(volume))) {
            throw new IllegalArgumentException("Volume cannot be " + volume);
        }

        return manager.executeTask(new MediaChannelResultHandler() {
            protected void execute(FlintClientImpl cllient) {
                synchronized (mLock) {
                    mMessageSender.setApiClient(manager);
                    try {
                        mMediaControlChannel
                                .setStreamVolume(this.requestTrackerCallback,
                                        volume, customData);
                    } catch (IllegalStateException e) {
                        postResult(createMediaChannelResult(new Status(
                                STATUS_FAILED)));
                    } catch (IllegalArgumentException ex) {
                        postResult(createMediaChannelResult(new Status(
                                STATUS_FAILED)));
                    } catch (IOException exception) {
                        postResult(createMediaChannelResult(new Status(
                                STATUS_FAILED)));
                    } finally {
                        mMessageSender.setApiClient(null);
                    }
                }
            }
        });
    }

    /**
     * Set media stream mute status
     * 
     * @param manager
     *            with which to perform the operation.
     * @param muteState
     *            Whether the stream should be muted or unmuted.
     * @return A PendingResult which can be used to track the progress of the
     *         request.
     */
    public PendingResult<MediaChannelResult> setStreamMute(
            FlintManager manager, boolean muteState) {
        return setStreamMute(manager, muteState, null);
    }

    /**
     * Set media stream mute status
     * 
     * @param manager
     *            with which to perform the operation.
     * @param muteState
     *            Whether the stream should be muted or unmuted.
     * @param customData
     *            Custom application-specific data to pass along with the
     *            request.
     * @return A PendingResult which can be used to track the progress of the
     *         request.
     */
    public PendingResult<MediaChannelResult> setStreamMute(
            final FlintManager manager, final boolean muteState,
            final JSONObject customData) {
        return manager.executeTask(new MediaChannelResultHandler() {
            protected void execute(FlintClientImpl client) {
                synchronized (mLock) {
                    mMessageSender.setApiClient(manager);
                    try {
                        mMediaControlChannel.setStreamMute(
                                this.requestTrackerCallback, muteState,
                                customData);
                    } catch (IllegalStateException e) {
                        postResult(createMediaChannelResult(new Status(
                                STATUS_FAILED)));
                    } catch (IOException ex) {
                        postResult(createMediaChannelResult(new Status(
                                STATUS_FAILED)));
                    } finally {
                        mMessageSender.setApiClient(null);
                    }
                }
            }
        });
    }

    /**
     * Request media status
     * 
     * Requests updated media status information from the receiver.
     * RemoteMediaPlayer.OnStatusUpdatedListener callback will be triggered,
     * when the updated media status has been received. This will also update
     * the internal state of the RemoteMediaPlayer object with the current state
     * of the receiver, including the current session ID. This method should be
     * called when joining an application that supports the media control
     * namespace.
     * 
     * @param manager
     *            with which to perform the operation.
     * @return A PendingResult which can be used to track the progress of the
     *         request.
     */
    public PendingResult<MediaChannelResult> requestStatus(
            final FlintManager manager) {
        return manager.executeTask(new MediaChannelResultHandler() {
            protected void execute(FlintClientImpl client) {
                synchronized (mLock) {
                    mMessageSender.setApiClient(manager);
                    try {
                        mMediaControlChannel
                                .requestStatus(this.requestTrackerCallback);
                    } catch (IOException e) {
                        postResult(createMediaChannelResult(new Status(
                                STATUS_FAILED)));
                    } finally {
                        mMessageSender.setApiClient(null);
                    }
                }
            }
        });
    }

    /**
     * Get media approximate stream position
     * 
     * Returns the approximate stream position as calculated from the last
     * received stream information and the elapsed wall-time since that update.
     * 
     * @return The approximate stream position, in milliseconds.
     */
    public long getApproximateStreamPosition() {
        synchronized (mLock) {
            return mMediaControlChannel.getApproximateStreamPosition();
        }
    }

    /**
     * Get media stream duration
     * 
     * @return media duration, in milliseconds.
     */
    public long getStreamDuration() {
        synchronized (mLock) {
            return mMediaControlChannel.getStreamDuration();
        }
    }

    /**
     * Get current media status
     * 
     * @return media status
     */
    public MediaStatus getMediaStatus() {
        synchronized (mLock) {
            return mMediaControlChannel.getMediaStatus();
        }
    }

    /**
     * Get current media info
     * 
     * @return media info
     */
    public MediaInfo getMediaInfo() {
        synchronized (mLock) {
            return mMediaControlChannel.getMediaInfo();
        }
    }

    /**
     * Set status updated listener
     * 
     * @param listener
     */
    public void setOnStatusUpdatedListener(OnStatusUpdatedListener listener) {
        mStatusUpdatedListener = listener;
    }

    /**
     * Called when current status changed to get status updates.
     */
    private void onStatusUpdated() {
        if (mStatusUpdatedListener == null) {
            return;
        }
        mStatusUpdatedListener.onStatusUpdated();
    }

    /**
     * Set media meta data updated listener to get meta data updates.
     * 
     * @param listener
     */
    public void setOnMetadataUpdatedListener(OnMetadataUpdatedListener listener) {
        mMetadataUpdatedListener = listener;
    }

    /**
     * Called when media meta data updated
     */
    private void onMetadataUpdated() {
        if (mMetadataUpdatedListener == null) {
            return;
        }
        mMetadataUpdatedListener.onMetadataUpdated();
    }

    /**
     * Get current media control channel's namespace
     * 
     * @return
     */
    public String getNamespace() {
        return mMediaControlChannel.getNamespace();
    }

    /**
     * Called when message received from a given device
     * 
     * @param flintDevice
     *            from where the message originated.
     * @param namespace
     *            The namespace of the received message.
     * @param message
     *            The received payload for the message.
     */
    public void onMessageReceived(FlintDevice flintDevice, String namespace,
            String message) {
        mMediaControlChannel.onMessageReceived(message);
    }

    /**
     * Media channel result handler
     */
    private static abstract class MediaChannelResultHandler extends
            Flint.PendingResultHandler<MediaChannelResult> {
        /**
         * Request tracker callback
         */
        RequestTrackerCallback requestTrackerCallback = new RequestTrackerCallback() {
            /**
             * Called when sign in required
             */
            public void onSignInRequired(long requestId) {
                postResult(createMediaChannelResult(new Status(
                        ConnectionResult.SIGN_IN_REQUIRED)));
            }

            /**
             * Called when there's track request
             */
            public void onTrackRequest(long requestId, int statusCode,
                    JSONObject customData) {
                postResult(new MediaChannelResultImpl(new Status(statusCode),
                        customData));
            }
        };

        /**
         * Create result with status
         * 
         * @param status
         */
        protected MediaChannelResult createResult(final Status status) {
            return new MediaChannelResult() {
                public Status getStatus() {
                    return status;
                }
            };
        };

        /**
         * Create media channel result
         * 
         * @param status
         * @return
         */
        public MediaChannelResult createMediaChannelResult(final Status status) {
            return new MediaChannelResult() {
                public Status getStatus() {
                    return status;
                }
            };
        }
    }

    /**
     * Media channel result implementation
     */
    private static final class MediaChannelResultImpl implements
            MediaChannelResult {
        private final Status status;
        private final JSONObject json;

        MediaChannelResultImpl(Status status, JSONObject json) {
            this.status = status;
            this.json = json;
        }

        public Status getStatus() {
            return status;
        }
    }

    /**
     * Message Sender implementation
     */
    private class MessageSenderImpl implements MessageSender {
        /**
         * Flint Api client
         */
        private FlintManager manager;

        /**
         * request Id
         */
        private long requestId = 0L;

        /**
         * Set API client
         * 
         * @param manager
         */
        public void setApiClient(FlintManager manager) {
            this.manager = manager;
        }

        /**
         * Get next request Id
         */
        public long getRequestId() {
            return (++this.requestId);
        }

        /**
         * Send message to device
         * 
         * @param namespace
         * @param message
         * @param requestId
         * @param targetId
         * 
         */
        public void sendTextMessage(String namespace, String message,
                long requestId, String targetId) throws IOException {
            if (manager == null) {
                throw new IOException("No FlintManager available");
            }

            Flint.FlintApi.sendMessage(manager, namespace, message)
                    .setResultCallback(new RequestResultCallback(requestId));
        }

        /**
         * Request result callback
         */
        private final class RequestResultCallback implements
                ResultCallback<Status> {
            /**
             * request Id
             */
            private final long reqId;

            /**
             * Init with request Id
             * 
             * @param requestId
             */
            RequestResultCallback(long requestId) {
                this.reqId = requestId;
            }

            @Override
            public void onResult(Status status) {
                // TODO Auto-generated method stub
                if (status.isSuccess()) {
                    return;
                }

                mMediaControlChannel.trackFailedRequests(this.reqId,
                        status.getStatusCode());
            }
        }

    }

    /**
     * Used to present a result of media command which is sent to Flint device
     */
    public interface MediaChannelResult extends Result {
    }

    /**
     * The listener interface for tracking media meta data changes.
     */
    public interface OnMetadataUpdatedListener {
        /**
         * Called when updated media metadata is received.
         */
        public abstract void onMetadataUpdated();
    }

    /**
     * The listener interface for tracking player status changes.
     */
    public interface OnStatusUpdatedListener {
        /**
         * Called when updated player status information is received.
         */
        public abstract void onStatusUpdated();
    }

}
