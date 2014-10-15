package tv.matchstick.server.fling;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import tv.matchstick.client.internal.FlingChannel;
import tv.matchstick.client.internal.MediaControlChannel;
import tv.matchstick.client.internal.RequestTrackerCallback;
import tv.matchstick.fling.ApplicationMetadata;
import tv.matchstick.fling.FlingDevice;
import tv.matchstick.fling.MediaInfo;
import tv.matchstick.fling.MediaMetadata;
import tv.matchstick.fling.MediaStatus;
import tv.matchstick.fling.images.WebImage;
import tv.matchstick.server.fling.channels.IMediaChannelHelper;
import tv.matchstick.server.fling.media.MediaItemStatusHelper;
import tv.matchstick.server.fling.media.RouteController;
import tv.matchstick.server.fling.media.RouteCtrlRequestCallback;
import tv.matchstick.server.utils.C_dt;

import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public final class FlingRouteController extends RouteController implements
RequestTrackerCallback, IMediaChannelHelper {
    public final FlingDevice mFlingDevice;
    public FlingDeviceController mFlingDeviceController;
    double c;
    String d;
    public MediaRouteSession mMediaRouteSession;
    public int f;
    public String mSessionId;
    boolean h;
    public boolean i;
    public final FlingMediaRouteProvider mFlingMediaRouteProvider;
    private String mApplicationId;
    private boolean isRelaunchApp;
    private boolean n;
    private RemotePlaybackRequest o;
    private RemotePlaybackRequest p;
    private RemotePlaybackRequest mSyncStatusRequest;
    private PendingIntent mPendingIntent;
    private MediaControlChannel mMediaControlChannel;
    private long mLoadRequestId;
    private boolean u;
    private final List w = new LinkedList();
    private TrackedItem mTrackedItem;

    public FlingRouteController(FlingMediaRouteProvider awb1,
            FlingDevice flingdevice) {
        super();
        mFlingMediaRouteProvider = awb1;
        mApplicationId = (String) FlingMediaRouteProvider.i.b();
        isRelaunchApp = true;
        mFlingDevice = flingdevice;
        c = 0.0D;
        f = 0;
        mLoadRequestId = -1L;
    }

    private static Bundle a(JSONObject jsonobject) {
        if (jsonobject == null) {
            return null;
        }
        Bundle bundle;
        if (!jsonobject.has("httpStatus")) {
            bundle = null;
        } else {
            int httpStatus;
            try {
                httpStatus = jsonobject.getInt("httpStatus");
                bundle = new Bundle();
                bundle.putInt("android.media.status.extra.HTTP_STATUS_CODE",
                        httpStatus);
            } catch (JSONException jsonexception1) {
                bundle = null;
            }
        }
        if (jsonobject.has("httpHeaders")) {
            try {
                Bundle httpHeaders = FlingMediaManagerHelper
                        .getBundle(jsonobject.getJSONObject("httpHeaders"));
                if (bundle == null)
                    bundle = new Bundle();
                bundle.putBundle(
                        "android.media.status.extra.HTTP_RESPONSE_HEADERS",
                        httpHeaders);
            } catch (JSONException jsonexception) {
            }
        }
        return bundle;
    }

    private void a(Intent intent) {
        long l1 = mFlingDeviceController.h();
        Bundle bundle = intent.getExtras();
        if (bundle
                .containsKey("tv.matchstick.fling.EXTRA_DEBUG_LOGGING_ENABLED")) {
            boolean flag = bundle
                    .getBoolean("tv.matchstick.fling.EXTRA_DEBUG_LOGGING_ENABLED");
            if (flag)
                l1 |= 1L;
            else
                l1 &= -2L;
            FlingMediaRouteProvider.getLogs_a().setDebugEnabled(flag);
        }
        mFlingDeviceController.setDebugLevel(l1);
    }

    private void a(TrackedItem aws1) {
        if (mTrackedItem == aws1)
            mTrackedItem = null;
        w.remove(aws1);
    }

    private void sendPlaybackStateForItem(TrackedItem item, int playbackState,
            Bundle bundle) {
        FlingMediaRouteProvider.getLogs_a().d(
                "sendPlaybackStateForItem for item: %s, playbackState: %d",
                item, playbackState);
        if (item.mPendingIntent == null)
            return;
        Intent intent = new Intent();
        intent.putExtra("android.media.intent.extra.ITEM_ID", item.mItemId);
        MediaItemStatusHelper nr1 = (new MediaItemStatusHelper(playbackState))
                .putTimestamp(SystemClock.uptimeMillis());
        if (bundle != null)
            nr1.putExtras(bundle);
        intent.putExtra("android.media.intent.extra.ITEM_STATUS",
                nr1.createMediaItemStatus().mBundle);
        try {
            item.mPendingIntent.send(
                    ((MediaRouteProvider) (mFlingMediaRouteProvider)).mContext,
                    0, intent);
            return;
        } catch (android.app.PendingIntent.CanceledException canceledexception) {
            FlingMediaRouteProvider.getLogs_a().w(canceledexception,
                    "exception while sending PendingIntent");
        }
    }

    private boolean processRemotePlaybackRequest(RemotePlaybackRequest awt1) {
        String albumTitle;
        Integer discNumber;
        Integer trackNumber;
        FlingMediaRouteProvider.getLogs_a().d("processRemotePlaybackRequest()",
                new Object[0]);
        Intent intent = awt1.mIntent;
        String action = intent.getAction();
        Bundle bundle = intent
                .getBundleExtra("tv.matchstick.fling.EXTRA_CUSTOM_DATA");
        JSONObject jsonobject;
        Bundle bundle1;
        Bundle bundle2;
        String appId;
        String s3;
        boolean flag;
        PendingIntent pendingintent;
        FlingChannel avv1;
        Bundle bundle3;
        long itemPosition;
        boolean flag1;
        boolean flag2;
        boolean flag3;
        MediaInfo info;
        Uri uri;
        Bundle bundle4;
        MediaMetadata mediaMetadata;
        String albumArtist;
        String composer;
        String title;
        String artist;
        String artworkUri;
        String contentType;
        Bundle bundle5;
        JSONObject jsonobject1;
        long pos;
        PendingIntent pendingintent1;
        TrackedItem aws1;
        Bundle bundle6;
        JSONObject jsonobject2;
        int year;
        Calendar calendar;
        String s11;
        if (bundle != null)
            jsonobject = FlingMediaManagerHelper.getJsonObject(bundle, null);
        else
            jsonobject = null;
        FlingMediaRouteProvider.getLogs_a().d(
                "got remote playback request; action=%s", action);
        try {
            if (!action.equals("android.media.intent.action.PLAY")
                    || intent.getData() == null) {
                if (action.equals("android.media.intent.action.PAUSE")) {
                    flag3 = checkSession(awt1, 0);
                    if (!flag3)
                        return true;
                    try {
                        mMediaControlChannel.pause(this, jsonobject);
                        // } catch (IOException ioexception4) {
                    } catch (Exception ioexception4) {
                        FlingMediaRouteProvider.getLogs_a().w(ioexception4,
                                "exception while processing %s", action);
                        awt1.onRouteCtrlRequestFailed(1);
                    }
                    return true;
                }

                if (action.equals("android.media.intent.action.RESUME")) {
                    flag2 = checkSession(awt1, 0);
                    if (!flag2)
                        return true;
                    try {
                        mMediaControlChannel.play(this, jsonobject);
                        // } catch (IOException ioexception3) {
                    } catch (Exception ioexception3) {
                        FlingMediaRouteProvider.getLogs_a().w(ioexception3,
                                "exception while processing %s", action);
                        awt1.onRouteCtrlRequestFailed(1);
                    }
                    return true;
                }

                if (action.equals("android.media.intent.action.STOP")) {
                    flag1 = checkSession(awt1, 0);
                    if (!flag1)
                        return true;
                    try {
                        mMediaControlChannel.stop(this, jsonobject);
                        // } catch (IOException ioexception2) {
                    } catch (Exception ioexception2) {
                        FlingMediaRouteProvider.getLogs_a().w(ioexception2,
                                "exception while processing %s", action);
                        awt1.onRouteCtrlRequestFailed(1);
                    }
                    return true;
                }

                if (action.equals("android.media.intent.action.SEEK")) {
                    if (!checkSession(awt1, 0))
                        return true;
                    d(intent.getStringExtra("android.media.intent.extra.ITEM_ID"));
                    itemPosition = intent.getLongExtra(
                            "android.media.intent.extra.ITEM_POSITION", 0L);
                    try {
                        FlingMediaRouteProvider.getLogs_a().d(
                                "seeking to %d ms", itemPosition);
                        mMediaControlChannel.seek(this, itemPosition, 0,
                                jsonobject);
                        // } catch (IOException ioexception1) {
                    } catch (Exception ioexception1) {
                        FlingMediaRouteProvider.getLogs_a().w(ioexception1,
                                "exception while processing %s", action);
                        awt1.onRouteCtrlRequestFailed(1);
                    }
                    return true;
                }

                if (action.equals("android.media.intent.action.GET_STATUS")) {
                    if (!checkSession(awt1, 0))
                        return true;
                    d(intent.getStringExtra("android.media.intent.extra.ITEM_ID"));
                    if (mMediaControlChannel == null) {
                        awt1.onRouteCtrlRequestFailed(2);
                        return true;
                    }
                    bundle3 = new Bundle();
                    bundle3.putParcelable(
                            "android.media.intent.extra.ITEM_STATUS",
                            getItemStatusBundle());
                    bundle3.putParcelable(
                            "android.media.intent.extra.SESSION_STATUS",
                            createSessionStatusBundle(0));
                    awt1.onRouteCtrlRequestOk(bundle3);
                    return true;
                }

                if (action.equals("tv.matchstick.fling.ACTION_SYNC_STATUS")) {
                    if (!checkSession(awt1, 0))
                        return true;
                    avv1 = mMediaControlChannel;
                    if (avv1 == null) {
                        awt1.onRouteCtrlRequestFailed(2);
                        return true;
                    }
                    try {
                        if (mLoadRequestId == -1L)
                            mLoadRequestId = mMediaControlChannel
                                    .requestStatus(this);
                        mSyncStatusRequest = awt1;
                        // } catch (IOException ioexception) {
                    } catch (Exception ioexception) {
                        mSyncStatusRequest = null;
                        FlingMediaRouteProvider.getLogs_a().w(ioexception,
                                "exception while processing %s", action);
                        awt1.onRouteCtrlRequestFailed(1);
                    }
                    return true;
                }

                if (!action.equals("android.media.intent.action.START_SESSION")) {
                    if (action
                            .equals("android.media.intent.action.GET_SESSION_STATUS")) {
                        checkSession(awt1, 0);
                        bundle2 = new Bundle();
                        bundle2.putParcelable(
                                "android.media.intent.extra.SESSION_STATUS",
                                createSessionStatusBundle(0));
                        awt1.onRouteCtrlRequestOk(bundle2);
                        return true;
                    }
                    if (action
                            .equals("android.media.intent.action.END_SESSION")) {
                        checkSession(awt1, 0);
                        sendPendingIntent(getSessionId(), 1);
                        mPendingIntent = null;
                        endSession();
                        bundle1 = new Bundle();
                        bundle1.putParcelable(
                                "android.media.intent.extra.SESSION_STATUS",
                                createSessionStatusBundle(1));
                        awt1.onRouteCtrlRequestOk(bundle1);
                        return true;
                    }
                    return false;

                }
                appId = intent
                        .getStringExtra("tv.matchstick.fling.EXTRA_FLING_APPLICATION_ID");
                if (!TextUtils.isEmpty(appId)) {
                    s3 = appId;

                } else {
                    s3 = (String) FlingMediaRouteProvider.i.b();
                }
                flag = intent.getBooleanExtra(
                        "tv.matchstick.fling.EXTRA_FLING_RELAUNCH_APPLICATION",
                        true);
                n = intent
                        .getBooleanExtra(
                                "tv.matchstick.fling.EXTRA_FLING_STOP_APPLICATION_WHEN_SESSION_ENDS",
                                false);
                pendingintent = (PendingIntent) intent
                        .getParcelableExtra("android.media.intent.extra.SESSION_STATUS_UPDATE_RECEIVER");
                if (pendingintent == null) {
                    FlingMediaRouteProvider.getLogs_a().d(
                            "No status update receiver supplied to %s",
                            new Object[] { action });
                    return false;
                }
                a(intent);
                mPendingIntent = pendingintent;
                mApplicationId = s3;
                isRelaunchApp = flag;
                p = awt1;
                startSession(0);
                return true;

            }
            if (intent.getStringExtra("android.media.intent.extra.SESSION_ID") == null) {
                s11 = intent
                        .getStringExtra("tv.matchstick.fling.EXTRA_FLING_APPLICATION_ID");
                if (TextUtils.isEmpty(s11))
                    s11 = (String) FlingMediaRouteProvider.i.b();
                mApplicationId = s11;
            }
            if (!checkSession(awt1, 1))
                return true;
            uri = intent.getData();
            if (uri == null)
                return false;
            FlingMediaRouteProvider.getLogs_a().d(
                    "Device received play request, uri %s", uri);
            a(intent);
            bundle4 = intent
                    .getBundleExtra("android.media.intent.extra.ITEM_METADATA");
            mediaMetadata = null;
            if (bundle4 != null) {
                albumTitle = bundle4
                        .getString("android.media.metadata.ALBUM_TITLE");
                albumArtist = bundle4
                        .getString("android.media.metadata.ALBUM_ARTIST");
                composer = bundle4.getString("android.media.metadata.COMPOSER");
                if (!bundle4.containsKey("android.media.metadata.DISC_NUMBER")) {
                    discNumber = null;

                } else {
                    discNumber = Integer.valueOf(bundle4
                            .getInt("android.media.metadata.DISC_NUMBER"));
                }
                if (!bundle4.containsKey("android.media.metadata.TRACK_NUMBER")) {
                    trackNumber = null;

                } else {
                    trackNumber = Integer.valueOf(bundle4
                            .getInt("android.media.metadata.TRACK_NUMBER"));
                }
                if (albumTitle == null && discNumber == null
                        && trackNumber == null) {
                    mediaMetadata = new MediaMetadata(0);

                } else {
                    mediaMetadata = new MediaMetadata(3);
                    if (albumTitle != null)
                        mediaMetadata.putString(
                                "tv.matchstick.fling.metadata.ALBUM_TITLE",
                                albumTitle);
                    if (albumArtist != null)
                        mediaMetadata.putString(
                                "tv.matchstick.fling.metadata.ALBUM_ARTIST",
                                albumArtist);
                    if (composer != null)
                        mediaMetadata.putString(
                                "tv.matchstick.fling.metadata.COMPOSER",
                                composer);
                    if (discNumber != null)
                        mediaMetadata.putInt(
                                "tv.matchstick.fling.metadata.DISC_NUMBER",
                                discNumber.intValue());
                    if (trackNumber != null)
                        mediaMetadata.putInt(
                                "tv.matchstick.fling.metadata.TRACK_NUMBER",
                                trackNumber.intValue());
                }
                title = bundle4.getString("android.media.metadata.TITLE");
                if (title != null)
                    mediaMetadata.putString(
                            "tv.matchstick.fling.metadata.TITLE", title);
                artist = bundle4.getString("android.media.metadata.ARTIST");
                if (artist != null)
                    mediaMetadata.putString(
                            "tv.matchstick.fling.metadata.ARTIST", artist);
                if (bundle4.containsKey("android.media.metadata.YEAR")) {
                    year = bundle4.getInt("android.media.metadata.YEAR");
                    calendar = Calendar.getInstance();
                    calendar.set(1, year);
                    mediaMetadata.putDate(
                            "tv.matchstick.fling.metadata.RELEASE_DATE",
                            calendar);
                }
                if (bundle4.containsKey("android.media.metadata.ARTWORK_URI")) {
                    artworkUri = bundle4
                            .getString("android.media.metadata.ARTWORK_URI");
                    if (!TextUtils.isEmpty(artworkUri))
                        mediaMetadata.addImage(new WebImage(Uri
                                .parse(artworkUri)));
                }

            }

            contentType = intent.getType();
            if (!TextUtils.isEmpty(contentType)) {
                info = new MediaInfo.Builder(uri.toString())
                        .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                        .setContentType(contentType).setMetadata(mediaMetadata)
                        .build();
                bundle5 = intent
                        .getBundleExtra("android.media.intent.extra.HTTP_HEADERS");
                if (bundle5 == null) {
                    jsonobject1 = jsonobject;

                } else {
                    try {
                        jsonobject2 = FlingMediaManagerHelper.getJsonObject(
                                bundle5, null);
                        if (jsonobject == null) {
                            jsonobject = new JSONObject();
                        }
                        jsonobject.put("httpHeaders", jsonobject2);
                    } catch (JSONException e) {
                    }
                    jsonobject1 = jsonobject;
                }
                pos = intent.getLongExtra(
                        "android.media.intent.extra.ITEM_POSITION", 0L);
                pendingintent1 = (PendingIntent) intent
                        .getParcelableExtra("android.media.intent.extra.ITEM_STATUS_UPDATE_RECEIVER");
                try {
                    aws1 = new TrackedItem(this, mMediaControlChannel.load(
                            this, info, true, pos, jsonobject1));
                    aws1.mPendingIntent = pendingintent1;
                    w.add(aws1);

                    FlingMediaRouteProvider.getLogs_a().d(
                            "loading media with item id assigned as %s",
                            aws1.mItemId);
                    bundle6 = new Bundle();
                    bundle6.putString("android.media.intent.extra.SESSION_ID",
                            getSessionId());
                    bundle6.putParcelable(
                            "android.media.intent.extra.SESSION_STATUS",
                            createSessionStatusBundle(0));
                    bundle6.putString("android.media.intent.extra.ITEM_ID",
                            aws1.mItemId);
                    bundle6.putBundle(
                            "android.media.intent.extra.ITEM_STATUS",
                            (new MediaItemStatusHelper(3)).putTimestamp(
                                    SystemClock.uptimeMillis())
                                    .createMediaItemStatus().mBundle);
                    awt1.onRouteCtrlRequestOk(bundle6);
                    // } catch (IOException ioexception5) {
                } catch (Exception ioexception5) {
                    FlingMediaRouteProvider.getLogs_a().w(ioexception5,
                            "exception while processing %s", action);
                    awt1.onRouteCtrlRequestFailed(1);
                }
                return true;

            }
            throw new IllegalArgumentException(
                    "content type cannot be null or empty");
        } catch (IllegalStateException e) {
            FlingMediaRouteProvider.getLogs_a().d("can't process command; %s",
                    e.getMessage());
            return false;
        }
    }

    private boolean checkSession(RemotePlaybackRequest awt1, int i1) {
        String sessionId = awt1.mIntent
                .getStringExtra("android.media.intent.extra.SESSION_ID");
        String currentSessionId = getSessionId();
        FlingMediaRouteProvider.getLogs_a().d(
                "checkSession() sessionId=%s, currentSessionId=%s",
                new Object[] { sessionId, currentSessionId });
        if (TextUtils.isEmpty(sessionId)) {
            if (h && currentSessionId != null) {
                h = false;
                return true;
            }
            if (i1 == 1) {
                o = awt1;
                h = true;
                if (mFlingDeviceController.isConnected()) {
                    startSession(0);
                } else {
                    f = 2;
                    mSessionId = null;
                }
                return false;
            }
        } else {
            if (sessionId.equals(currentSessionId)) {
                h = false;
                return true;
            }
            if (currentSessionId == null) {
                o = awt1;
                if (mFlingDeviceController.isConnected()) {
                    resumeSession(sessionId);
                } else {
                    f = 2;
                    mSessionId = sessionId;
                }
                return false;
            }
        }
        awt1.onRouteCtrlRequestFailed(2);
        return false;
    }

    private void resumeSession(String sessionId) {
        FlingMediaRouteProvider.getLogs_a().d("resumeSession()", new Object[0]);
        mMediaRouteSession.joinApplication(mApplicationId, sessionId);
    }

    private void d(String itemId) {
        if (mTrackedItem == null)
            throw new IllegalStateException("no current item");
        if (!mTrackedItem.mItemId.equals(itemId))
            throw new IllegalStateException(
                    "item ID does not match current item");
        else
            return;
    }

    private void startSession(int i1) {
        FlingMediaRouteProvider.getLogs_a().d("startSession()");
        mMediaRouteSession.startSession(mApplicationId, null, isRelaunchApp);
    }

    private void f(int i1) {
        for (Iterator iterator = w.iterator(); iterator.hasNext(); sendPlaybackStateForItem(
                (TrackedItem) iterator.next(), i1, ((Bundle) (null))))
            ;
        w.clear();
        mTrackedItem = null;
    }

    private static final class MediaSessionStatus {

        public final Bundle mData;

        private MediaSessionStatus(Bundle bundle) {
            mData = bundle;
        }

        public MediaSessionStatus(Bundle bundle, byte byte0) {
            this(bundle);
        }

        public final String toString() {
            StringBuilder stringbuilder;
            StringBuilder stringbuilder1;
            int i;
            stringbuilder = new StringBuilder();
            stringbuilder.append("MediaSessionStatus{ ");
            stringbuilder.append("timestamp=");
            C_dt.a(SystemClock.elapsedRealtime() - mData.getLong("timestamp"),
                    stringbuilder);
            stringbuilder.append(" ms ago");
            stringbuilder1 = stringbuilder.append(", sessionState=");
            i = mData.getInt("sessionState", 2);
            String s;
            switch (i) {
            case 0:
                s = "active";
                break;
            case 1:
                s = "ended";
                break;
            case 2:
                s = "invalidated";
                break;
            default:
                s = Integer.toString(i);
            }
            stringbuilder1.append(s);
            stringbuilder.append(", queuePaused=").append(
                    mData.getBoolean("queuePaused"));
            stringbuilder.append(", extras=").append(mData.getBundle("extras"));
            stringbuilder.append(" }");
            return stringbuilder.toString();
        }
    }

    private static class SessionStatusBundle {

        public final Bundle data = new Bundle();

        public SessionStatusBundle(int sessionState) {
            setTimestamp(SystemClock.elapsedRealtime());
            data.putInt("sessionState", sessionState);
        }

        public final SessionStatusBundle setTimestamp(long timestamp) {
            data.putLong("timestamp", timestamp);
            return this;
        }
    }

    private Bundle createSessionStatusBundle(int sessionState) {
        SessionStatusBundle bundle = new SessionStatusBundle(sessionState);
        MediaStatus status;

        boolean queuePaused;
        if (mMediaControlChannel != null
                && (status = mMediaControlChannel.getMediaStatus()) != null) {
            if (status.getPlayerState() == MediaStatus.PLAYER_STATE_PAUSED)
                queuePaused = true;
            else
                queuePaused = false;
        } else {
            queuePaused = false;
        }
        bundle.data.putBoolean("queuePaused", queuePaused);
        return (new MediaSessionStatus(bundle.setTimestamp(SystemClock
                .uptimeMillis()).data, (byte) 0)).mData;
    }

    private Bundle getItemStatusBundle() {
        int playbackState = 5;
        MediaStatus mediaStatus = mMediaControlChannel.getMediaStatus();
        if (mediaStatus != null) {
            int playerState = mediaStatus.getPlayerState();
            int reason = mediaStatus.getIdleReason();
            switch (playerState) {
            case MediaStatus.PLAYER_STATE_IDLE:
                switch (reason) {
                case MediaStatus.IDLE_REASON_ERROR:
                    playbackState = 7;
                    break;
                case MediaStatus.IDLE_REASON_FINISHED:
                    playbackState = 4;
                    break;
                case MediaStatus.IDLE_REASON_INTERRUPTED:
                    playbackState = 6;
                    break;
                case MediaStatus.IDLE_REASON_CANCELED:
                    break;
                default:
                    playbackState = 7;
                    break;
                }
                break;
            case MediaStatus.PLAYER_STATE_PLAYING:
                playbackState = 1;
                break;
            case MediaStatus.PLAYER_STATE_PAUSED:
                playbackState = 2;
                break;
            case MediaStatus.PLAYER_STATE_BUFFERING:
                playbackState = 3;
                break;
            default:
                playbackState = 7;
                break;
            }
            MediaItemStatusHelper helper = new MediaItemStatusHelper(
                    playbackState)
                    .putContentDuration(
                            mMediaControlChannel.getStreamDuration())
                    .putContentPosition(
                            mMediaControlChannel.getStreamDuration())
                    .putTimestamp(SystemClock.uptimeMillis());
            Bundle bundle = a(mediaStatus.getCustomData());
            if (bundle != null)
                helper.putExtras(bundle);
            return helper.createMediaItemStatus().mBundle;
        }
        FlingMediaRouteProvider.getLogs_a().d("*** media status is null!");
        return (new MediaItemStatusHelper(playbackState))
                .createMediaItemStatus().mBundle;
    }

    public final void onRelease() {
        FlingMediaRouteProvider.getLogs_a().d("Controller released",
                new Object[0]);
    }

    public final void onSetVolume(int volume) {
        FlingMediaRouteProvider.getLogs_a()
                .d("onSetVolume() volume=%d", volume);
        if (mFlingDeviceController == null) {
            return;
        }
        double d1 = (double) volume / 20D;
        try {
            mFlingDeviceController.setVolume(d1, c, false);
        } catch (IllegalStateException e) {
            FlingMediaRouteProvider.getLogs_a().d("Unable to set volume: %s",
                    e.getMessage());
        }
    }

    @Override
    public final void onTrackRequest(long loadRequestId, int status,
            JSONObject jsonobject) {
        Iterator iterator = w.iterator();
        TrackedItem aws1;
        do {
            if (!iterator.hasNext()) {
                aws1 = null;
                break;
            }
            aws1 = (TrackedItem) iterator.next();
        } while (aws1.mLoadRequestId != loadRequestId);
        if (aws1 == null) {
            if (loadRequestId != mLoadRequestId) {
                return;
            }
            FlingMediaRouteProvider.getLogs_a()
                    .d("requestStatus has completed");
            mLoadRequestId = -1L;
            long sessionId;
            Iterator iterator1;
            try {
                sessionId = mMediaControlChannel.getMediaSessionId();
                iterator1 = w.iterator();
                TrackedItem aws2;
                do {
                    if (!iterator1.hasNext()) {
                        aws2 = null;
                        break;
                    }
                    aws2 = (TrackedItem) iterator1.next();
                } while (aws2.mMediaSessionId != sessionId);
                if (mTrackedItem != null && mTrackedItem != aws2) {
                    sendPlaybackStateForItem(mTrackedItem, 4, ((Bundle) (null)));
                    a(mTrackedItem);
                }
                if (mSyncStatusRequest != null) {
                    TrackedItem aws3 = new TrackedItem(this);
                    aws3.mMediaSessionId = sessionId;
                    aws3.mPendingIntent = (PendingIntent) mSyncStatusRequest.mIntent
                            .getParcelableExtra("android.media.intent.extra.ITEM_STATUS_UPDATE_RECEIVER");
                    w.add(aws3);
                    mTrackedItem = aws3;
                }
                Iterator iterator2 = w.iterator();
                while (iterator2.hasNext()) {
                    TrackedItem aws4 = (TrackedItem) iterator2.next();
                    if (aws4.mMediaSessionId != -1L
                            && (mTrackedItem == null || aws4.mMediaSessionId < mTrackedItem.mMediaSessionId)) {
                        sendPlaybackStateForItem(aws4, 4, ((Bundle) (null)));
                        iterator2.remove();
                    }
                }
            } catch (IllegalStateException illegalstateexception) {
                f(4);
                mTrackedItem = null;
            }
            FlingMediaRouteProvider.getLogs_a().d(
                    "mSyncStatusRequest = %s, status=%d", status);
            if (mSyncStatusRequest != null) {
                if (status == 0) {
                    FlingMediaRouteProvider.getLogs_a().d(
                            "requestStatus completed; sending response");
                    Bundle bundle = new Bundle();
                    if (mTrackedItem != null) {
                        MediaStatus aud1 = mMediaControlChannel
                                .getMediaStatus();
                        bundle.putString("android.media.intent.extra.ITEM_ID",
                                mTrackedItem.mItemId);
                        bundle.putParcelable(
                                "android.media.intent.extra.ITEM_STATUS",
                                getItemStatusBundle());
                        MediaInfo atz1 = aud1.getMediaInfo();
                        if (atz1 != null) {
                            Bundle bundle1 = FlingMediaManagerHelper
                                    .createMetadataBundle(atz1);
                            FlingMediaRouteProvider.getLogs_a().d(
                                    "adding metadata bundle: %s",
                                    new Object[] { bundle1 });
                            bundle.putParcelable(
                                    "android.media.intent.extra.ITEM_METADATA",
                                    bundle1);
                        }
                    }
                    mSyncStatusRequest.onRouteCtrlRequestOk(bundle);
                } else {
                    mSyncStatusRequest.onRouteCtrlRequestFailed(1);
                }
                mSyncStatusRequest = null;
            }

            return;
        }
        long mediaSessionId = mMediaControlChannel.getMediaSessionId();
        switch (status) {
        default:
        case 1:
            FlingMediaRouteProvider.getLogs_a().d(
                    "unknown status %d; sending error state", status);
            sendPlaybackStateForItem(aws1, 7, a(jsonobject));
            a(aws1);
            break;
        case 0:
            FlingMediaRouteProvider.getLogs_a().d(
                    "Load completed; mediaSessionId=%d", mediaSessionId);
            aws1.mLoadRequestId = -1L;
            aws1.mMediaSessionId = mediaSessionId;
            mTrackedItem = aws1;
            sendItemStatusUpdate();
            break;
        case 2:
            FlingMediaRouteProvider.getLogs_a().d(
                    "STATUS_CANCELED; sending error state");
            sendPlaybackStateForItem(aws1, 5, ((Bundle) (null)));
            a(aws1);
            break;
        case 3:
            FlingMediaRouteProvider.getLogs_a().d(
                    "STATUS_TIMED_OUT; sending error state");
            sendPlaybackStateForItem(aws1, 7, ((Bundle) (null)));
            a(aws1);
            break;
        }
    }

    public final void attachMediaChannel(String sessionId) {
        ApplicationMetadata applicationmetadata;
        applicationmetadata = mMediaRouteSession.getApplicationMetadata();
        if (p != null) {
            Bundle bundle = new Bundle();
            bundle.putString("android.media.intent.extra.SESSION_ID", sessionId);
            p.onRouteCtrlRequestOk(bundle);
            p = null;
        }
        sendPendingIntent(sessionId, 0);
        if (mApplicationId.equals(applicationmetadata.getApplicationId())) {
            FlingMediaRouteProvider.getLogs_a().d("attachMediaChannel",
                    new Object[0]);

            // mMediaControlChannel_s = new C_awo(this);
            mMediaControlChannel = new MediaControlChannel() {
                @Override
                protected final void onStatusUpdated() {
                    FlingMediaRouteProvider.getLogs_a().d("onStatusUpdated");
                    sendItemStatusUpdate();
                }
                @Override
                protected final void onMetadataUpdated() {
                    sendItemStatusUpdate();
                }
            };

            mFlingDeviceController.a(mMediaControlChannel);
            if (o != null) {
                processRemotePlaybackRequest(o);
                o = null;
            }
        }
        if (mLoadRequestId != -1L || mMediaControlChannel == null)
            return;
        try {
            mLoadRequestId = mMediaControlChannel.requestStatus(this);
        } catch (Exception ioexception1) {
            FlingMediaRouteProvider.getLogs_a().w(ioexception1,
                    "Exception while requesting media status");

        }
    }

    final void sendPendingIntent(String sessionId, int sessionStatus) {
        if (sessionId == null || mPendingIntent == null)
            return;
        Intent intent = new Intent();
        intent.putExtra("android.media.intent.extra.SESSION_ID", sessionId);
        intent.putExtra("android.media.intent.extra.SESSION_STATUS",
                createSessionStatusBundle(sessionStatus));
        try {
            FlingMediaRouteProvider.getLogs_a().d(
                    "Invoking session status PendingIntent with: %s", intent);
            mPendingIntent.send(
                    ((MediaRouteProvider) (mFlingMediaRouteProvider)).mContext,
                    0, intent);
            return;
        } catch (android.app.PendingIntent.CanceledException canceledexception) {
            FlingMediaRouteProvider.getLogs_a().w(canceledexception,
                    "exception while sending PendingIntent");
        }
    }

    public final boolean onControlRequest(Intent intent,
            RouteCtrlRequestCallback om) {
        boolean flag;
        FlingMediaRouteProvider.getLogs_a().d("Received control request %s",
                intent);
        RemotePlaybackRequest awt1 = new RemotePlaybackRequest(
                mFlingMediaRouteProvider, intent, om);
        if (!intent
                .hasCategory("android.media.intent.category.REMOTE_PLAYBACK")) {
            boolean flag1 = intent
                    .hasCategory("tv.matchstick.fling.CATEGORY_FLING_REMOTE_PLAYBACK");
            flag = false;
            if (!flag1)
                return flag;
        }
        flag = processRemotePlaybackRequest(awt1);
        return flag;
    }

    public final void onSelect() {
        FlingMediaRouteProvider.getLogs_a().d("onSelect");
        mFlingDeviceController = FlingMediaRouteProvider
                .createDeviceController(mFlingMediaRouteProvider, this);
        mMediaRouteSession = new MediaRouteSession(mFlingDeviceController,
                this,
                ((MediaRouteProvider) (mFlingMediaRouteProvider)).mHandler);
    }

    public final void onUpdateVolume(int delta) {
        FlingMediaRouteProvider.getLogs_a().d("onUpdateVolume() delta=%d",
                delta);
        if (mFlingDeviceController == null)
            return;
        try {
            double d1 = c + (double) delta / 20D;
            mFlingDeviceController.setVolume(d1, c, false);
            return;
        } catch (IllegalStateException illegalstateexception) {
            FlingMediaRouteProvider.getLogs_a().d(
                    "Unable to update volume: %s",
                    illegalstateexception.getMessage());
            return;
        }
    }

    public final void sendPendingIntent(String sessionId) {
        if (p != null) {
            p.onRouteCtrlRequestFailed(2);
            p = null;
        }
        sendPendingIntent(sessionId, 1);
    }

    public final void onUnselect() {
        FlingMediaRouteProvider.getLogs_a().d("onUnselect");
        endSession();
        FlingMediaRouteProvider.b(mFlingMediaRouteProvider, this);
        mFlingDeviceController = null;
    }

    public final void onApplicationDisconnected(int statusCode) {
        FlingMediaRouteProvider.getLogs_a().d(
                "onApplicationDisconnected: statusCode=%d", statusCode);
        if (mMediaRouteSession != null) {
            mMediaRouteSession.onApplicationDisconnected(statusCode);
            sendPendingIntent(getSessionId(), 1);
        }
    }

    public final void endSession() {
        FlingMediaRouteProvider.getLogs_a()
                .d("endSession() voluntary=%b", true);
        if (mMediaRouteSession != null) {
            mMediaRouteSession.stopSession(u | n);
            u = false;
            n = false;
        }
    }

    public final void detachMediaChannel(int i1) {
        boolean flag;
        byte byte0;
        if (i1 == 0)
            flag = true;
        else
            flag = false;
        if (flag)
            byte0 = 5;
        else
            byte0 = 6;
        f(byte0);
        if (!i) {
            FlingMediaRouteProvider.getLogs_a().d("detachMediaChannel");
            if (mMediaControlChannel != null) {
                if (mFlingDeviceController != null)
                    mFlingDeviceController.b(mMediaControlChannel);
                mMediaControlChannel = null;
            }
        }
        p = null;
    }

    final String getSessionId() {
        if (mMediaRouteSession == null)
            return null;
        else
            return mMediaRouteSession.getSessionId();
    }

    public final void startSession() {
        if (f == 2) {
            FlingMediaRouteProvider.getLogs_a().d(
                    "starting pending session for media with session ID %s",
                    mSessionId);
            if (mSessionId != null) {
                resumeSession(mSessionId);
                mSessionId = null;
            } else {
                startSession(0);
            }
        }
        f = 0;
    }

    public final void g() {
        FlingMediaRouteProvider.b(mFlingMediaRouteProvider, this);
    }

    final void sendItemStatusUpdate() {
        FlingMediaRouteProvider.getLogs_a().d(
                "sendItemStatusUpdate(); current item is %s", mTrackedItem);
        if (mTrackedItem != null) {
            PendingIntent pendingintent = mTrackedItem.mPendingIntent;
            if (pendingintent != null) {
                FlingMediaRouteProvider.getLogs_a().d(
                        "found a PendingIntent for item %s", mTrackedItem);
                Intent intent = new Intent();
                intent.putExtra("android.media.intent.extra.ITEM_ID",
                        mTrackedItem.mItemId);
                intent.putExtra("android.media.intent.extra.ITEM_STATUS",
                        getItemStatusBundle());
                MediaInfo atz1 = mMediaControlChannel.getMediaInfo();
                if (atz1 != null) {
                    Bundle bundle = FlingMediaManagerHelper
                            .createMetadataBundle(atz1);
                    FlingMediaRouteProvider.getLogs_a().d(
                            "adding metadata bundle: %s", bundle.toString());
                    intent.putExtra("android.media.intent.extra.ITEM_METADATA",
                            bundle);
                }
                try {
                    FlingMediaRouteProvider.getLogs_a().d(
                            "Invoking item status PendingIntent with: %s",
                            intent);
                    pendingintent
                            .send(((MediaRouteProvider) (mFlingMediaRouteProvider)).mContext,
                                    0, intent);
                } catch (android.app.PendingIntent.CanceledException canceledexception) {
                    FlingMediaRouteProvider.getLogs_a().d(canceledexception,
                            "exception while sending PendingIntent",
                            new Object[0]);
                }
            }

            if (mMediaControlChannel.getMediaStatus().getPlayerState() == MediaStatus.PLAYER_STATE_IDLE) {
                FlingMediaRouteProvider.getLogs_a().d(
                        "player state is now IDLE; removing tracked item %s",
                        mTrackedItem);
                a(mTrackedItem);
            }
        }
    }

    @Override
    public void onSignInRequired(long requestId) {
        
    }
}
