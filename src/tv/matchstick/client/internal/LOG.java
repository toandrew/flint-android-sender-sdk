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

import android.util.Log;

public class LOG {

	private static boolean DEBUG = false;

	private final String TAG;
	private boolean mDebugEnabled;
	private boolean isVerbose;
	private String prefixMsg;

	public LOG(String tag, boolean enable) {
		this.TAG = tag;
		this.mDebugEnabled = enable;
		this.isVerbose = false;
	}

	public LOG(String tag) {
		this(tag, isDefaultDebugable());
	}

	public void setPrefixMsg(String msg) {
		this.prefixMsg = String.format("[%s] ", msg);
	}

	public boolean isDebugEnabled() {
		return this.mDebugEnabled;
	}

	public boolean printVerbose() {
		return this.isVerbose;
	}

	public void v(String message, Object... args) {
		if (!printVerbose()) {
			return;
		}

		Log.v(TAG, format(message, args));
	}

	public void d(String message, Object... args) {
		if (!isDebugEnabled() && !DEBUG) {
			return;
		}

		Log.d(TAG, format(message, args));
	}

	public void dd(Throwable t, String message, Object... args) {
		if (!isDebugEnabled() && !DEBUG) {
			return;
		}

		Log.d(TAG, format(message, args), t);
	}

	public void i(String message, Object... args) {
		Log.i(TAG, format(message, args));
	}

	public final void w(Throwable t, String message, Object... args) {
		Log.w(TAG, String.format(message, args), t);
	}

	public void w(String message, Object... args) {
		Log.w(TAG, format(message, args));
	}

	public void e(String message, Object... args) {
		Log.e(TAG, format(message, args));
	}

	public final void e(Throwable t, String message, Object... args) {
		Log.e(TAG, String.format(message, args), t);
	}

	private String format(String message, Object... args) {
		String msg = String.format(message, args);
		if (this.prefixMsg != null) {
			msg = this.prefixMsg + msg;
		}

		return msg;
	}

	public static boolean isDefaultDebugable() {
		return DEBUG;
	}

	public static void setDebugEnabledByDefault(boolean flag) {
		DEBUG = flag;
	}

	public final void setDebugEnabled(boolean flag) {
		mDebugEnabled = flag;
	}
}
