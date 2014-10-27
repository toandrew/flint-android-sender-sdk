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

package tv.matchstick.server.fling;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tv.matchstick.fling.FlingDevice;
import tv.matchstick.server.fling.socket.FlingSocket;
import tv.matchstick.server.fling.socket.FlingSocketListener;
import tv.matchstick.server.fling.socket.data.FlingMessage;
import tv.matchstick.server.utils.LOG;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * This is used to filter device
 */
abstract class DeviceFilter {
	private static final LOG log = new LOG("DeviceFilter");
	private static AtomicLong mIdGen = new AtomicLong(0L);
	private final Context mContext;
	private final String mPackageName;
	private final List mDeviceConnections = new ArrayList();
	private final Handler mHandler = new Handler(Looper.getMainLooper());
	private Set<DiscoveryCriteria> mDiscoveryCriterias;

	public DeviceFilter(Context context, Set<DiscoveryCriteria> set,
			String packageName) {
		mContext = context;
		mDiscoveryCriterias = new HashSet<DiscoveryCriteria>(set);
		mPackageName = packageName;
	}

	protected abstract void setDeviceOffline(FlingDevice flingdevice);

	protected abstract void onDeviceAccepted(FlingDevice flingdevice, Set set);

	/**
	 * Reset discovery criteria.
	 * 
	 * @param criterias
	 */
	public final void reset(Set<DiscoveryCriteria> criterias) {
		for (Iterator iterator = mDeviceConnections.iterator(); iterator
				.hasNext();) {
			((FlingDeviceManager) iterator.next()).mIsConnecting = false;
		}

		mDeviceConnections.clear();

		mDiscoveryCriterias = new HashSet<DiscoveryCriteria>(criterias);
	}

	/**
	 * Connect or accept device
	 * 
	 * @param device
	 */
	public final void connectOrAcceptDevice(FlingDevice device) {
		FlingDeviceManager manager = new FlingDeviceManager(this, device);

		if (!manager.mNoApp || !manager.mNoNamespace) {
			try {
				log.d("connecting to: %s:%d (%s)", manager.mFlingDevice
						.getIpAddress().toString(), manager.mFlingDevice
						.getServicePort(), manager.mFlingDevice
						.getFriendlyName());

				manager.mFlingSocket.connect(
						manager.mFlingDevice.getIpAddress(),
						manager.mFlingDevice.getServicePort());
			} catch (Exception e) {
				log.e(e, "Exception while connecting socket");
			}
		} else {
			log.d("accept device to: %s:%d (%s)", manager.mFlingDevice
					.getIpAddress().toString(), manager.mFlingDevice
					.getServicePort(), manager.mFlingDevice.getFriendlyName());

			manager.acceptDevice(manager.mFlingDevice,
					manager.mDeviceFilter.mDiscoveryCriterias);
		}

		mDeviceConnections.add(manager);
	}

	private final class FlingDeviceManager implements FlingSocketListener {
		final FlingSocket mFlingSocket;
		final FlingDevice mFlingDevice;
		boolean mNoApp;
		boolean mNoNamespace;
		boolean mIsConnecting;
		final DeviceFilter mDeviceFilter;
		private final String mSourceId;
		private final AppInfoHelper mAppInfoHelper = new AppInfoHelper();
		private final JSONArray mApplicationIds = new JSONArray();

		public FlingDeviceManager(DeviceFilter deviceFilter,
				FlingDevice flingDevice) {
			super();

			mDeviceFilter = deviceFilter;

			mNoApp = true;
			mNoNamespace = false;
			mIsConnecting = true;
			mFlingSocket = new FlingSocket(mContext, this);
			mFlingDevice = flingDevice;

			mSourceId = String.format("%s-%d", mPackageName,
					mIdGen.incrementAndGet());

			if (mDiscoveryCriterias.size() > 0) {
				boolean noNameSpace = true;
				boolean noApp = true;

				Iterator<DiscoveryCriteria> iterator = mDiscoveryCriterias
						.iterator();
				while (iterator.hasNext()) {
					DiscoveryCriteria criteria = (DiscoveryCriteria) iterator
							.next();
					Set namespaces = Collections
							.unmodifiableSet(criteria.mNamespaceList);
					if (criteria.mAppid != null) {
						mApplicationIds.put(criteria.mAppid);
						noApp = false;
					}
					if (namespaces.size() > 0) {
						noNameSpace = false;
					}
				}

				if (noApp) {
					setNoApp();
				}

				if (noNameSpace) {
					setNoNameSpace();
				}
			}
		}

		private void sendMessage(String namespace, String message)
				throws Exception {
			log.d("Sending text message to %s: (ns=%s, dest=%s) %s",
					mFlingDevice.getFriendlyName(), namespace, "receiver-0",
					message);

			FlingMessage msg = new FlingMessage();
			msg.setProtocolVersion(0);
			msg.setSourceId(mSourceId);
			msg.setDestinationId("receiver-0");
			msg.setNamespace(namespace);
			msg.setPayloadMessage(message);

			byte bytes[] = msg.buildJson().toString().getBytes("UTF-8");

			mFlingSocket.send(ByteBuffer.wrap(bytes));
		}

		private void setNoApp() {
			mNoApp = true;
			if (mNoApp && mNoNamespace) {
				updateStatus();
			}
		}

		private void setNoNameSpace() {
			mNoNamespace = true;
			if (mNoApp && mNoNamespace) {
				updateStatus();
			}
		}

