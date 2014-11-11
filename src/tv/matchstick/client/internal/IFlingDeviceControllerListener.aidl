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
import tv.matchstick.fling.ApplicationMetadata;

interface IFlingDeviceControllerListener
{
	void onDisconnected(int statusCode);

	void onApplicationConnected(
			in ApplicationMetadata applicationMetadata, String statusText,
			boolean relaunched);

	void postApplicationConnectionResult(int statusCode);

	void notifyApplicationStatusOrVolumeChanged(String status,
			double volume, boolean muted);

	void onMessageReceived(String namespace, String message);

	void onReceiveBinary(String namespace, in byte[] binary);

	void onRequestResult(int result);

	void onRequestStatus(int status);

	void onApplicationDisconnected(int statusCode);

	void requestCallback(String namespace, long requestId,
			int statusCode);
}
