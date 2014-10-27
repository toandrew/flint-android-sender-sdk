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

import tv.matchstick.fling.FlingDevice;
import tv.matchstick.server.fling.media.RouteController;

public final class FlingRouteController extends RouteController {
	private final FlingDevice mFlingDevice;
	private final FlingMediaRouteProvider mFlingMediaRouteProvider;

	public FlingRouteController(FlingMediaRouteProvider provider,
			FlingDevice flingdevice) {
		super();
		mFlingMediaRouteProvider = provider;
		mFlingDevice = flingdevice;
	}

	@Override
	public final void onRelease() {
		FlingMediaRouteProvider.log().d("Controller released", new Object[0]);
	}

	@Override
	public final void onSelect() {
		FlingMediaRouteProvider.log().d("onSelect");
		mFlingMediaRouteProvider.setDeviceControllerListener(this);
	}

	@Override
	public final void onUnselect() {
		FlingMediaRouteProvider.log().d("onUnselect");
		mFlingMediaRouteProvider.onUnselect(this);
	}

	public FlingDevice getFlingDevice() {
		return mFlingDevice;
	}
}
