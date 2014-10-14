package tv.matchstick.server.fling.media;

import android.os.Bundle;
import android.os.SystemClock;

public final class MediaItemStatusHelper {
    private final Bundle mData = new Bundle();

    public MediaItemStatusHelper(int playbackState) {
        putTimestamp(SystemClock.elapsedRealtime());
        mData.putInt("playbackState", playbackState);
    }

    public final MediaItemStatus createMediaItemStatus() {
        return new MediaItemStatus(mData, (byte) 0);
    }

    public final MediaItemStatusHelper putTimestamp(long timestamp) {
        mData.putLong("timestamp", timestamp);
        return this;
    }

    public final MediaItemStatusHelper putExtras(Bundle extras) {
        mData.putBundle("extras", extras);
        return this;
    }
    
    public final MediaItemStatusHelper putContentDuration(long contentDuration) {
        mData.putLong("contentDuration", contentDuration);
        return this;
    }
    
    public final MediaItemStatusHelper putContentPosition(long contentPosition) {
        mData.putLong("contentPosition", contentPosition);
        return this;
    }
}
