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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tv.matchstick.client.internal.FlingChannel;
import tv.matchstick.client.internal.RequestTracker;
import tv.matchstick.client.internal.RequestTrackerCallback;
import tv.matchstick.server.utils.ApplicationInfo;
import android.text.TextUtils;

public abstract class ReceiverControlChannel extends FlingChannel {
	public String mTransportId;
	public final RequestTracker mLaunchRequestTracker;
	public final RequestTracker mStopSessionRequestTracker;
	public final RequestTracker mGetStatusRequestTracker;
	public final RequestTracker mSetVolumeRequestTracker;
	public final RequestTracker mSetMuteRequestTracker;
	public double mLevel;
	public boolean mMuted;
	public boolean mFirst;
	private final String mReceiverDestinationId;
	private final RequestTrackerCallback mLaunchAppResult;
	private final RequestTrackerCallback mStopSessionResult;
	private final RequestTrackerCallback mGetStatusResult;

	public ReceiverControlChannel(String receiverDestinationId) {
		super("urn:x-cast:com.google.cast.receiver", "ReceiverControlChannel");

		if (TextUtils.isEmpty(receiverDestinationId)) {
			throw new IllegalArgumentException(
					"receiverDestinationId can't be empty");
		}

		mReceiverDestinationId = receiverDestinationId;

		mLaunchRequestTracker = new RequestTracker(30000L); // 10000L

		mLaunchAppResult = new RequestTrackerCallback() {

			@Override
			public void onTrackRequest(long requestId, int result,
					JSONObject jsonobject) {
				if (result != 0) {
					onApplicationConnectionFailed(result);
				}
			}

			@Override
			public void onSignInRequired(long requestId) {

			}

		};

		mStopSessionRequestTracker = new RequestTracker(3000L);

		mStopSessionResult = new RequestTrackerCallback() {

			@Override
			public void onTrackRequest(long requestId, int result,
					JSONObject jsonobject) {
				if (result != 0) {
					onRequestStatus(result);
				}
			}

			@Override
			public void onSignInRequired(long requestId) {

			}

		};

		mGetStatusRequestTracker = new RequestTracker(3000L);

		mGetStatusResult = new RequestTrackerCallback() {

			@Override
			public void onTrackRequest(long requestId, int result,
					JSONObject jsonobject) {
				if (result != 0) {
					onStatusRequestFailed(result);
				}
			}

			@Override
			public void onSignInRequired(long requestId) {

			}

		};

		mSetVolumeRequestTracker = new RequestTracker(3000L);
		mSetMuteRequestTracker = new RequestTracker(3000L);
	}