		private void updateStatus() {
			if (mFlingSocket.isConnected()) {
				try {
					sendMessage("urn:x-cast:com.google.cast.tp.connection",
							(new JSONObject()).put("type", "CLOSE").toString());
				} catch (IOException ex) {
					log.d(ex.toString(), "Failed to send disconnect message");
				} catch (JSONException je) {
					log.e(je.toString(), "Failed to build disconnect message");
				} catch (Exception e) {
					e.printStackTrace();
					log.d(e.toString(), "Failed to send disconnect message");
				}
				mFlingSocket.disconnect();
			}
			HashSet hashset = new HashSet();
			Iterator<DiscoveryCriteria> iterator = mDiscoveryCriterias
					.iterator();
			do {
				if (!iterator.hasNext()) {
					break;
				}
				DiscoveryCriteria criteria = (DiscoveryCriteria) iterator
						.next();

				boolean flag = false;

				// if we need setNoApp later, the mAppAvailabityList must
				// contains
				if (/*
					 * (criteria.mAppid == null ||
					 * h.mAppAvailabityList.contains(criteria.mAppid)) &&
					 */mAppInfoHelper.mAppNamespaceList.containsAll(Collections
						.unmodifiableSet(criteria.mNamespaceList))) {
					flag = true;
				}

				if (flag) {
					hashset.add(criteria);
				}
			} while (true);

			if (mIsConnecting && hashset.size() > 0) {
				acceptDevice(mFlingDevice, hashset);
				return;
			}

			log.d("rejected device: %s", mFlingDevice);
			return;
		}

		@Override
		public final void onConnected() {
			try {
				sendMessage(
						"urn:x-cast:com.google.cast.tp.connection",
						(new JSONObject())
								.put("type", "CONNECT")
								.put("origin",
										new JSONObject().put("package",
												mPackageName)).toString());

				if (!mNoApp)
					sendMessage(
							"urn:x-cast:com.google.cast.receiver",
							(new JSONObject()).put("requestId", 1)
									.put("type", "GET_APP_AVAILABILITY")
									.put("appId", mApplicationIds).toString());

				if (!mNoNamespace)
					sendMessage(
							"urn:x-cast:com.google.cast.receiver",
							(new JSONObject()).put("requestId", 2)
									.put("type", "GET_STATUS").toString());

				return;
			} catch (JSONException ex) {
				log.e(ex.toString(), "Failed to build messages");
			} catch (Exception e) {
				log.e(e.toString(), "Failed to send messages");
				e.printStackTrace();
			}
		}

		@Override
		public final void onConnectionFailed(int reason) {
			log.w("Connection to %s:%d (%s) failed with error %d", mFlingDevice
					.getIpAddress().toString(), mFlingDevice.getServicePort(),
					mFlingDevice.getFriendlyName(), reason);

			mHandler.post(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					mDeviceFilter.setDeviceOffline(mFlingDevice);
				}

			});
		}

		final void acceptDevice(final FlingDevice flingdevice, final Set set) {
			mHandler.post(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					mDeviceFilter.onDeviceAccepted(flingdevice, set);
				}

			});
		}

		@Override
		public final void onMessageReceived(ByteBuffer receivedMessage) {
			long requestId;
			log.d("onMessageReceived:in[%s]", mSourceId);

			FlingMessage flingMessage = new FlingMessage(
					receivedMessage.array());

			if (flingMessage.getPayloadType() != 0) {
				return;
			}

			String message = flingMessage.getMessage();
			Log.d("DeviceFilter", "onMessageReceived:" + message);

			try {
				JSONObject obj = new JSONObject(message);
				requestId = obj.optLong("requestId", -1L);
				if (requestId == -1L) {
					return;
				}
				if (requestId != 1L) {
					if (requestId != 2L) {
						log.d("Unrecognized request ID: " + requestId);
						return;
					}
					mAppInfoHelper.fillNamespaceList(obj);
					setNoNameSpace();
					return;
				}
				mAppInfoHelper.fillAppAvailabityList(obj);
				setNoApp();
			} catch (JSONException e) {
				log.e("Failed to parse response: %s", e.getMessage());
			}

		}

		@Override
		public final void onDisconnected(int reason) {
			log.d("Device filter disconnected:" + reason);
		}

		private final class AppInfoHelper {
			final Set mAppNamespaceList;
			final Set mAppAvailabityList;

			private AppInfoHelper() {
				mAppNamespaceList = new HashSet();
				mAppAvailabityList = new HashSet();
			}

			public final void fillNamespaceList(JSONObject jsonobject) {
				try {
					JSONArray applications = jsonobject.getJSONObject("status")
							.getJSONArray("applications");
					int i = 0;
					while (i < applications.length()) {
						JSONArray namespaces = applications.getJSONObject(i)
								.getJSONArray("namespaces");
						int j = 0;
						while (j < namespaces.length()) {
							String name = namespaces.getJSONObject(j)
									.getString("name");
							mAppNamespaceList.add(name);
							j++;
						}
						i++;
					}
				} catch (JSONException e) {
					e.printStackTrace();

					log.d("No namespaces found in receiver response: %s",
							e.getMessage());
				}
			}

			public final void fillAppAvailabityList(JSONObject obj) {
				try {
					JSONObject availability = obj.getJSONObject("availability");
					Iterator<String> iterator = availability.keys();
					do {
						if (!iterator.hasNext())
							break;
						String appId = (String) iterator.next();
						if ("APP_AVAILABLE".equals(availability
								.optString(appId))) {
							mAppAvailabityList.add(appId);
						}
					} while (true);
				} catch (JSONException e) {
					log.d("No app availabities found in receiver response: %s",
							e.getMessage());
				}
			}
		}
	}

}
