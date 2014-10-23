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

import android.os.Bundle;

interface IFlingDeviceController
{
	void disconnect();

	void launchApplication(String applicationId,
			boolean relaunchIfRunning);

	void joinApplication(String applicationId, String sessionId);

	void leaveApplication();

	void stopApplication(String sessionId);

	void requestStatus();

	void setVolume(double volume, double originalVolume, boolean isMute);

	void setMute(boolean mute, double volume, boolean isMute);

	void sendMessage(String namespace, String message, long requestId);

	void setMessageReceivedCallbacks(String namespace);

	void removeMessageReceivedCallbacks(String namespace);

	void sendBinaryMessage(String namespace, in byte[] message,
			long requestId);
}
