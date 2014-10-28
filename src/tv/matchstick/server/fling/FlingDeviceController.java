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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import tv.matchstick.client.internal.FlingChannel;
import tv.matchstick.client.internal.LOG;
import tv.matchstick.client.internal.MessageSender;
import tv.matchstick.fling.ApplicationMetadata;
import tv.matchstick.fling.FlingDevice;
import tv.matchstick.fling.FlingStatusCodes;
import tv.matchstick.fling.service.FlingDeviceService;
import tv.matchstick.server.common.checker.PlatformChecker;
import tv.matchstick.server.common.exception.FlingMessageTooLargeException;
import tv.matchstick.server.fling.bridge.IFlingSrvController;
import tv.matchstick.server.fling.channels.ConnectionControlChannel;
import tv.matchstick.server.fling.channels.HeartbeatChannel;
import tv.matchstick.server.fling.channels.ReceiverControlChannel;
import tv.matchstick.server.fling.socket.FlingSocket;
import tv.matchstick.server.fling.socket.FlingSocketListener;
import tv.matchstick.server.fling.socket.data.BinaryPayload;
import tv.matchstick.server.fling.socket.data.FlingMessage;
import tv.matchstick.server.utils.ApplicationInfo;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;

public final class FlingDeviceController implements FlingSocketListener {
	private static AtomicLong ID = new AtomicLong(0L);

	private final Runnable mHeartbeatRunnable = new Runnable() {

		@Override
		public void run() {
			long time = SystemClock.elapsedRealtime();
			if (mHeartbeatChannel != null && mHeartbeatChannel.isTimeout(time)) {
				log.d("disconnecting due to heartbeat timeout");
				onSocketError(FlingStatusCodes.TIMEOUT);// 15
				return;
			}

			if (mReceiverControlChannel != null) {
				mReceiverControlChannel.mLaunchRequestTracker
						.trackRequestTimeout(time, 15);
				mReceiverControlChannel.mStopSessionRequestTracker
						.trackRequestTimeout(time, 15);
				mReceiverControlChannel.mGetStatusRequestTracker
						.trackRequestTimeout(time, 15);
				mReceiverControlChannel.mSetVolumeRequestTracker
						.trackRequestTimeout(time, 0);
				mReceiverControlChannel.mSetMuteRequestTracker
						.trackRequestTimeout(time, 0);
			}

			mHandler.postDelayed(mHeartbeatRunnable, 1000L);
		}

	};

	private boolean mIsConnecting;
	private boolean mIsConnected;
	private boolean mDisposed;
	private int mDisconnectStatusCode;
	private long mDebugLevel;
	private final ReconnectStrategy mReconnectStrategy = new ReconnectStrategy();

	private final Runnable mReconnectRunnable = new Runnable() {

		@Override
		public void run() {
			if (mReconnectStrategy.wasReconnecting()) {
				log.d("in reconnect Runnable");

				connectToDeviceInternal();
			}
		}

	};

	private String mLastApplicationId;
	private String mLastSessionId;
	private final LOG log = new LOG("FlingDeviceController");
	private Integer controlerId;
	private final Context mContext;
	private final Handler mHandler;
	private final FlingDevice mFlingDevice;
	private final IFlingSrvController mFlingSrvController;
	private final FlingSocket mFlingSocket;
	private AtomicLong mRequestIdGen;

	private final MessageSender mMsgSender = new MessageSender() {

		@Override
		public long getRequestId() {
			return mRequestIdGen.incrementAndGet();
		}

		@Override
		public void sendTextMessage(String namespace, String message, long id,
				String transportId) {
			String transId;
			if (transportId == null) {
				transId = getTransId();
			} else {
				transId = transportId;
			}

			FlingDeviceService
					.sendTextMessage(mContext, FlingDeviceController.this,
							namespace, message, id, transId);
		}
	};

