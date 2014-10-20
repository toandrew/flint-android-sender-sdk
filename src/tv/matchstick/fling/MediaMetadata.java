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

package tv.matchstick.fling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import tv.matchstick.client.internal.MetadataUtils;
import tv.matchstick.fling.images.WebImage;
import android.os.Bundle;
import android.text.TextUtils;

/**
 * Media meta data.
 * 
 * Metadata has a media type, an optional list of images, and a collection of
 * metadata fields. Keys for common metadata fields are predefined as constants,
 * but the application is free to define and use additional fields of its own.
 * <p>
 * The values of the predefined fields have predefined types. For example, a
 * track number is an int and a creation date is a String containing an ISO-8601
 * representation of a date and time. Attempting to store a value of an
 * incorrect type in a field will result in a IllegalArgumentException.
 * <p>
 * Note that the Fling protocol limits which metadata fields can be used for a
 * given media type. When a MediaMetadata object is serialized to JSON for
 * delivery to a Fling receiver, any predefined fields which are not supported
 * for a given media type will not be included in the serialized form, but any
 * application-defined fields will always be included.
 */
public class MediaMetadata {
	/**
	 * Generic media
	 */
	public static final int MEDIA_TYPE_GENERIC = 0;

	/**
	 * Movie
	 */
	public static final int MEDIA_TYPE_MOVIE = 1;

	/**
	 * TV SHOW
	 */
	public static final int MEDIA_TYPE_TV_SHOW = 2;

	/**
	 * Music
	 */
	public static final int MEDIA_TYPE_MUSIC_TRACK = 3;

	/**
	 * Photo
	 */
	public static final int MEDIA_TYPE_PHOTO = 4;

	/**
	 * User defined
	 */
	public static final int MEDIA_TYPE_USER = 100;

	/**
	 * null
	 */
	static final int VALUE_TYPE_NULL = 0;

	/**
	 * string
	 */
	static final int VALUE_TYPE_STRING = 1;

	/**
	 * int
	 */
	static final int VALUE_TYPE_INT = 2;

	/**
	 * double
	 */
	static final int VALUE_TYPE_DOUBLE = 3;

	/**
	 * ISO_8601_STRING
	 */
	static final int VALUE_TYPE_ISO_8601_STRING = 4;

	/**
	 * values map
	 */
	private static final String[] VALUE_TYPES = { null, "String", "int",
			"double", "ISO-8601 date String" };

	/**
	 * Value checker
	 */
	private static final ValueTypesChecker mChecker = new ValueTypesChecker()
			.addKeyValue("tv.matchstick.fling.metadata.CREATION_DATE",
					"creationDateTime", VALUE_TYPE_ISO_8601_STRING)
			.addKeyValue("tv.matchstick.fling.metadata.RELEASE_DATE",
					"releaseDate", VALUE_TYPE_ISO_8601_STRING)
			.addKeyValue("tv.matchstick.fling.metadata.BROADCAST_DATE",
					"originalAirdate", VALUE_TYPE_ISO_8601_STRING)
			.addKeyValue("tv.matchstick.fling.metadata.TITLE", "title",
					VALUE_TYPE_STRING)
			.addKeyValue("tv.matchstick.fling.metadata.SUBTITLE", "subtitle",
					VALUE_TYPE_STRING)
			.addKeyValue("tv.matchstick.fling.metadata.ARTIST", "artist",
					VALUE_TYPE_STRING)
			.addKeyValue("tv.matchstick.fling.metadata.ALBUM_ARTIST",
					"albumArtist", VALUE_TYPE_STRING)
			.addKeyValue("tv.matchstick.fling.metadata.ALBUM_TITLE",
					"albumName", VALUE_TYPE_STRING)
			.addKeyValue("tv.matchstick.fling.metadata.COMPOSER", "composer",
					VALUE_TYPE_STRING)
			.addKeyValue("tv.matchstick.fling.metadata.DISC_NUMBER",
					"discNumber", VALUE_TYPE_INT)
			.addKeyValue("tv.matchstick.fling.metadata.TRACK_NUMBER",
					"trackNumber", VALUE_TYPE_INT)
			.addKeyValue("tv.matchstick.fling.metadata.SEASON_NUMBER",
					"season", VALUE_TYPE_INT)
			.addKeyValue("tv.matchstick.fling.metadata.EPISODE_NUMBER",
					"episode", VALUE_TYPE_INT)
			.addKeyValue("tv.matchstick.fling.metadata.SERIES_TITLE",
					"seriesTitle", VALUE_TYPE_STRING)
			.addKeyValue("tv.matchstick.fling.metadata.STUDIO", "studio",
					VALUE_TYPE_STRING)
			.addKeyValue("tv.matchstick.fling.metadata.WIDTH", "width",
					VALUE_TYPE_INT)
			.addKeyValue("tv.matchstick.fling.metadata.HEIGHT", "height",
					VALUE_TYPE_INT)
			.addKeyValue("tv.matchstick.fling.metadata.LOCATION_NAME",
					"location", VALUE_TYPE_STRING)
			.addKeyValue("tv.matchstick.fling.metadata.LOCATION_LATITUDE",
					"latitude", VALUE_TYPE_DOUBLE)
			.addKeyValue("tv.matchstick.fling.metadata.LOCATION_LONGITUDE",
					"longitude", VALUE_TYPE_DOUBLE);

