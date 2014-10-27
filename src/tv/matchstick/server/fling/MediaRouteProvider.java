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

import tv.matchstick.server.fling.media.RouteController;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

/**
 * @author jianminz
 */
public abstract class MediaRouteProvider {
	public static final int MSG_DELIVER_DESCRIPTOR_CHANGED = 1;
	public static final int MSG_DELIVER_DISCOVERY_REQUEST_CHANGED = 2;

	public final Context mContext;
	final ComponentName mComponentName;

	public final Handler mHandler = new Handler() {
		public final void handleMessage(Message message) {
			switch (message.what) {
			case MSG_DELIVER_DESCRIPTOR_CHANGED:
				mPendingDescriptorChange = false;
				if (mDescriptorChangedListener != null) {
					mDescriptorChangedListener
							.onDescriptorChanged(mMediaRouteProviderDescriptor);
					return;
				}
				break;
			case MSG_DELIVER_DISCOVERY_REQUEST_CHANGED: // device discovery
														// request
				mPendingDiscoveryRequestChange = false;
				onDiscoveryRequestChanged(mDiscoveryRequest); // call
																// FlingMediaRouteProvider_awb.a
				break;
			}
		}
	};

	DescriptorChangedListener mDescriptorChangedListener;
	public DiscoveryRequest mDiscoveryRequest;
	boolean mPendingDiscoveryRequestChange;
	public MediaRouteProviderDescriptor mMediaRouteProviderDescriptor;
	public boolean mPendingDescriptorChange;

	public MediaRouteProvider(Context context) {
		if (context == null) {
			throw new IllegalArgumentException("context must not be null");
		}

		this.mContext = context;

		this.mComponentName = new ComponentName(context, getClass());
	}

	public RouteController getRouteController(String routeId) {
		return null;
	}

	public void onDiscoveryRequestChanged(DiscoveryRequest request) {
	}
}
