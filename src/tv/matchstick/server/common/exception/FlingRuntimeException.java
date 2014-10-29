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

package tv.matchstick.server.common.exception;

import android.os.Parcel;

/**
 * Fling Runtime exception.
 */
public final class FlingRuntimeException extends RuntimeException {
    /**
     * auto gen version UID.
     */
    private static final long serialVersionUID = 3065715124018403311L;

    public FlingRuntimeException(String message, Parcel data) {
        super((new StringBuilder()).append(message).append(" Parcel: pos=")
                .append(data.dataPosition()).append(" size=")
                .append(data.dataSize()).toString());
    }
}
