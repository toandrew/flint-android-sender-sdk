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
import java.util.List;

public final class FlingDeviceInfoContainer {
	FlingDeviceInfo mFlingDeviceInfo;

	public FlingDeviceInfoContainer(String fqdn) {
		this.mFlingDeviceInfo = new FlingDeviceInfo(fqdn);
	}

	public final FlingDeviceInfoContainer setProto(int proto) {
		this.mFlingDeviceInfo.mProto = proto;
		return this;
	}

	final class FlingDeviceInfo {
		String mFQDN;
		List<Inet4Address> mIpV4AddrList;
		List mIpV6AddrList;
		String mName;
		String mFriendlyName;
		String mHost;
		int mProto; // 1:tcp, 2: udp
		int mPort;
		int mPriority;
		int mWeight;
		List<String> mTextStringList;
		long mTTL;

		private FlingDeviceInfo(String fqdn) {
			this.mFQDN = fqdn;
			this.mProto = 0;
			this.mTTL = -1L;
		}
	}
}