	/**
	 * web images
	 */
	private final List<WebImage> mImages;

	/**
	 * data
	 */
	private final Bundle mBundle;

	/**
	 * media type
	 */
	private int mMediaType;

	/**
	 * media keys
	 */
	public static final String KEY_CREATION_DATE = "tv.matchstick.fling.metadata.CREATION_DATE";
	public static final String KEY_RELEASE_DATE = "tv.matchstick.fling.metadata.RELEASE_DATE";
	public static final String KEY_BROADCAST_DATE = "tv.matchstick.fling.metadata.BROADCAST_DATE";
	public static final String KEY_TITLE = "tv.matchstick.fling.metadata.TITLE";
	public static final String KEY_SUBTITLE = "tv.matchstick.fling.metadata.SUBTITLE";
	public static final String KEY_ARTIST = "tv.matchstick.fling.metadata.ARTIST";
	public static final String KEY_ALBUM_ARTIST = "tv.matchstick.fling.metadata.ALBUM_ARTIST";
	public static final String KEY_ALBUM_TITLE = "tv.matchstick.fling.metadata.ALBUM_TITLE";
	public static final String KEY_COMPOSER = "tv.matchstick.fling.metadata.COMPOSER";
	public static final String KEY_DISC_NUMBER = "tv.matchstick.fling.metadata.DISC_NUMBER";
	public static final String KEY_TRACK_NUMBER = "tv.matchstick.fling.metadata.TRACK_NUMBER";
	public static final String KEY_SEASON_NUMBER = "tv.matchstick.fling.metadata.SEASON_NUMBER";
	public static final String KEY_EPISODE_NUMBER = "tv.matchstick.fling.metadata.EPISODE_NUMBER";
	public static final String KEY_SERIES_TITLE = "tv.matchstick.fling.metadata.SERIES_TITLE";
	public static final String KEY_STUDIO = "tv.matchstick.fling.metadata.STUDIO";
	public static final String KEY_WIDTH = "tv.matchstick.fling.metadata.WIDTH";
	public static final String KEY_HEIGHT = "tv.matchstick.fling.metadata.HEIGHT";
	public static final String KEY_LOCATION_NAME = "tv.matchstick.fling.metadata.LOCATION_NAME";
	public static final String KEY_LOCATION_LATITUDE = "tv.matchstick.fling.metadata.LOCATION_LATITUDE";
	public static final String KEY_LOCATION_LONGITUDE = "tv.matchstick.fling.metadata.LOCATION_LONGITUDE";

	/**
	 * Create default media data with MEDIA_TYPE_GENERIC
	 */
	public MediaMetadata() {
		this(MEDIA_TYPE_GENERIC);
	}

