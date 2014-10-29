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

package tv.matchstick.server.common.checker;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

/**
 * Platform validate checker
 */
public final class PlatformChecker {
    public int mPlatform;
    public String mPackage;
    public Uri mUri;

    public PlatformChecker(JSONObject obj) {
        try {
            int platform = obj.getInt("platform");
            if (platform < 0 || platform > 3) {
                throw new JSONException("Invalid value for 'platform'");
            }

            mPlatform = platform;
            mPackage = obj.getString("package");
            mUri = Uri.parse(obj.getString("url"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
