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

package tv.matchstick.fling;

/**
 * All Fling status
 */
public final class FlingStatusCodes {
    /**
     * Operation success
     */
    public static final int SUCCESS = 0;

    /**
     * Network error
     */
    public static final int NETWORK_ERROR = 7;

    /**
     * Internal software error
     */
    public static final int INTERNAL_ERROR = 8;

    /**
     * Unknown error
     */
    public static final int UNKNOWN_ERROR = 13;

    /**
     * Interrupted error
     */
    public static final int INTERRUPTED = 14;

    /**
     * Timeout error
     */
    public static final int TIMEOUT = 15;

    /**
     * Authentication failed
     */
    public static final int AUTHENTICATION_FAILED = 2000;

    /**
     * Invalid request
     */
    public static final int INVALID_REQUEST = 2001;

    /**
     * Operation is canceled
     */
    public static final int CANCELED = 2002;

    /**
     * Operation is not allowed
     */
    public static final int NOT_ALLOWED = 2003;

    /**
     * Application not found
     */
    public static final int APPLICATION_NOT_FOUND = 2004;

    /**
     * Application not running
     */
    public static final int APPLICATION_NOT_RUNNING = 2005;

    /**
     * Message is too large
     */
    public static final int MESSAGE_TOO_LARGE = 2006;

    /**
     * The send message buffer is full
     */
    public static final int MESSAGE_SEND_BUFFER_TOO_FULL = 2007;
}
