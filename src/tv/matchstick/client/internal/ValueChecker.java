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

public class ValueChecker {

    public static <T> T checkNullPointer(T obj) {
        if (obj == null)
            throw new NullPointerException("null reference");
        return obj;
    }

    public static <T> T checkNullPointer(T obj, Object msg) {
        if (obj == null)
            throw new NullPointerException(String.valueOf(msg));
        return obj;
    }

    public static void checkTrue(boolean val) {
        if (val) {
            return;
        }
        throw new IllegalStateException();
    }

    public static void checkTrueWithErrorMsg(boolean val, Object msg) {
        if (val) {
            return;
        }
        throw new IllegalStateException(String.valueOf(msg));
    }

}