	private final ReceiverControlChannel mReceiverControlChannel = new ReceiverControlChannel(
			"receiver-0") {

		@Override
		protected void onRequestStatus(int result) {
			mFlingSrvController.onRequestStatus(result);
		}

		@Override
		protected void onConnectToApplicationAndNotify(ApplicationInfo appInfo) {
			connectToApplicationAndNotify(appInfo, true);
		}

		@Override
		protected void onStatusReceived(ApplicationInfo appInfo, double level,
				boolean muted) {
			log.d("onStatusReceived");

			mVolumeLevel = level;

			processReceiverStatus(appInfo, level, muted);
		}

		@Override
		protected void onApplicationDisconnected() {
			int result;
			if (v)
				result = 0;
			else
				result = FlingStatusCodes.APPLICATION_NOT_RUNNING; // 2005;
			v = false;
			mLastApplicationId = null;
			mLastSessionId = null;

			onApplicationDisconnectedInternal(result);
		}

		@Override
		protected void onApplicationConnectionFailed(int result) {
			mFlingSrvController.onApplicationConnectionFailed(result);
		}

		@Override
		protected void onStatusRequestFailed(int statusCode) {
			log.d("onStatusRequestFailed: statusCode=%d", statusCode);

			if (mApplicationId != null) {
				if (mReconnectStrategy.reset()) {
					log.d("calling Listener.onConnectedWithoutApp()");

					mFlingSrvController.onConnectedWithoutApp();
				} else {
					mFlingSrvController
							.onApplicationConnectionFailed(statusCode);
				}
				mApplicationId = null;
				mSessionId_y = null;
			}
		}

	};

	private final ConnectionControlChannel mConnectionControlChannel;
	private HeartbeatChannel mHeartbeatChannel;
	private final Set mNamespaces = new HashSet();
	private final Map<String, FlingChannel> mFlingChannelMap = new HashMap<String, FlingChannel>();
	private final String mSourceId;
	private double mVolumeLevel;
	private ApplicationMetadata mApplicationMetadata;
	private String mSessionId;
	private String mStatusText;
	private boolean v;
	private boolean w;
	private String mApplicationId;
	private String mSessionId_y;

	private static FlingDeviceController mFlingDeviceController;

	private FlingDeviceController(Context context, Handler handler,
			FlingDevice device, String packageName, long debugLevel,
			IFlingSrvController controller) {
		controlerId = 0;
		mContext = context;
		mHandler = handler;
		mFlingDevice = device;
		mFlingSrvController = controller;
		setDebugLevel(debugLevel);
		mDisconnectStatusCode = 0;
		mFlingSocket = new FlingSocket(context, this);
		setSender(mReceiverControlChannel);
		mConnectionControlChannel = new ConnectionControlChannel(packageName);
		setSender(mConnectionControlChannel);

		mSourceId = String.format("%s-%d", packageName, ID.incrementAndGet());
	}

	public static FlingDeviceController create(Context context,
			Handler handler, String packageName, FlingDevice device,
			long debugLevel, IFlingSrvController ctrl) {
		FlingDeviceController controller = new FlingDeviceController(context,
				handler, device, packageName, debugLevel, ctrl);
		controller.generateId();

		mFlingDeviceController = controller;
		return controller;
	}

	public static FlingDeviceController getCurrentController() {
		return mFlingDeviceController;
	}

	private void connectToApplicationAndNotify(ApplicationInfo applicationInfo,
			boolean relaunched) {
		log.d("connectToApplicationAndNotify");

		String transportId = applicationInfo.getTransportId();

		try {
			mConnectionControlChannel.connect(transportId);
		} catch (Exception e) {
			mReceiverControlChannel.setTransportId(null);
			onSocketError(FlingStatusCodes.NETWORK_ERROR);
			return;
		}

		log.d("setting current transport ID to %s", transportId);

		mReceiverControlChannel.setTransportId(transportId);

		PlatformChecker checker = applicationInfo.getPlatformChecker();
		String senderAppIdentifier = null;
		Uri senderAppLaunchUrl = null;
		if (checker != null) {
			senderAppIdentifier = checker.mPackage;
			senderAppLaunchUrl = checker.mUri;
		}
		mStatusText = applicationInfo.getStatusText();
		mApplicationMetadata = new ApplicationMetadata(1,
				applicationInfo.getApplicationId(),
				applicationInfo.getDisplayName(),
				applicationInfo.getAppImages(),
				applicationInfo.getNamespaces(), senderAppIdentifier,
				senderAppLaunchUrl);
		mSessionId = applicationInfo.getSessionId();
		mLastApplicationId = applicationInfo.getApplicationId();
		mLastSessionId = mSessionId;
		if (mReconnectStrategy.reset()) {
			mHandler.removeCallbacks(mReconnectRunnable);

			mFlingSrvController.onConnected();
		} else {
			mFlingSrvController.onApplicationConnected(mApplicationMetadata,
					mStatusText, mSessionId, relaunched);
		}
	}

