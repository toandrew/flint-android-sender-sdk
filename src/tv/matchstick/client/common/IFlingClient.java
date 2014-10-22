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

package tv.matchstick.client.common;

import tv.matchstick.fling.ConnectionResult;
import android.os.Bundle;

public interface IFlingClient {
	public void connect();

	public void disconnect();

	public boolean isConnected();

	public boolean isConnecting();

	public void registerConnectionCallbacks(ConnectionCallbacks callbacks);

	public boolean isConnectionCallbacksRegistered(ConnectionCallbacks callbacks);

	public void unregisterConnectionCallbacks(ConnectionCallbacks callbacks);

	public void registerConnectionFailedListener(
			OnConnectionFailedListener listener);

	public boolean isConnectionFailedListenerRegistered(
			OnConnectionFailedListener listener);

	public void unregisterConnectionFailedListener(
			OnConnectionFailedListener listener);

	public interface OnConnectionFailedListener {
		/**
		 * Called when there was an error connecting the client to the service.
		 *
		 * @param result
		 */
		public void onConnectionFailed(ConnectionResult result);
	}

	public interface ConnectionCallbacks {
		public void onConnected(Bundle paramBundle);

		public void onDisconnected();
	}

}
