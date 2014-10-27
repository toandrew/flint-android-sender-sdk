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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.os.Bundle;

public final class MediaRouteProviderDescriptor {
	final Bundle mRoutes;
	private List mMediaRouteDescriptorList;

	public MediaRouteProviderDescriptor(Bundle routes, List descriptors) {
		this.mRoutes = routes;
		this.mMediaRouteDescriptorList = descriptors;
	}

	private List getMediaRouteDescriptorList() {
		init();
		return this.mMediaRouteDescriptorList;
	}

	private void init() {
		if (mMediaRouteDescriptorList == null) {
			ArrayList routes = mRoutes.getParcelableArrayList("routes");
			if (routes == null || routes.isEmpty()) {
				mMediaRouteDescriptorList = Collections.emptyList();
			} else {
				int size = routes.size();
				mMediaRouteDescriptorList = new ArrayList(size);
				int i = 0;
				while (i < size) {
					Bundle bundle = (Bundle) routes.get(i);
					MediaRouteDescriptor desc = null;
					if (bundle != null)
						desc = new MediaRouteDescriptor(bundle, null);

					if (desc != null) {
						mMediaRouteDescriptorList.add(desc);
					}
					i++;
				}
			}
		}
	}

	private boolean isValid() {
		init();
		int size = this.mMediaRouteDescriptorList.size();
		for (int i = 0; i < size; i++) {
			MediaRouteDescriptor desc = (MediaRouteDescriptor) this.mMediaRouteDescriptorList
					.get(i);
			if ((desc == null) || (!desc.isValid()))
				return false;
		}
		return true;
	}

	@Override
	public final String toString() {
		StringBuilder dn = new StringBuilder();
		dn.append("MediaRouteProviderDescriptor{ ");
		dn.append("routes=").append(
				Arrays.toString(getMediaRouteDescriptorList().toArray()));
		dn.append(", isValid=").append(isValid());
		dn.append(" }");
		return dn.toString();
	}
}
