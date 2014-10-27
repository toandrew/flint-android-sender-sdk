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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.os.Bundle;
import android.text.TextUtils;

/**
 * @author jianminz
 */
public final class MediaRouteDescriptor {
	final Bundle mData;
	private List mControlFilters;

	MediaRouteDescriptor(Bundle data, List filter) {
		this.mData = data;
		this.mControlFilters = filter;
	}

	private String getId() {
		return this.mData.getString("id");
	}

	private String getName() {
		return this.mData.getString("name");
	}

	private void getControlFilters() {
		if (this.mControlFilters == null) {
			this.mControlFilters = this.mData
					.getParcelableArrayList("controlFilters");
			if (this.mControlFilters == null) {
				this.mControlFilters = Collections.emptyList();
			}
		}
	}

	public final boolean isValid() {
		getControlFilters();
		return (!TextUtils.isEmpty(getId())) && (!TextUtils.isEmpty(getName()))
				&& (!this.mControlFilters.contains(null));
	}

	public final String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("MediaRouteDescriptor{ ");
		sb.append("id=").append(getId());
		sb.append(", name=").append(getName());
		sb.append(", description=").append(this.mData.getString("status"));
		sb.append(", isEnabled=")
				.append(this.mData.getBoolean("enabled", true));
		sb.append(", isConnecting=").append(
				this.mData.getBoolean("connecting", false));
		StringBuilder sb2 = sb.append(", controlFilters=");
		getControlFilters();
		sb2.append(Arrays.toString(this.mControlFilters.toArray()));
		sb.append(", playbackType=").append(
				this.mData.getInt("playbackType", 1));
		sb.append(", playbackStream=").append(
				this.mData.getInt("playbackStream", -1));
		sb.append(", volume=").append(this.mData.getInt("volume"));
		sb.append(", volumeMax=").append(this.mData.getInt("volumeMax"));
		sb.append(", volumeHandling=").append(
				this.mData.getInt("volumeHandling", 0));
		sb.append(", presentationDisplayId=").append(
				this.mData.getInt("presentationDisplayId", -1));
		sb.append(", extras=").append(this.mData.getBundle("extras"));
		sb.append(", isValid=").append(isValid());
		sb.append(" }");

		return sb.toString();
	}
}
