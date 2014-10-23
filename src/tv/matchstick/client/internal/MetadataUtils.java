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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tv.matchstick.fling.images.WebImage;
import android.text.TextUtils;

public class MetadataUtils {

	private static final LogUtil log = new LogUtil("MetadataUtils");
	private static final String[] FORMATS = { "Z", "+hh", "+hhmm", "+hh:mm" };
	private static final String DEFAULT_FORMAT = "yyyyMMdd'T'HHmmss"
			+ FORMATS[0];

	public static void getWebImageListFromJson(List<WebImage> webImages,
			JSONObject data) {
		try {
			webImages.clear();
			JSONArray images = data.getJSONArray("images");
			int size = images.length();
			for (int j = 0; j < size; ++j) {
				JSONObject image = images.getJSONObject(j);
				try {
					webImages.add(new WebImage(image));
				} catch (IllegalArgumentException e) {
				}
			}
		} catch (JSONException e) {
		}
	}

	public static void writeToJson(JSONObject data, List<WebImage> webImages) {
		if ((webImages == null) || (webImages.isEmpty())) {
			return;
		}
		JSONArray jsonArr = new JSONArray();
		Iterator<WebImage> it = webImages.iterator();
		while (it.hasNext()) {
			WebImage image = (WebImage) it.next();
			jsonArr.put(image.buildJson());
		}
		try {
			data.put("images", jsonArr);
		} catch (JSONException localJSONException) {
		}
	}

	public static String getDateByCalendar(Calendar calendar) {
		if (calendar == null) {
			log.d("Calendar object cannot be null");
			return null;
		}
		String format = DEFAULT_FORMAT;
		if ((calendar.get(11) == 0) && (calendar.get(12) == 0)
				&& (calendar.get(13) == 0)) {
			format = "yyyyMMdd";
		}
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
		simpleDateFormat.setTimeZone(calendar.getTimeZone());
		String date = simpleDateFormat.format(calendar.getTime());
		if (date.endsWith("+0000")) {
			date = date.replace("+0000", FORMATS[0]);
		}
		return date;
	}

	public static Calendar getCalendarByDate(String dateString) {
		if (TextUtils.isEmpty(dateString)) {
			log.d("Input string is empty or null");
			return null;
		}
		String date = extractDate(dateString);
		if (TextUtils.isEmpty(date)) {
			log.d("Invalid date format", new Object[0]);
			return null;
		}
		String time = extractTime(dateString);
		String format = "yyyyMMdd";
		if (!(TextUtils.isEmpty(time))) {
			date = date + "T" + time;
			if (time.length() == "HHmmss".length())
				format = "yyyyMMdd'T'HHmmss";
			else
				format = DEFAULT_FORMAT;
		}
		Calendar localCalendar = GregorianCalendar.getInstance();
		Date localDate;
		try {
			localDate = new SimpleDateFormat(format).parse(date);
		} catch (ParseException e) {
			log.d("Error parsing string: %s", e.getMessage());
			return null;
		}
		localCalendar.setTime(localDate);
		return localCalendar;
	}

	private static String extractDate(String date) {
		if (TextUtils.isEmpty(date)) {
			log.d("Input string is empty or null");
			return null;
		}
		try {
			return date.substring(0, "yyyyMMdd".length());
		} catch (IndexOutOfBoundsException e) {
			log.i("Error extracting the date: %s", e.getMessage());
		}
		return null;
	}

	private static String extractTime(String date) {
		if (TextUtils.isEmpty(date)) {
			log.d("string is empty or null");
			return null;
		}
		int i = date.indexOf(84);
		if (i++ != "yyyyMMdd".length()) {
			log.d("T delimeter is not found");
			return null;
		}
		String str;
		try {
			str = date.substring(i);
		} catch (IndexOutOfBoundsException e) {
			log.d("Error extracting the time substring: %s", e.getMessage());
			return null;
		}
		if (str.length() == "HHmmss".length())
			return str;
		int j = str.charAt("HHmmss".length());
		switch (j) {
		case 90:
			if (str.length() == "HHmmss".length() + FORMATS[0].length())
				return str.substring(0, str.length() - 1) + "+0000";
			return null;
		case 43:
		case 45:
			if (!(getLen(str))) {
				break;
			}
			return str.replaceAll("([\\+\\-]\\d\\d):(\\d\\d)", "$1$2");
		}
		return null;
	}

	private static boolean getLen(String paramString) {
		int iLen = paramString.length();
		int len = "HHmmss".length();
		return ((iLen == len + FORMATS[1].length())
				|| (iLen == len + FORMATS[2].length()) || (iLen == len
				+ FORMATS[3].length()));
	}

}
