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

package tv.matchstick.server.flint;

import tv.matchstick.flint.FlintDevice;
import tv.matchstick.server.flint.media.RouteController;

public final class FlintRouteController extends RouteController {
    private final FlintDevice mFlintDevice;
    private final FlintMediaRouteProvider mFlintMediaRouteProvider;

    public FlintRouteController(FlintMediaRouteProvider provider,
            FlintDevice flintdevice) {
        super();
        mFlintMediaRouteProvider = provider;
        mFlintDevice = flintdevice;
    }

    @Override
    public final void onRelease() {
        FlintMediaRouteProvider.log().d("Controller released", new Object[0]);
    }

    @Override
    public final void onSelect() {
        FlintMediaRouteProvider.log().d("onSelect");
        mFlintMediaRouteProvider.setDeviceControllerListener(this);
    }

    @Override
    public final void onUnselect() {
        FlintMediaRouteProvider.log().d("onUnselect");
        mFlintMediaRouteProvider.onUnselect(this);
    }

    public FlintDevice getFlintDevice() {
        return mFlintDevice;
    }
}
