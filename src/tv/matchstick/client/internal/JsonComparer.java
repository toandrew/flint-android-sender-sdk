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

package tv.matchstick.client.internal;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonComparer {

    public static boolean compare(Object one, Object other) {
        if ((one instanceof JSONObject) && (other instanceof JSONObject)) {
            JSONObject jsonOne = (JSONObject) one;
            JSONObject jsonOther = (JSONObject) other;
            if (jsonOne.length() != jsonOther.length()) {
                return false;
            }
            Iterator jsonOneKeys = jsonOne.keys();
            while (jsonOneKeys.hasNext()) {
                String key = (String) jsonOneKeys.next();
                if (!(jsonOther.has(key))) {
                    return false;
                }
                try {
                    Object value = jsonOne.get(key);
                    Object other_value = jsonOther.get(key);
                    if (!compare(value, other_value)) {
                        return false;
                    }
                } catch (JSONException e) {
                    return false;
                }
            }
            return true;
        }
        if ((one instanceof JSONArray) && (other instanceof JSONArray)) {
            JSONArray jArrOne = (JSONArray) one;
            JSONArray jArrOther = (JSONArray) other;
            if (jArrOne.length() != jArrOther.length()) {
                return false;
            }
            for (int i = 0; i < jArrOne.length(); ++i) {
                try {
                    Object value = jArrOne.get(i);
                    Object value_other = jArrOther.get(i);
                    if (!compare(value, value_other)) {
                        return false;
                    }
                } catch (JSONException e) {
                    return false;
                }
            }
            return true;
        }
        return one.equals(other);
    }
}