	/**
	 * Create the given media type data
	 * 
	 * @param mediaType
	 */
	public MediaMetadata(int mediaType) {
		this.mImages = new ArrayList<WebImage>();
		this.mBundle = new Bundle();
		this.mMediaType = mediaType;
	}

	/**
	 * Get media type
	 * 
	 * @return
	 */
	public int getMediaType() {
		return this.mMediaType;
	}

	/**
	 * Clear all resources of this object
	 */
	public void clear() {
		this.mBundle.clear();
		this.mImages.clear();
	}

	/**
	 * Check whether the object contains a field with the given key.
	 * 
	 * @param key
	 * @return
	 */
	public boolean containsKey(String key) {
		return this.mBundle.containsKey(key);
	}

	/**
	 * Get a set of keys for all fields that are present in the object.
	 * 
	 * @return
	 */
	public Set<String> keySet() {
		return this.mBundle.keySet();
	}

	/**
	 * Put Sting data
	 * 
	 * @param key
	 * @param value
	 */
	public void putString(String key, String value) {
		checkValueType(key, VALUE_TYPE_STRING);
		this.mBundle.putString(key, value);
	}

	/**
	 * Get String data
	 * 
	 * @param key
	 * @return
	 */
	public String getString(String key) {
		checkValueType(key, VALUE_TYPE_STRING);
		return this.mBundle.getString(key);
	}

	/**
	 * Put int data
	 * 
	 * @param key
	 * @param value
	 */
	public void putInt(String key, int value) {
		checkValueType(key, VALUE_TYPE_INT);
		this.mBundle.putInt(key, value);
	}

	/**
	 * Get int data
	 * 
	 * @param key
	 * @return
	 */
	public int getInt(String key) {
		checkValueType(key, VALUE_TYPE_INT);
		return this.mBundle.getInt(key);
	}

	/**
	 * Put double data
	 * 
	 * @param key
	 * @param value
	 */
	public void putDouble(String key, double value) {
		checkValueType(key, VALUE_TYPE_DOUBLE);
		this.mBundle.putDouble(key, value);
	}

	/**
	 * Get double data
	 * 
	 * @param key
	 * @return
	 */
	public double getDouble(String key) {
		checkValueType(key, VALUE_TYPE_DOUBLE);
		return this.mBundle.getDouble(key);
	}

	/**
	 * Put date data
	 * 
	 * @param key
	 * @param value
	 */
	public void putDate(String key, Calendar value) {
		checkValueType(key, VALUE_TYPE_ISO_8601_STRING);
		this.mBundle.putString(key, MetadataUtils.getDateByCalendar(value));
	}

	/**
	 * Get date data
	 * 
	 * @param key
	 * @return
	 */
	public Calendar getDate(String key) {
		checkValueType(key, VALUE_TYPE_ISO_8601_STRING);
		String str = this.mBundle.getString(key);
		return ((str != null) ? MetadataUtils.getCalendarByDate(str) : null);
	}

	/**
	 * Get date data and convert it to String type
	 * 
	 * @param key
	 * @return
	 */
	public String getDateAsString(String key) {
		checkValueType(key, VALUE_TYPE_ISO_8601_STRING);
		return this.mBundle.getString(key);
	}

	/**
	 * Check the specific key's value type
	 * 
	 * @param key
	 * @param valueType
	 * @throws IllegalArgumentException
	 */
	private void checkValueType(String key, int valueType)
			throws IllegalArgumentException {
		if (TextUtils.isEmpty(key))
			throw new IllegalArgumentException(
					"null and empty keys are not allowed");
		int type = mChecker.getValueTypeByBundleKey(key);
		if ((type == valueType) || (type == 0)) {
			return;
		}
		throw new IllegalArgumentException("Value for " + key + " must be a "
				+ VALUE_TYPES[valueType]);
	}

