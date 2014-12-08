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

package tv.matchstick.flint;

import java.util.Collection;
import java.util.Iterator;

import android.text.TextUtils;

/**
 * Flint media control intent.
 * 
 * Intent constants for use with the Flint MediaRouteProvider. <br>
 * This class also contains utility methods for creating a control category for
 * discovering Flint media routes that support a specific app and/or set of
 * namespaces, to be used with MediaRouteSelector.
 */
public class FlintMediaControlIntent {
    /**
     * Flint category
     */
    public static final String CATEGORY_FLINT = "tv.matchstick.flint.CATEGORY_FLINT";

    /**
     * Remote media playback category
     */
    public static final String CATEGORY_REMOTE_PLAYBACK = "tv.matchstick.flint.CATEGORY_FLINT_REMOTE_PLAYBACK";

    /**
     * Get flint category with application Id.
     * 
     * Returns a custom control category for discovering Flint devices that
     * support running the specified app, independent of whether the app is
     * running or not.
     * 
     * @param applicationId
     *            application's Id
     * @return flint category
     * @throws IllegalArgumentException
     */
    public static String categoryForFlint(String applicationId)
            throws IllegalArgumentException {
        if (applicationId == null) {
            throw new IllegalArgumentException("applicationId cannot be null");
        }
        return buildCategory(CATEGORY_FLINT, applicationId, null);
    }

    /**
     * Get media remote playback category.
     * 
     * Returns a custom control category for discovering Flint devices which
     * support the default Android remote playback actions using the specified
     * Flint player.
     * 
     * @param applicationId
     *            application's id
     * @return media remote playback category
     * @throws IllegalArgumentException
     */
    public static String categoryForRemotePlayback(String applicationId)
            throws IllegalArgumentException {
        if (TextUtils.isEmpty(applicationId)) {
            throw new IllegalArgumentException(
                    "applicationId cannot be null or empty");
        }
        return buildCategory(CATEGORY_REMOTE_PLAYBACK, applicationId, null);
    }

    /**
     * Get default remote media playback category.
     * 
     * Returns a custom control category for discovering Flint devices which
     * support the Default Media Receiver.
     * 
     * @return media remote playback category
     */
    public static String categoryForRemotePlayback() {
        return buildCategory(CATEGORY_REMOTE_PLAYBACK, null, null);
    }

    /**
     * Build category with category, application id and namespace list.
     * 
     * @param category
     *            the specific category
     * @param applicationId
     *            application Id
     * @param namespaces
     *            namespace list
     * @return category
     * @throws IllegalArgumentException
     */
    private static String buildCategory(String category, String applicationId,
            Collection<String> namespaces) throws IllegalArgumentException {
        StringBuffer sb = new StringBuffer(category);
        if (applicationId != null) {
            sb.append("/").append(applicationId);
        }
        if (namespaces != null) {
            if (namespaces.isEmpty()) {
                throw new IllegalArgumentException(
                        "Must specify at least one namespace");
            }
            Iterator<String> nss = namespaces.iterator();
            while (nss.hasNext()) {
                String namespace = (String) nss.next();
                if ((TextUtils.isEmpty(namespace))
                        || (namespace.trim().equals(""))) {
                    throw new IllegalArgumentException(
                            "Namespaces must not be null or empty");
                }
            }
            if (applicationId == null) {
                sb.append("/");
            }
            sb.append("/").append(TextUtils.join(",", namespaces));
        }
        return sb.toString();
    }
}
