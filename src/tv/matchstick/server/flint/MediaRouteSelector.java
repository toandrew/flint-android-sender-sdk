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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.os.Bundle;

public final class MediaRouteSelector {
    public static final MediaRouteSelector EMPTY = new MediaRouteSelector(
            new Bundle(), null);
    private final Bundle mData;
    private List mControlCategories;

    public MediaRouteSelector(Bundle bundle, List controlCategories) {
        mData = bundle;
        mControlCategories = controlCategories;
    }

    public static MediaRouteSelector fromBundle(Bundle data) {
        if (data != null) {
            return new MediaRouteSelector(data, null);
        }

        return null;
    }

    static void ensureControlCategories(MediaRouteSelector selector) {
        selector.ensureControlCategories();
    }

    static List getControlCategories(MediaRouteSelector selector) {
        return selector.mControlCategories;
    }

    private void ensureControlCategories() {
        if (mControlCategories == null) {
            mControlCategories = mData.getStringArrayList("controlCategories");
            if (mControlCategories == null || mControlCategories.isEmpty()) {
                mControlCategories = Collections.emptyList();
            }
        }
    }

    public final List getControlCategories() {
        ensureControlCategories();
        return mControlCategories;
    }

    public final boolean isEmpty() {
        ensureControlCategories();
        return mControlCategories.isEmpty();
    }

    public final boolean isValid() {
        ensureControlCategories();
        return !mControlCategories.contains(null);
    }

    public final Bundle asBundle() {
        return mData;
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof MediaRouteSelector) {
            MediaRouteSelector other = (MediaRouteSelector) obj;
            ensureControlCategories();
            other.ensureControlCategories();
            return mControlCategories.equals(other.mControlCategories);
        } else {
            return false;
        }
    }

    @Override
    public final int hashCode() {
        ensureControlCategories();
        return mControlCategories.hashCode();
    }

    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MediaRouteSelector{ ");
        sb.append("controlCategories=").append(
                Arrays.toString(getControlCategories().toArray()));
        sb.append(" }");

        return sb.toString();
    }
}
