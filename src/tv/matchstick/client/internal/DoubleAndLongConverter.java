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

public class DoubleAndLongConverter {

    public static <T> boolean compare(T one, T other) {
        return (((one == null) && (other == null)) || ((one != null)
                && (other != null) && (one.equals(other))));
    }

    public static double long2double(long l) {
        return (l / 1000.0D);
    }

    public static long double2long(double d) {
        return (long) (d * 1000.0D);
    }
}
