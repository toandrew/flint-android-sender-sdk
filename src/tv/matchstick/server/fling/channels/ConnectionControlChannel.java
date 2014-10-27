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

package tv.matchstick.server.fling.channels;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import tv.matchstick.client.internal.FlingChannel;
import android.os.Build;

public final class ConnectionControlChannel extends FlingChannel {

	private static final String mUserAgent = String.format(
			"Android FlingSDK,%d,%s,%s,%s", 0x40be38, Build.MODEL,
			Build.PRODUCT, android.os.Build.VERSION.RELEASE);

	private final String mPackage;

	public ConnectionControlChannel(String pName) {
		super("urn:x-cast:com.google.cast.tp.connection",
				"ConnectionControlChannel");
		mPackage = pName;
	}

	public final void connect(String transportId) throws IOException {
		JSONObject jsonobject = new JSONObject();
		try {
			jsonobject.put("type", "CONNECT");
			JSONObject obj = new JSONObject();
			obj.put("package", mPackage);
			jsonobject.put("origin", obj);
			jsonobject.put("userAgent", mUserAgent);
		} catch (JSONException e) {
		}
		sendTextMessage(jsonobject.toString(), 0L, transportId);
	}

	public final void close(String targetId) throws IOException {
		JSONObject jsonobject = new JSONObject();
		try {
			jsonobject.put("type", "CLOSE");
		} catch (JSONException e) {
		}

		sendTextMessage(jsonobject.toString(), 0L, targetId);
	}
}
