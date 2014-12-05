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

import android.os.Bundle;

/**
 * Discovery request
 */
public final class DiscoveryRequest {
    private final Bundle mData;
    private MediaRouteSelector mMediaRouteSelector;

    DiscoveryRequest(Bundle bundle) {
        mData = bundle;
    }

    public DiscoveryRequest(MediaRouteSelector selector, boolean activeScan) {
        if (selector == null) {
            throw new IllegalArgumentException("selector must not be null");
        }

        mData = new Bundle();
        mMediaRouteSelector = selector;
        mData.putBundle("selector", selector.asBundle());
        mData.putBoolean("activeScan", activeScan);
    }

    private void ensureSelector() {
        if (mMediaRouteSelector == null) {
            mMediaRouteSelector = MediaRouteSelector.fromBundle(mData
                    .getBundle("selector"));
            if (mMediaRouteSelector == null) {
                mMediaRouteSelector = MediaRouteSelector.EMPTY;
            }
        }
    }

    public final MediaRouteSelector getSelector() {
        ensureSelector();
        return mMediaRouteSelector;
    }

    public final boolean isActiveScan() {
        return mData.getBoolean("activeScan");
    }

    public final boolean isValid() {
        ensureSelector();
        return mMediaRouteSelector.isValid();
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof DiscoveryRequest) {
            DiscoveryRequest other = (DiscoveryRequest) o;
            return getSelector().equals(other.getSelector())
                    && isActiveScan() == other.isActiveScan();
        }

        return false;
    }

    @Override
    public final int hashCode() {
        return getSelector().hashCode() ^ (isActiveScan() ? 1 : 0);
    }

    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DiscoveryRequest{ selector=").append(getSelector());
        sb.append(", activeScan=").append(isActiveScan());
        sb.append(", isValid=").append(isValid());
        sb.append(" }");

        return sb.toString();
    }
}