	private void processReceiverStatus(ApplicationInfo applicationInfo,
			double volume, boolean muteState) {
		log.d("processReceiverStatus: applicationInfo=%s, volume=%f, muteState=%b",
				applicationInfo, volume, muteState);

		String statusText;
		if (applicationInfo != null) {
			statusText = applicationInfo.getStatusText();
		} else {
			statusText = null;
		}

		mStatusText = statusText;

		mFlingSrvController.onVolumeChanged(mStatusText, volume, muteState);

		if (mApplicationId != null) {
			String applicationId;

			String appId;
			if (applicationInfo != null) {
				applicationId = applicationInfo.getApplicationId();
			} else {
				applicationId = null;
			}

			if (mApplicationId.equals("")
					|| mApplicationId.equals(applicationId)) {
				if (mSessionId_y != null
						&& !mSessionId_y.equals(applicationInfo.getSessionId())) {
					mApplicationId = null;
					mSessionId_y = null;
					if (w) {
						w = false;
						launchApplicationInternal(mApplicationId,
								((String) (null)), true);
						return;
					} else {
						mFlingSrvController
								.onApplicationConnectionFailed(FlingStatusCodes.APPLICATION_NOT_RUNNING);
						return;
					}
				}
				String status;
				if (applicationInfo != null)
					status = applicationInfo.getStatusText();
				else
					status = null;
				mStatusText = status;
				if (applicationInfo != null
						&& !TextUtils.isEmpty(applicationInfo.getTransportId())) {
					connectToApplicationAndNotify(applicationInfo, false);
				} else {
					IFlingSrvController controller = mFlingSrvController;
					char result;
					if ("".equals(mApplicationId))
						result = '\u07D5';
					else
						result = '\u07D4';

					controller.onApplicationConnectionFailed(result);
				}
				mApplicationId = null;
				mSessionId_y = null;
				w = false;
				return;
			}
			log.d("application to join (%s) is NOT available!", mApplicationId);
			appId = mApplicationId;
			mApplicationId = null;
			mSessionId_y = null;
			mLastApplicationId = null;
			mLastSessionId = null;
			if (!w) {
				if (mReconnectStrategy.reset()) {
					mFlingSrvController.onConnectedWithoutApp();
					return;
				} else {
					log.d("calling mListener.onApplicationConnectionFailed");

					mFlingSrvController
							.onApplicationConnectionFailed(FlingStatusCodes.APPLICATION_NOT_RUNNING);

					return;
				}
			}
			w = false;
			launchApplicationInternal(appId, ((String) (null)), true);
		} else {
			log.d("processReceiverStatus_a:ignore status messages for application id is null!");
		}

		return;
	}

	private void sendMessage(ByteBuffer message, String namespace, long id)
			throws IOException, RemoteException {
		try {
			mFlingSocket.send(message);

			mFlingSrvController.onRequestCallback(namespace, id, 0);
		} catch (FlingMessageTooLargeException e) {
			mFlingSrvController.onRequestCallback(namespace, id,
					FlingStatusCodes.MESSAGE_TOO_LARGE); // 2006
		}
	}

