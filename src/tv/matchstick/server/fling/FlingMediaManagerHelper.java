package tv.matchstick.server.fling;

import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

import tv.matchstick.fling.MediaInfo;
import tv.matchstick.fling.MediaMetadata;
import tv.matchstick.fling.images.WebImage;

import java.util.Calendar;
import java.util.Iterator;
import java.util.Set;

final class FlingMediaManagerHelper {
    static Bundle createMetadataBundle(MediaInfo mediaInfo) {
        Bundle bundle = new Bundle();
        bundle.putLong("android.media.metadata.DURATION", mediaInfo.getStreamDuration());
        MediaMetadata mediaMetadata = mediaInfo.getMetadata();
        String albumArtist = mediaMetadata
                .getString("tv.matchstick.fling.metadata.ALBUM_ARTIST");
        if (albumArtist != null)
            bundle.putString("android.media.metadata.ALBUM_ARTIST", albumArtist);
        String albumTitle = mediaMetadata
                .getString("tv.matchstick.fling.metadata.ALBUM_TITLE");
        if (albumTitle != null)
            bundle.putString("android.media.metadata.ALBUM_TITLE", albumTitle);
        String artist = mediaMetadata
                .getString("tv.matchstick.fling.metadata.ARTIST");
        if (artist == null)
            artist = mediaMetadata
                    .getString("tv.matchstick.fling.metadata.STUDIO");
        if (artist != null)
            bundle.putString("android.media.metadata.ARTIST", artist);
        String composer = mediaMetadata
                .getString("tv.matchstick.fling.metadata.COMPOSER");
        if (composer != null)
            bundle.putString("android.media.metadata.COMPOSER", composer);
        if (mediaMetadata
                .containsKey("tv.matchstick.fling.metadata.DISC_NUMBER"))
            bundle.putInt("android.media.metadata.DISC_NUMBER", mediaMetadata
                    .getInt("tv.matchstick.fling.metadata.DISC_NUMBER"));
        String title = mediaMetadata
                .getString("tv.matchstick.fling.metadata.TITLE");
        if (title != null)
            bundle.putString("android.media.metadata.TITLE", title);
        if (mediaMetadata
                .containsKey("tv.matchstick.fling.metadata.TRACK_NUMBER"))
            bundle.putInt("android.media.metadata.TRACK_NUMBER", mediaMetadata
                    .getInt("tv.matchstick.fling.metadata.TRACK_NUMBER"));
        Calendar calendar = mediaMetadata
                .getDate("tv.matchstick.fling.metadata.BROADCAST_DATE");
        if (calendar == null)
            calendar = mediaMetadata
                    .getDate("tv.matchstick.fling.metadata.RELEASE_DATE");
        if (calendar == null)
            calendar = mediaMetadata
                    .getDate("tv.matchstick.fling.metadata.CREATION_DATE");
        if (calendar != null)
            bundle.putInt("android.media.metadata.YEAR", calendar.get(1));
        if (mediaMetadata.hasImages())
            bundle.putString("android.media.metadata.ARTWORK_URI",
                    ((WebImage) mediaMetadata.getImages().get(0)).getUrl()
                            .toString());
        return bundle;
    }

    static Bundle getBundle(JSONObject jsonobject) {
        Bundle bundle;
        Iterator iterator;
        bundle = new Bundle();
        iterator = jsonobject.keys();

        while (iterator.hasNext()) {
            try {
                String key = (String) iterator.next();;
                Object value = jsonobject.get(key);
                if (value == JSONObject.NULL) {
                    bundle.putParcelable(key, null);
                } else if (value instanceof String) {
                    bundle.putString(key, (String) value);
                } else if (value instanceof Boolean) {
                    bundle.putBoolean(key, ((Boolean) value).booleanValue());
                } else if (value instanceof Integer) {
                    bundle.putInt(key, ((Integer) value).intValue());
                } else if (value instanceof Long) {
                    bundle.putLong(key, ((Long) value).longValue());
                } else if (value instanceof Double) {
                    bundle.putDouble(key, ((Double) value).doubleValue());
                } else if (value instanceof JSONObject)
                    bundle.putBundle(key, getBundle((JSONObject) value));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return bundle;
    }

    static JSONObject getJsonObject(Bundle bundle, Set set) {
        JSONObject jsonobject;
        Iterator iterator;
        jsonobject = new JSONObject();
        iterator = bundle.keySet().iterator();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            if (set != null && set.contains(key)) {
                continue;
            }
            Object obj = bundle.get(key);
            if (!(obj instanceof Bundle)) {
                try {
                    jsonobject.put(key, obj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    jsonobject.put(key, getJsonObject((Bundle) obj, set));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        return jsonobject;
    }
}
