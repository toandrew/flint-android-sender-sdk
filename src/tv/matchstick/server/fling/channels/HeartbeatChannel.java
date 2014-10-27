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
import android.os.SystemClock;

public final class HeartbeatChannel extends FlingChannel {
	private long mCreateTime;
	private long mTimeout;
	private boolean isPingSent;
	private int mCounter;

	public HeartbeatChannel() {
		super("urn:x-cast:com.google.cast.tp.heartbeat", "HeartbeatChannel");
		mTimeout = 10000L;
		reset();
	}

	public final void reset() {
		mCreateTime = SystemClock.elapsedRealtime();
		isPingSent = false;
		mCounter = 0;
	}

	public final boolean isTimeout(long currentTime) {
		if (mTimeout == 0L) {
			return false;
		}

		long elapsedTime;
		elapsedTime = currentTime - mCreateTime;
		if (elapsedTime >= mTimeout) {
			if (mCounter < 5) {
				android.util.Log.d("HeartbeatChannel", "retry PING: "
						+ mCounter);
				log.v("retry PING", new Object[0]);
				sendPing();
				mCounter++;
			} else {
				mCounter = 0;
				return true;
			}

		}
		if (isPingSent || elapsedTime < mTimeout / 2L) {
			return false;
		}

		sendPing();
		return false;
	}

	private void sendPing() {
		JSONObject jsonobject;
		log.v("sending PING", new Object[0]);
		jsonobject = new JSONObject();
		try {
			jsonobject.put("type", "PING");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		try {
			sendTextMessage(jsonobject.toString(), 0L, "transport-0");
		} catch (IOException e) {
			// TODO: No FlingManager available
			e.printStackTrace();
		}
		isPingSent = true;
	}

	@Override
	public final void onMessageReceived(String message) {
		reset();
		log.v("Received: %s", message);
		boolean isEqual;

		try {
			isEqual = "PING"
					.equals((new JSONObject(message)).getString("type"));
		} catch (JSONException e) {
			log.w("Message is malformed (%s); ignoring: %s", e.getMessage(),
					message);
			return;
		}

		if (!isEqual) {
			return;
		}

		log.v("sending PONG", new Object[0]);
		JSONObject jsonobject = new JSONObject();
		try {
			jsonobject.put("type", "PONG");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		try {
			sendTextMessage(jsonobject.toString(), 0L, "transport-0");
		} catch (IOException e) {
			// TODO: No FlingManager available
			e.printStackTrace();
		}
	}
}