	private void handleConnectionFailure(boolean flag) {
		mIsConnecting = false;
		boolean wasReconnecting = mReconnectStrategy.wasReconnecting();
		log.d("handleConnectionFailure; wasReconnecting=%b", wasReconnecting);
		if (flag) {
			long time = mReconnectStrategy.getCurrentReconnectTime();
			if (time >= 0L) {
				mHandler.postDelayed(mReconnectRunnable, time);
				return;
			}
		} else {
			mReconnectStrategy.reset();
		}

		if (wasReconnecting) {
			mFlingSrvController.onDisconnected(mDisconnectStatusCode);

			mDisconnectStatusCode = 0;
			return;
		} else {
			mFlingSrvController.onConnectionFailed();
			return;
		}
	}

	private void onApplicationDisconnectedInternal(int statusCode) {
		if (mReceiverControlChannel.mTransportId != null) {
			try {
				mConnectionControlChannel
						.close(mReceiverControlChannel.mTransportId);
			} catch (Exception e) {
				log.w(e, "Error while leaving application");
				onSocketError(FlingStatusCodes.NETWORK_ERROR);
			}
			mReceiverControlChannel.setTransportId(null);
		}
		if (mApplicationMetadata != null) {
			mApplicationMetadata = null;
			mSessionId = null;
		}

		mFlingSrvController.onApplicationDisconnected(statusCode);
	}

	private void finishDisconnecting(int socketError) {
		log.d("finishDisconnecting; socketError=%d, mDisconnectStatusCode=%d",
				socketError, mDisconnectStatusCode);

		mIsConnecting = false;
		mIsConnected = false;
		mVolumeLevel = 0.0D;
		mApplicationMetadata = null;
		mSessionId = null;
		mStatusText = null;
		mHandler.removeCallbacks(mHeartbeatRunnable);
		int disconnectStatusCode;
		if (mDisconnectStatusCode != 0) {
			disconnectStatusCode = mDisconnectStatusCode;
			mDisconnectStatusCode = FlingStatusCodes.SUCCESS;
		} else if (socketError == 3)
			disconnectStatusCode = FlingStatusCodes.TIMEOUT;
		else if (socketError == 4)
			disconnectStatusCode = FlingStatusCodes.AUTHENTICATION_FAILED;
		else
			disconnectStatusCode = FlingStatusCodes.NETWORK_ERROR;
		mReconnectStrategy.reset();

		mFlingSrvController.onDisconnected(disconnectStatusCode);
	}

	private void onSocketError(int socketError) {
		if (mFlingSocket.isConnected()) {
			mDisconnectStatusCode = socketError;
			mFlingSocket.disconnect();
			return;
		}

		finishDisconnecting(socketError);
	}

	private void finishConnecting() {
		log.d("finishConnecting");
		mRequestIdGen = new AtomicLong(0L);
		try {
			mConnectionControlChannel.connect("receiver-0");
		} catch (Exception e) {
			onSocketError(FlingStatusCodes.NETWORK_ERROR);// 7
			return;
		}

		mHeartbeatChannel = new HeartbeatChannel();
		setSender(mHeartbeatChannel);
		mHandler.postDelayed(mHeartbeatRunnable, 1000L);
		mIsConnected = true;
		mIsConnecting = false;
		if (mLastApplicationId != null && mLastSessionId != null) {
			w = false;
			joinApplicationInternal(mLastApplicationId, mLastSessionId);
			return;
		}
		mReconnectStrategy.reset();

		mFlingSrvController.onConnected();

		try {
			mReceiverControlChannel.getStatus();
			return;
		} catch (Exception e) {
			onSocketError(FlingStatusCodes.NETWORK_ERROR); // 7
		}
	}

	@Override
	public final void onConnected() {
		FlingDeviceService.onSocketConnected(mContext, this);
	}

	public final void setVolume(double volume, double expected_level,
			boolean flag) {
		FlingDeviceService.setVolume(mContext, this, volume, expected_level,
				flag);
	}

	@Override
	public final void onConnectionFailed(int socketError) {
		log.d("onConnectionFailed; socketError=%d", socketError);
		FlingDeviceService
				.onSocketConnectionFailed(mContext, this, socketError);
	}