	public final void getStatus() throws IOException {
		if (mGetStatusRequestTracker.isRequestIdAvailable())
			return;

		long requestId = getRequestId();
		JSONObject jsonobject = new JSONObject();
		try {
			jsonobject.put("requestId", requestId);
			jsonobject.put("type", "GET_STATUS");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		sendTextMessage(jsonobject.toString(), requestId,
				mReceiverDestinationId);

		mGetStatusRequestTracker.startTrack(requestId, mGetStatusResult);
	}

	public final void setVolume(double level, double expected_level,
			boolean muted) throws IOException {
		if (level < 0.0D)
			level = 0.0D;
		else if (level > 1.0D)
			level = 1.0D;

		long requestId = getRequestId();
		JSONObject jsonobject = new JSONObject();
		try {
			jsonobject.put("requestId", requestId);
			jsonobject.put("type", "SET_VOLUME");
			JSONObject obj1 = new JSONObject();
			obj1.put("level", level);
			jsonobject.put("volume", obj1);
			JSONObject obj2 = new JSONObject();
			obj2.put("level", expected_level);
			obj2.put("muted", muted);
			jsonobject.put("expectedVolume", obj2);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		sendTextMessage(jsonobject.toString(), requestId,
				mReceiverDestinationId);

		mSetVolumeRequestTracker.startTrack(requestId, null);
	}

	protected abstract void onRequestStatus(int result);

	protected abstract void onConnectToApplicationAndNotify(
			ApplicationInfo appInfo);

	protected abstract void onStatusReceived(ApplicationInfo atn1,
			double level, boolean muted);

	public final void launchApplication(String appId, String param)
			throws IOException {
		long requestId = getRequestId();
		JSONObject jsonobject = new JSONObject();
		try {
			jsonobject.put("requestId", requestId);
			jsonobject.put("type", "LAUNCH");
			jsonobject.put("appId", appId);
			if (param != null) {
				jsonobject.put("commandParameters", param);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		sendTextMessage(jsonobject.toString(), requestId,
				mReceiverDestinationId);

		mLaunchRequestTracker.startTrack(requestId, mLaunchAppResult);
	}

	public final void setMute(boolean muted, double level, boolean isMuted)
			throws IOException {
		long requestId = getRequestId();
		JSONObject jsonobject = new JSONObject();
		try {
			jsonobject.put("requestId", requestId);
			jsonobject.put("type", "SET_VOLUME");
			JSONObject obj1 = new JSONObject();
			obj1.put("muted", muted);
			jsonobject.put("volume", obj1);
			JSONObject obj2 = new JSONObject();
			obj2.put("level", level);
			obj2.put("muted", isMuted);
			jsonobject.put("expectedVolume", obj2);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		sendTextMessage(jsonobject.toString(), requestId,
				mReceiverDestinationId);

		mSetMuteRequestTracker.startTrack(requestId, null);
	}

	@Override
	public final void onMessageReceived(String message) {
		char c1;
		char c2;
		c1 = '\u07D1';
		c2 = '\r';
		log.d("Received: %s", message);

		try {
			JSONObject jsonobject;
			String type;
			long reuestId;
			jsonobject = new JSONObject(message);
			type = jsonobject.getString("type");
			reuestId = jsonobject.optLong("requestId", -1L);
			if ("RECEIVER_STATUS".equals(type)) {
				JSONObject statusJsonObj;
				boolean hasApplications;
				statusJsonObj = jsonobject.getJSONObject("status");
				hasApplications = statusJsonObj.has("applications");
				ApplicationInfo appInfo;
				appInfo = null;
				if (hasApplications) {
					JSONArray applications;
					int len;
					JSONObject firstApplication;
					ApplicationInfo applicationInfo;
					try {
						applications = statusJsonObj
								.getJSONArray("applications");
						len = applications.length();
					} catch (JSONException e) {
						log.w("Message is malformed (%s); ignoring: %s",
								e.getMessage(), message);
						return;
					}
					appInfo = null;
					if (len > 0) {
						firstApplication = applications.getJSONObject(0);
						applicationInfo = new ApplicationInfo(firstApplication);
						appInfo = applicationInfo;
					}
				}
				mGetStatusRequestTracker.trackRequest(reuestId, 0);
				if (mLaunchRequestTracker.trackRequest(reuestId, 0)) {
					if (appInfo != null) {
						log.d("application launch has completed");

						onConnectToApplicationAndNotify(appInfo);
					}
					return;
				}
				boolean flag1;
				boolean ignoreVolume;
				if (mTransportId == null || (appInfo != null)
						&& appInfo.getTransportId().equals(mTransportId)) {
					flag1 = false;
				} else {
					flag1 = true;
				}

				boolean flag2 = mStopSessionRequestTracker.trackRequest(
						reuestId, 0);

				if (flag1) {
					log.d("application has stopped");
				}

				if (!flag1 && !flag2) {

				} else {
					onApplicationDisconnected();
				}

				synchronized (RequestTracker.mLock) {
					mSetVolumeRequestTracker.trackRequest(reuestId, 0);
					mSetMuteRequestTracker.trackRequest(reuestId, 0);
					if (mSetVolumeRequestTracker.isRequestIdAvailable()
							|| mSetMuteRequestTracker.isRequestIdAvailable())
						ignoreVolume = true;
					else
						ignoreVolume = false;
				}

				log.d("requestId = %d, ignoreVolume = %b", reuestId,
						ignoreVolume);

				if (!mFirst) {
					log.d("first status received, so not ignoring volume change");
					mFirst = true;
					ignoreVolume = false;
				}
				if (!statusJsonObj.has("volume") || ignoreVolume) {

				} else {
					JSONObject jsonobject3 = statusJsonObj
							.getJSONObject("volume");
					mLevel = jsonobject3.getDouble("level");
					mMuted = jsonobject3.getBoolean("muted");
				}
				onStatusReceived(appInfo, mLevel, mMuted);
				return;
			} else if ("LAUNCH_ERROR".equals(type)) {
				String reason;
				reason = jsonobject.getString("reason");
				if ("BAD_PARAMETER".equals(reason)) {
					mLaunchRequestTracker.trackRequest(reuestId, c1);
					return;
				} else if ("CANCELLED".equals(reason)) {
					c1 = '\u07D2';
				} else if ("NOT_ALLOWED".equals(reason)) {
					c1 = '\u07D3';
				} else if ("NOT_FOUND".equals(reason)) {
					c1 = '\u07D4';
				} else if ("CAST_INIT_TIMEOUT".equals(reason)) {
					c1 = '\017';
				} else {
					c1 = c2;
				}
				mLaunchRequestTracker.trackRequest(reuestId, c1);
				return;
			} else if ("INVALID_REQUEST".equals(type)) {
				String reason = jsonobject.getString("reason");
				if ("INVALID_COMMAND".equals(reason)
						|| "DUPLICATE_REQUEST_ID".equals(reason))
					c2 = c1;
				mLaunchRequestTracker.trackRequest(reuestId, c2);
				mStopSessionRequestTracker.trackRequest(reuestId, c2);
				mGetStatusRequestTracker.trackRequest(reuestId, c2);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return;
	}

	protected abstract void onApplicationDisconnected();

	protected abstract void onApplicationConnectionFailed(int reason);

	public final void stopSession(String sessionId) throws IOException {
		JSONObject jsonobject;
		long requestId;
		jsonobject = new JSONObject();
		requestId = getRequestId();
		try {
			jsonobject.put("requestId", requestId);
			jsonobject.put("type", "STOP");
			if (sessionId != null)
				if (!"".equals(sessionId))
					jsonobject.put("sessionId", sessionId);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		sendTextMessage(jsonobject.toString(), requestId,
				mReceiverDestinationId);

		mStopSessionRequestTracker.startTrack(requestId, mStopSessionResult);
		return;
	}

	protected abstract void onStatusRequestFailed(int reason);

	public final void setTransportId(String transId) {
		log.d("current transport id (in control channel) is now: %s", transId);
		mTransportId = transId;
	}
}
