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

import tv.matchstick.client.internal.ValueChecker;

public final class MdnsClientPrivData {
	final byte[] a;
	final long mCurrentTime;
	final int c;

	public MdnsClientPrivData(byte[] paramArrayOfByte, int paramInt) {
		boolean bool = false;
		if (paramInt <= 0 || paramInt >= 604800) {
			bool = false;
		} else {
			bool = true;
		}

		ValueChecker.checkTrue(bool);
		this.a = paramArrayOfByte;
		this.mCurrentTime = System.currentTimeMillis();
		this.c = paramInt;
	}
}
