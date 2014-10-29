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

package tv.matchstick.server.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tv.matchstick.client.internal.LOG;
import tv.matchstick.fling.images.WebImage;
import tv.matchstick.server.common.checker.PlatformChecker;

public final class ApplicationInfo {
    private static final LOG log = new LOG("ApplicationInfo");
    private String appId;
    private String displayName;
    private String transportId;
    private String sessionId;
    private String statusText;
    private List<String> namespaces;
    private final List senderApps;
    private final List<WebImage> appImages;

    private ApplicationInfo() {
        senderApps = new ArrayList();
        appImages = new ArrayList<WebImage>();
    }

    public ApplicationInfo(JSONObject obj) {
        this();
        try {
            appId = obj.getString("appId");
            sessionId = obj.getString("sessionId");
            transportId = obj.optString("transportId");
            displayName = obj.optString("displayName");
            statusText = obj.optString("statusText");
            if (obj.has("appImages")) {
                JSONArray images = obj.getJSONArray("appImages");
                int len = images.length();
                int i = 0;
                while (i < len) {
                    JSONObject img = images.getJSONObject(i);
                    try {
                        appImages.add(new WebImage(img));
                    } catch (IllegalArgumentException e) {
                        log.w(e, "Ignoring invalid image structure");
                    }
                    i++;
                }
            }

            if (obj.has("senderApps")) {
                JSONArray apps = obj.getJSONArray("senderApps");
                int len = apps.length();
                int i = 0;
                do {
                    if (i >= len)
                        break;
                    try {
                        PlatformChecker checker = new PlatformChecker(
                                apps.getJSONObject(i));
                        senderApps.add(checker);
                    } catch (JSONException e) {
                        log.w("Ignorning invalid sender app structure: %s",
                                e.getMessage());
                    }
                    i++;
                } while (true);
            }
            if (obj.has("namespaces")) {
                JSONArray ns = obj.getJSONArray("namespaces");
                int i = ns.length();
                if (i > 0) {
                    namespaces = new ArrayList();
                    for (int j = 0; j < i; j++)
                        namespaces.add(ns.getString(j));

                }
            }
        } catch (Exception e) {
        }
    }

    public final String getApplicationId() {
        return appId;
    }

    public final String getDisplayName() {
        return displayName;
    }

    public final String getTransportId() {
        return transportId;
    }

    public final PlatformChecker getPlatformChecker() {
        for (Iterator iterator = senderApps.iterator(); iterator.hasNext();) {
            PlatformChecker checker = (PlatformChecker) iterator.next();
            if (checker.mPlatform == 1)
                return checker;
        }

        return null;
    }

    public final String getStatusText() {
        return statusText;
    }

    public final List<String> getNamespaces() {
        return Collections.unmodifiableList(namespaces);
    }

    public final List<WebImage> getAppImages() {
        return Collections.unmodifiableList(appImages);
    }

    public final String getSessionId() {
        return sessionId;
    }
}