	/**
	 * Get Jsong data from meta data
	 * 
	 * @return
	 */
	public JSONObject buildJson() {
		JSONObject json = new JSONObject();
		try {
			json.put("metadataType", this.mMediaType);
		} catch (JSONException e) {
		}
		MetadataUtils.writeToJson(json, this.mImages);
		switch (this.mMediaType) {
		case MEDIA_TYPE_GENERIC:
			writeToJson(json, new String[] {
					"tv.matchstick.fling.metadata.TITLE",
					"tv.matchstick.fling.metadata.ARTIST",
					"tv.matchstick.fling.metadata.SUBTITLE",
					"tv.matchstick.fling.metadata.RELEASE_DATE" });
			break;
		case MEDIA_TYPE_MOVIE:
			writeToJson(json, new String[] {
					"tv.matchstick.fling.metadata.TITLE",
					"tv.matchstick.fling.metadata.STUDIO",
					"tv.matchstick.fling.metadata.SUBTITLE",
					"tv.matchstick.fling.metadata.RELEASE_DATE" });
			break;
		case MEDIA_TYPE_TV_SHOW:
			writeToJson(json, new String[] {
					"tv.matchstick.fling.metadata.TITLE",
					"tv.matchstick.fling.metadata.SERIES_TITLE",
					"tv.matchstick.fling.metadata.SEASON_NUMBER",
					"tv.matchstick.fling.metadata.EPISODE_NUMBER",
					"tv.matchstick.fling.metadata.BROADCAST_DATE" });
			break;
		case MEDIA_TYPE_MUSIC_TRACK:
			writeToJson(json, new String[] {
					"tv.matchstick.fling.metadata.TITLE",
					"tv.matchstick.fling.metadata.ARTIST",
					"tv.matchstick.fling.metadata.ALBUM_TITLE",
					"tv.matchstick.fling.metadata.ALBUM_ARTIST",
					"tv.matchstick.fling.metadata.COMPOSER",
					"tv.matchstick.fling.metadata.TRACK_NUMBER",
					"tv.matchstick.fling.metadata.DISC_NUMBER",
					"tv.matchstick.fling.metadata.RELEASE_DATE" });
			break;
		case MEDIA_TYPE_PHOTO:
			writeToJson(json, new String[] {
					"tv.matchstick.fling.metadata.TITLE",
					"tv.matchstick.fling.metadata.ARTIST",
					"tv.matchstick.fling.metadata.LOCATION_NAME",
					"tv.matchstick.fling.metadata.LOCATION_LATITUDE",
					"tv.matchstick.fling.metadata.LOCATION_LONGITUDE",
					"tv.matchstick.fling.metadata.WIDTH",
					"tv.matchstick.fling.metadata.HEIGHT",
					"tv.matchstick.fling.metadata.CREATION_DATE" });
			break;
		default:
			writeToJson(json, new String[0]);
		}
		return json;
	}

	/**
	 * Write Json to media data
	 * 
	 * @param json
	 */
	public void writeToBundle(JSONObject json) {
		clear();
		this.mMediaType = MEDIA_TYPE_GENERIC;
		try {
			this.mMediaType = json.getInt("metadataType");
		} catch (JSONException localJSONException) {
		}
		MetadataUtils.getWebImageListFromJson(this.mImages, json);
		switch (this.mMediaType) {
		case MEDIA_TYPE_GENERIC:
			writeToBundle(json, new String[] {
					"tv.matchstick.fling.metadata.TITLE",
					"tv.matchstick.fling.metadata.ARTIST",
					"tv.matchstick.fling.metadata.SUBTITLE",
					"tv.matchstick.fling.metadata.RELEASE_DATE" });
			break;
		case MEDIA_TYPE_MOVIE:
			writeToBundle(json, new String[] {
					"tv.matchstick.fling.metadata.TITLE",
					"tv.matchstick.fling.metadata.STUDIO",
					"tv.matchstick.fling.metadata.SUBTITLE",
					"tv.matchstick.fling.metadata.RELEASE_DATE" });
			break;
		case MEDIA_TYPE_TV_SHOW:
			writeToBundle(json, new String[] {
					"tv.matchstick.fling.metadata.TITLE",
					"tv.matchstick.fling.metadata.SERIES_TITLE",
					"tv.matchstick.fling.metadata.SEASON_NUMBER",
					"tv.matchstick.fling.metadata.EPISODE_NUMBER",
					"tv.matchstick.fling.metadata.BROADCAST_DATE" });
			break;
		case MEDIA_TYPE_MUSIC_TRACK:
			writeToBundle(json, new String[] {
					"tv.matchstick.fling.metadata.TITLE",
					"tv.matchstick.fling.metadata.ALBUM_TITLE",
					"tv.matchstick.fling.metadata.ARTIST",
					"tv.matchstick.fling.metadata.ALBUM_ARTIST",
					"tv.matchstick.fling.metadata.COMPOSER",
					"tv.matchstick.fling.metadata.TRACK_NUMBER",
					"tv.matchstick.fling.metadata.DISC_NUMBER",
					"tv.matchstick.fling.metadata.RELEASE_DATE" });
			break;
		case MEDIA_TYPE_PHOTO:
			writeToBundle(json, new String[] {
					"tv.matchstick.fling.metadata.TITLE",
					"tv.matchstick.fling.metadata.ARTIST",
					"tv.matchstick.fling.metadata.LOCATION_NAME",
					"tv.matchstick.fling.metadata.LOCATION_LATITUDE",
					"tv.matchstick.fling.metadata.LOCATION_LONGITUDE",
					"tv.matchstick.fling.metadata.WIDTH",
					"tv.matchstick.fling.metadata.HEIGHT",
					"tv.matchstick.fling.metadata.CREATION_DATE" });
			break;
		default:
			writeToBundle(json, new String[0]);
		}
	}

