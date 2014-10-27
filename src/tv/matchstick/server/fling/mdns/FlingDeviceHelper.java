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

package tv.matchstick.server.fling.mdns;

import java.net.Inet4Address;

import tv.matchstick.fling.FlingDevice;

public final class FlingDeviceHelper {
	public final FlingDevice mFlingDevice;

	public FlingDeviceHelper(FlingDevice flingdevice, String deviceId,
			Inet4Address inet4address) {
		super();
		mFlingDevice = flingdevice;
		FlingDevice.setHost(flingdevice, inet4address);

		String address = null;
		if (FlingDevice.getHost(flingdevice) != null) {
			address = FlingDevice.getHost(flingdevice).getHostAddress();
		}

		flingdevice.mHostAddress = address;
		FlingDevice.setDeviceId(flingdevice, deviceId);
	}
}
