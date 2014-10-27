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

package tv.matchstick.server.fling.bridge;

import tv.matchstick.client.internal.IFlingDeviceController;
import tv.matchstick.fling.service.FlingService;
import tv.matchstick.server.fling.FlingDeviceController;

public final class FlingDeviceControllerStubImpl extends
		IFlingDeviceController.Stub {
	final FlingService mFlingService;
	private final FlingDeviceController mFlingDeviceController;

	public FlingDeviceControllerStubImpl(FlingService service,
			FlingDeviceController controller) {
		super();
		mFlingService = service;
		mFlingDeviceController = controller;
	}

	public final void disconnect() {
		if (!mFlingDeviceController.isDisposed())
			mFlingDeviceController.releaseReference();
	}

	public final void setVolume(double volume, double expected_level,
			boolean flag) {
		mFlingDeviceController.setVolume(volume, expected_level, flag);
	}

	public final void stopApplication(String sessionId) {
		mFlingDeviceController.stopApplication(sessionId);
	}

	public final void joinApplication(String applicationId, String sessionId) {
		mFlingDeviceController.joinApplication(applicationId, sessionId);
	}

	public final void sendMessage(String namespace, String message, long id) {
		if (namespace == null || namespace.length() > 128) {
			return;
		}

		mFlingDeviceController.sendMessageInternal(namespace, message, id);
	}

	public final void launchApplication(String applicationId,
			boolean relaunchIfRunning) {
		mFlingDeviceController.launchApplication(applicationId, null,
				relaunchIfRunning);
	}

	public final void sendBinaryMessage(String namespace, byte message[],
			long requestId) {
		if (namespace == null || namespace.length() > 128) {
			return;
		}

		mFlingDeviceController.sendBinaryMessage(namespace, message, requestId);
	}

	public final void setMute(boolean flag, double d, boolean flag1) {
		mFlingDeviceController.setMute(flag, d, flag1);
	}

	public final void leaveApplication() {
		mFlingDeviceController.leaveApplication();
	}

	public final void setMessageReceivedCallbacks(String namespace) {
		mFlingDeviceController.setMessageReceivedCallbacks(namespace);
	}

	public final void requestStatus() {
		mFlingDeviceController.requestStatus();
	}

	public final void removeMessageReceivedCallbacks(String s) {
		mFlingDeviceController.removeMessageReceivedCallbacks(s);
	}
}