	/**
	 * Create Json object by keys from meida meta data
	 * 
	 * @param json
	 * @param keys
	 */
	private void writeToJson(JSONObject json, String[] keys) {
		try {
			for (String key : keys) {
				if (!(this.mBundle.containsKey(key))) {
					continue;
				}
				int type = mChecker.getValueTypeByBundleKey(key);
				switch (type) {
				case VALUE_TYPE_STRING:
				case VALUE_TYPE_ISO_8601_STRING:
					json.put(mChecker.getJsonKeyByBundleKey(key),
							this.mBundle.getString(key));
					break;
				case VALUE_TYPE_INT:
					json.put(mChecker.getJsonKeyByBundleKey(key),
							this.mBundle.getInt(key));
					break;
				case VALUE_TYPE_DOUBLE:
					json.put(mChecker.getJsonKeyByBundleKey(key),
							this.mBundle.getDouble(key));
				}
			}
			Iterator<String> bundleKeys = this.mBundle.keySet().iterator();
			while (bundleKeys.hasNext()) {
				String key = bundleKeys.next();
				if (!(key.startsWith("com.google."))) {
					Object value = this.mBundle.get(key);
					if (value instanceof String) {
						json.put(key, value);
					} else if (value instanceof Integer) {
						json.put(key, value);
					} else if (value instanceof Double) {
						json.put(key, value);
					}
				}
			}
		} catch (JSONException e) {
		}
	}

	/**
	 * Write Json data to media meta data by keys
	 * 
	 * @param json
	 * @param keys
	 */
	private void writeToBundle(JSONObject json, String[] keys) {
		HashSet keysHashset = new HashSet(Arrays.asList(keys));
		try {
			Iterator jsonKeys = json.keys();
			while (jsonKeys.hasNext()) {
				String jsonKey = (String) jsonKeys.next();
				if ("metadataType".equals(jsonKey)) {
					continue;
				}
				String bundleKey = mChecker.getBundleKeyByJsonKey(jsonKey);
				if (bundleKey != null) {
					if (keysHashset.contains(bundleKey))
						try {
							Object value = json.get(jsonKey);
							if (value != null) {
								switch (mChecker
										.getValueTypeByBundleKey(bundleKey)) {
								case VALUE_TYPE_STRING:
									if (value instanceof String)
										this.mBundle.putString(bundleKey,
												(String) value);
									break;
								case VALUE_TYPE_ISO_8601_STRING:
									if (value instanceof String) {
										Calendar localCalendar = MetadataUtils
												.getCalendarByDate((String) value);
										if (localCalendar != null)
											this.mBundle.putString(bundleKey,
													(String) value);
									}
									break;
								case VALUE_TYPE_INT:
									if (value instanceof Integer)
										this.mBundle.putInt(bundleKey,
												((Integer) value).intValue());
									break;
								case VALUE_TYPE_DOUBLE:
									if (value instanceof Double)
										this.mBundle.putDouble(bundleKey,
												((Double) value).doubleValue());
								}
							}
						} catch (JSONException e) {
						}
				} else {
					Object obj = json.get(jsonKey);
					if (obj instanceof String)
						this.mBundle.putString(jsonKey, (String) obj);
					else if (obj instanceof Integer)
						this.mBundle
								.putInt(jsonKey, ((Integer) obj).intValue());
					else if (obj instanceof Double)
						this.mBundle.putDouble(jsonKey,
								((Double) obj).doubleValue());
				}
			}
		} catch (JSONException e) {
		}
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}

