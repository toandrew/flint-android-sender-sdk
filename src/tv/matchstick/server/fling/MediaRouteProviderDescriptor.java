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
		check_b();
		return this.mMediaRouteDescriptorList;
	}

	private void check_b() {
		if (mMediaRouteDescriptorList == null) {
			ArrayList arraylist = mRoutes.getParcelableArrayList("routes");
			if (arraylist == null || arraylist.isEmpty()) {
				mMediaRouteDescriptorList = Collections.emptyList();
			} else {
				int i = arraylist.size();
				mMediaRouteDescriptorList = new ArrayList(i);
				int j = 0;
				while (j < i) {
					List list = mMediaRouteDescriptorList;
					Bundle bundle = (Bundle) arraylist.get(j);
					MediaRouteDescriptor ns1;
					if (bundle != null)
						ns1 = new MediaRouteDescriptor(bundle, null);
					else
						ns1 = null;
					list.add(ns1);
					j++;
				}
			}
		}
	}

	private boolean isValid() {
		check_b();
		int i = this.mMediaRouteDescriptorList.size();
		for (int j = 0; j < i; j++) {
			MediaRouteDescriptor localns = (MediaRouteDescriptor) this.mMediaRouteDescriptorList
					.get(j);
			if ((localns == null) || (!localns.isValid()))
				return false;
		}
		return true;
	}

	public final String toString() {
		StringBuilder localStringBuilder = new StringBuilder();
		localStringBuilder.append("MediaRouteProviderDescriptor{ ");
		localStringBuilder.append("routes=").append(
				Arrays.toString(getMediaRouteDescriptorList().toArray()));
		localStringBuilder.append(", isValid=").append(isValid());
		localStringBuilder.append(" }");
		return localStringBuilder.toString();
	}
}