	public final void setDebugLevel(long level) {
		if (mDebugLevel == level) {
			return;
		}

		mDebugLevel = level;
		boolean enable;
		if ((1L & mDebugLevel) != 0L)
			enable = true;
		else
			enable = false;
		log.setDebugEnabled(enable);
		LOG.setDebugEnabledByDefault(enable);
	}

	public final void setSender(FlingChannel flingChannel) {
		flingChannel.setMessageSender(mMsgSender);
		mFlingChannelMap.put(flingChannel.getNamespace(), flingChannel);
	}

	public final void reconnectToDevice(String lastApplicationId,
			String lastSessionId) {
		log.d("reconnectToDevice: lastApplicationId=%s, lastSessionId=%s",
				lastApplicationId, lastSessionId);
		mLastApplicationId = lastApplicationId;
		mLastSessionId = lastSessionId;

		connectDevice();
	}

	public final void sendMessageInternal(String namespace, String message,
			long id) {
		FlingDeviceService.sendTextMessage(mContext, this, namespace, message,
				id, mReceiverControlChannel.mTransportId);
	}

	public final void sendTextMessage(String namespace, String message,
			long id, String transId) {
		if (TextUtils.isEmpty(transId)) {
			log.w("ignoring attempt to send a text message with no destination ID");

			mFlingSrvController.onRequestCallback(namespace, id,
					FlingStatusCodes.INVALID_REQUEST);

			return;
		}
		if (transId == null) {
			try {
				throw new IllegalStateException(
						"The application has not launched yet.");
			} catch (Exception e) {
				log.w(e, "Error while sending message", new Object[0]);
			}

			onSocketError(FlingStatusCodes.NETWORK_ERROR);

			return;
		}

		FlingMessage msg = new FlingMessage();
		msg.setProtocolVersion(0);
		msg.setSourceId(mSourceId);
		msg.setDestinationId(transId);
		msg.setNamespace(namespace);
		msg.setPayloadMessage(message);

		try {
			byte bytes[] = msg.buildJson().toString().getBytes("UTF-8");
			if (bytes.length > 0x10000) {
				throw new FlingMessageTooLargeException();
			}

			sendMessage(ByteBuffer.wrap(bytes), namespace, id);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public final void launchApplication(String applicationId, String sessionId,
			boolean relaunchIfRunning) {
		FlingDeviceService.launchApplication(mContext, this, applicationId,
				sessionId, relaunchIfRunning);
	}

	public final void sendBinaryMessage(String nameSpace, byte message[],
			long requestId, String transId) throws IOException {
		if (TextUtils.isEmpty(transId)) {
			log.w("ignoring attempt to send a binary message with no destination ID");

			mFlingSrvController.onRequestCallback(nameSpace, requestId,
					FlingStatusCodes.INVALID_REQUEST);
			return;
		}
		if (transId == null) {
			try {
				throw new IllegalStateException(
						"The application has not launched yet.");
			} catch (Exception e) {
				log.w(e, "Error while sending message");
			}

			onSocketError(FlingStatusCodes.NETWORK_ERROR);
			return;
		}
		FlingMessage msg = new FlingMessage();
		msg.setProtocolVersion(0);
		msg.setSourceId(mSourceId);
		msg.setDestinationId(transId);
		msg.setNamespace(nameSpace);
		msg.setPayloadBinary(BinaryPayload.a(message));
		byte bytes[] = msg.buildJson().toString().getBytes("UTF-8");
		try {
			sendMessage(ByteBuffer.wrap(bytes), nameSpace, requestId);
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		return;
	}

	@Override
	public final void onMessageReceived(ByteBuffer message) {
		FlingDeviceService.procReceivedMessage(mContext, this, message);
	}

	public final void setMute(boolean mute, double level, boolean isMuted) {
		FlingDeviceService.setMute(mContext, this, mute, level, isMuted);
	}

	public final void connectDevice() {
		if (mReconnectStrategy.wasReconnecting()) {
			log.d("already reconnecting; ignoring");
			return;
		}

		log.d("calling FlingMediaRouteProviderService.connectToDevice");
		FlingDeviceService.connectFlingDevice(mContext, this);
	}

	public final void setVolumeInternal(double level, double expected_level,
			boolean muted) {
		try {
			mReceiverControlChannel.setVolume(level, expected_level, muted);
			return;
		} catch (Exception e) {
			log.w(e, "Error while setting volume");
		}

		onSocketError(FlingStatusCodes.NETWORK_ERROR);
	}

	@Override
	public final void onDisconnected(int reason) {
		FlingDeviceService.onSocketDisconnected(mContext, this, reason);

		if (mHeartbeatChannel != null) {
			removeSender(((FlingChannel) (mHeartbeatChannel)));
			mHeartbeatChannel = null;
		}

		mReceiverControlChannel.mTransportId = null;
		mReceiverControlChannel.mLaunchRequestTracker.clear();
		mReceiverControlChannel.mStopSessionRequestTracker.clear();
		mReceiverControlChannel.mGetStatusRequestTracker.clear();
		mReceiverControlChannel.mSetVolumeRequestTracker.clear();
		mReceiverControlChannel.mSetMuteRequestTracker.clear();
		mReceiverControlChannel.mLevel = 0.0D;
		mReceiverControlChannel.mMuted = false;
		mReceiverControlChannel.mFirst = false;
	}

	public final void removeSender(FlingChannel channel) {
		channel.setMessageSender((MessageSender) null);
		mFlingChannelMap.remove(channel.getNamespace());
	}

	public final void stopApplication(String sessionId) {
		FlingDeviceService.stopApplication(mContext, this, sessionId);
	}

	public final void joinApplication(String applicationId, String sessionId) {
		FlingDeviceService.joinApplication(mContext, this, applicationId,
				sessionId);
	}

	public final void launchApplicationInternal(String appId, String param,
			boolean relaunch) {
		log.d("launchApplicationInternal() id=%s, relaunch=%b", appId, relaunch);
		if (appId == null || appId.equals("")) {
			mFlingSrvController
					.onApplicationConnectionFailed(FlingStatusCodes.APPLICATION_NOT_FOUND);
			return;
		}
		if (relaunch) {
			try {
				mReceiverControlChannel.launchApplication(appId, param);
				return;
			} catch (Exception e) {
				log.w(e, "Error while launching application");
			}
			onSocketError(FlingStatusCodes.NETWORK_ERROR);
			return;
		}

		w = true;
		joinApplicationInternal(appId, null);
		return;
	}

	public final void onReceivedMessage(ByteBuffer received) {
		if (mHeartbeatChannel != null) {
			mHeartbeatChannel.reset();
		}

		FlingMessage message = new FlingMessage(received.array());
		String nameSpace = message.getNamespace();
		if (TextUtils.isEmpty(nameSpace)) {
			log.d("Received a message with an empty or missing namespace");
			return;
		}

		int payloadType = message.getPayloadType();
		FlingChannel flingChannel = (FlingChannel) mFlingChannelMap
				.get(nameSpace);
		if (flingChannel != null) {
			BinaryPayload binaryPayload;
			switch (payloadType) {
			default:
				log.w("Unknown payload type %s; discarding message",
						payloadType);
				return;

			case 0:
				flingChannel.onMessageReceived(message.getMessage());
				return;

			case 1:
				binaryPayload = message.getBinaryMessage();
				break;
			}
			byte bytes[] = new byte[binaryPayload.getLength()];
			binaryPayload.copy(bytes);
			flingChannel.onMessageReceived(bytes);
			return;
		}

		boolean registeredNamespace = false;
		synchronized (mNamespaces) {
			registeredNamespace = mNamespaces.contains(nameSpace);
		}

		if (registeredNamespace) {
			BinaryPayload binary;
			switch (payloadType) {
			default:
				log.w("Unknown payload type %s; discarding message",
						payloadType);
				return;

			case 0:
				mFlingSrvController.notifyOnMessageReceived(nameSpace,
						message.getMessage());
				return;

			case 1:
				binary = message.getBinaryMessage();
				break;
			}
			byte bytes[] = new byte[binary.getLength()];
			binary.copy(bytes);

			mFlingSrvController.onReceiveBinary(nameSpace, bytes);

			return;
		}

		log.w("Ignoring message. Namespace has not been registered.");
	}

	public final void setMuteInternal(boolean mute, double level,
			boolean isMuted) {
		try {
			mReceiverControlChannel.setMute(mute, level, isMuted);
			return;
		} catch (Exception e) {
			log.w(e, "Error while setting mute state");
		}

		onSocketError(FlingStatusCodes.NETWORK_ERROR);
	}

	public final void connectToDeviceInternal() {
		int socketState = mFlingSocket.getState();
		log.d("connectToDeviceInternal; socket state = %d", socketState);
		if (socketState == 1 || socketState == 2) {
			log.w("Redundant call to connect to device");
			return;
		}
		mIsConnecting = true;
		if (!mReconnectStrategy.wasReconnecting()) {
			mReconnectStrategy.markConnectTime();
		}

		mReconnectStrategy.markStartConnectTime();
		try {
			log.d("connecting socket now");
			mFlingSocket.connect(mFlingDevice.getIpAddress(),
					mFlingDevice.getServicePort());
			return;
		} catch (Exception e) {
			log.w(e, "connection exception");
		}

		handleConnectionFailure(true);
	}

	public final void onSocketConnectionFailedInternal(int socketError) {
		log.d("onSocketConnectionFailedInternal: socketError=%d", socketError);
		handleConnectionFailure(true);
	}

	public final void stopApplicationInternal(String sessionId) {
		log.d("stopApplicationInternal() sessionId=%s", sessionId);
		try {
			v = true;
			mReceiverControlChannel.stopSession(sessionId);
			return;
		} catch (Exception e) {
			log.w(e, "Error while stopping application");
		}

		onSocketError(FlingStatusCodes.NETWORK_ERROR);
	}

	public final void joinApplicationInternal(String applicationId,
			String sessionId) {
		log.d("joinApplicationInternal(%s, %s)", applicationId, sessionId);

		if (mApplicationMetadata != null) {
			if (applicationId == null
					|| applicationId.equals(mApplicationMetadata
							.getApplicationId())
					&& (sessionId == null || sessionId.equals(mSessionId))) {
				log.d("already connected to requested app, so skipping join logic");

				mFlingSrvController.onApplicationConnected(
						mApplicationMetadata, mStatusText, mSessionId, false);
				return;
			}
			log.d("clearing mLastConnected* variables");
			mLastApplicationId = null;
			mLastSessionId = null;
			if (mReconnectStrategy.reset()) {
				mFlingSrvController.onConnectedWithoutApp();
				return;
			} else {
				mFlingSrvController
						.onApplicationConnectionFailed(FlingStatusCodes.APPLICATION_NOT_RUNNING);
				return;
			}
		}

		if (applicationId == null) {
			applicationId = "";
		}

		mApplicationId = applicationId;
		mSessionId_y = sessionId;
		try {
			mReceiverControlChannel.getStatus();
			return;
		} catch (Exception e) {
			log.w(e, "Error while requesting device status for join");
		}

		onSocketError(FlingStatusCodes.NETWORK_ERROR);
	}

	public final void onSocketDisconnectedInternal(int socketError) {
		log.d("onSocketDisconnectedInternal: socketError=%d", socketError);
		finishDisconnecting(socketError);
	}

	public final void setMessageReceivedCallbacks(String namespace) {
		FlingDeviceService.setMessageReceivedCallbacks(mContext, this,
				namespace);
	}

	public final boolean isConnected() {
		return mIsConnected;
	}

	public final void addNamespace(String namespace) {
		if (TextUtils.isEmpty(namespace)) {
			return;
		}

		synchronized (mNamespaces) {
			mNamespaces.add(namespace);
		}
	}

	public final boolean isConnecting() {
		return mIsConnecting;
	}

	public final double getVolume() {
		return mVolumeLevel;
	}

	public final void removeMessageReceivedCallbacks(String namespace) {
		FlingDeviceService.removeMessageReceivedCallbacks(mContext, this,
				namespace);
	}

	public final String getStatusText() {
		return mStatusText;
	}

	public final void removeNamespace(String namespace) {
		if (TextUtils.isEmpty(namespace)) {
			return;
		}

		synchronized (mNamespaces) {
			mNamespaces.remove(namespace);
		}
	}

	public final long getDebugLevel() {
		return mDebugLevel;
	}

	public final boolean isDisposed() {
		return mDisposed;
	}

	public final void leaveApplication() {
		FlingDeviceService.leaveApplication(mContext, this);
	}

	public final void leaveApplicationInternal() {
		log.d("leaveApplicationInternal()");
		if (mApplicationMetadata == null) {

			mFlingSrvController.onInvalidRequest();

			return;
		} else {
			onApplicationDisconnectedInternal(0);
			return;
		}
	}

	public final void requestStatus() {
		FlingDeviceService.requestStatus(mContext, this);
	}

	public final void getStatus() {
		try {
			mReceiverControlChannel.getStatus();
			return;
		} catch (Exception e) {
			log.w(e, "Error while stopping application");
		}

		onSocketError(FlingStatusCodes.NETWORK_ERROR);
	}

	public final void onSocketConnectedInternal() {
		log.d("onSocketConnectedInternal");
		finishConnecting();
	}

	public final String getTransId() {
		return mReceiverControlChannel.mTransportId;
	}

	public final void generateId() {
		synchronized (controlerId) {
			controlerId = Integer.valueOf(1 + controlerId.intValue());
		}
	}

	public final void releaseReference() {
		synchronized (controlerId) {
			boolean flag;
			if (controlerId.intValue() <= 0) {
				log.e("unbalanced call to releaseReference(); mDisposed=%b",
						mDisposed);
				flag = false;
			} else {
				Integer integer;
				integer = Integer.valueOf(-1 + controlerId.intValue());
				controlerId = integer;
				if (integer.intValue() == 0)
					flag = true;
				else
					flag = false;
			}

			if (flag) {
				mDisposed = true;

				log.d("[%s] *** disposing ***", mFlingDevice);
				mReconnectStrategy.reset();
				mHandler.removeCallbacks(mHeartbeatRunnable);
				mHandler.removeCallbacks(mReconnectRunnable);
				if (mFlingSocket.isConnected()) {
					mFlingSocket.disconnect();
				}
			}
		}
	}

	final class ReconnectStrategy {
		private final long mTimeout;
		private final long mMaxTimeout;
		private long mStartConnectTime;
		private long startReconnectTime;

		private ReconnectStrategy() {
			mTimeout = 3000L;
			mMaxTimeout = 15000L;
		}

		public final void markConnectTime() {
			long time = SystemClock.elapsedRealtime();
			mStartConnectTime = time;
			startReconnectTime = time;
		}

		public final boolean reset() {
			boolean isReconnecting = false;
			if (startReconnectTime != 0L) {
				isReconnecting = true;
			}

			startReconnectTime = 0L;
			mStartConnectTime = 0L;
			return isReconnecting;
		}

		public final boolean wasReconnecting() {
			return startReconnectTime != 0L;
		}

		public final long getCurrentReconnectTime() {
			long delay = -1L;
			if (startReconnectTime != 0L) {
				long time = SystemClock.elapsedRealtime();
				if (mStartConnectTime == 0L) {
					return 0L;
				}

				if (time - startReconnectTime >= mMaxTimeout) {
					startReconnectTime = 0L;
					return delay;
				}

				delay = mTimeout - (time - mStartConnectTime);
				if (delay <= 0L)
					return 0L;
			}
			return delay;
		}

		public final void markStartConnectTime() {
			if (startReconnectTime != 0L) {
				mStartConnectTime = SystemClock.elapsedRealtime();
			}
		}
	}
}