		if (!(other instanceof MediaMetadata)) {
			return false;
		}

		MediaMetadata data = (MediaMetadata) other;
		return ((compareBundle(this.mBundle, data.mBundle)) && (this.mImages
				.equals(data.mImages)));
	}

	@Override
	public int hashCode() {
		int i = 17;
		Set keySet = this.mBundle.keySet();
		Iterator iterator = keySet.iterator();
		while (iterator.hasNext()) {
			String str = (String) iterator.next();
			i = 31 * i + this.mBundle.get(str).hashCode();
		}
		i = 31 * i + this.mImages.hashCode();
		return i;
	}

	/**
	 * Get Images
	 * 
	 * @return
	 */
	public List<WebImage> getImages() {
		return this.mImages;
	}

	/**
	 * Check whether there're images
	 * 
	 * @return
	 */
	public boolean hasImages() {
		return ((this.mImages != null) && (!(this.mImages.isEmpty())));
	}

	/**
	 * Clear images
	 */
	public void clearImages() {
		this.mImages.clear();
	}

	/**
	 * Add an image to the list of images.
	 * 
	 * @param image
	 */
	public void addImage(WebImage image) {
		this.mImages.add(image);
	}

	/**
	 * Compare bundle data
	 * 
	 * @param one
	 * @param other
	 * @return
	 */
	private boolean compareBundle(Bundle one, Bundle other) {
		if (one.size() != other.size()) {
			return false;
		}

		Set keySet = one.keySet();
		Iterator iterator = keySet.iterator();
		while (iterator.hasNext()) {
			String key = (String) iterator.next();
			Object valueOne = one.get(key);
			Object valueOther = other.get(key);
			if ((valueOne instanceof Bundle)
					&& (valueOther instanceof Bundle)
					&& (!(compareBundle((Bundle) valueOne, (Bundle) valueOther)))) {
				return false;
			}

			if (valueOne == null) {
				if ((valueOther != null) || (!(other.containsKey(key))))
					return false;
				else if (!(valueOne.equals(valueOther)))
					return false;
			}
		}

		return true;
	}

	/**
	 * Value type checker utils
	 */
	private static class ValueTypesChecker {
		private final Map<String, String> bundleKeyToJsonKey = new HashMap<String, String>();
		private final Map<String, String> jsonKeyToBundleKey = new HashMap<String, String>();
		private final Map<String, Integer> bundleKeyToValueType = new HashMap<String, Integer>();

		public ValueTypesChecker addKeyValue(String bundleKey, String jsonKey,
				int valueType) {
			this.bundleKeyToJsonKey.put(bundleKey, jsonKey);
			this.jsonKeyToBundleKey.put(jsonKey, bundleKey);
			this.bundleKeyToValueType
					.put(bundleKey, Integer.valueOf(valueType));
			return this;
		}

		public String getJsonKeyByBundleKey(String key) {
			return ((String) this.bundleKeyToJsonKey.get(key));
		}

		public String getBundleKeyByJsonKey(String key) {
			return ((String) this.jsonKeyToBundleKey.get(key));
		}

		public int getValueTypeByBundleKey(String key) {
			Integer i = (Integer) this.bundleKeyToValueType.get(key);
			return ((i != null) ? i.intValue() : 0);
		}
	}

}
