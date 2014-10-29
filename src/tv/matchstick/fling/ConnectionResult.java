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

import tv.matchstick.client.internal.MyStringBuilder;
import android.app.PendingIntent;

/**
 * Fling connection result.
 *
 * Contains all possible error codes for when an error happens while connecting
 * to a device. These error codes are used by
 * {@link FlingManager.OnConnectionFailedListener}.
 */
public final class ConnectionResult {

    /**
     * The connection was successful.
     */
    public static final int SUCCESS = 0;

    /**
     * Fling Service is missing.
     */
    public static final int SERVICE_MISSING = 1;

    /**
     * Need update Fling service.
     */
    public static final int SERVICE_VERSION_UPDATE_REQUIRED = 2;

    /**
     * Fling Service is disabled.
     */
    public static final int SERVICE_DISABLED = 3;

    /**
     * Need sign in.
     */
    public static final int SIGN_IN_REQUIRED = 4;

    /**
     * Invalid account.
     */
    public static final int INVALID_ACCOUNT = 5;

    /**
     * Completing the connection requires some form of resolution.
     */
    public static final int RESOLUTION_REQUIRED = 6;

    /**
     * Network error.
     */
    public static final int NETWORK_ERROR = 7;

    /**
     * Internal software error.
     */
    public static final int INTERNAL_ERROR = 8;

    /**
     * Service is invalid.
     */
    public static final int SERVICE_INVALID = 9;

    /**
     * The application is misconfigured.
     */
    public static final int DEVELOPER_ERROR = 10;

    /**
     * Invalid license.
     */
    public static final int LICENSE_CHECK_FAILED = 11;

    /**
     * Invalid device date.
     */
    public static final int DATE_INVALID = 12;

    /**
     * The connection was canceled by calling disconnect().
     */
    public static final int CANCELED = 13;

    /**
     * An interrupt occurred while waiting for the connection complete.
     */
    public static final int INTERRUPTED = 14;

    /**
     * The timeout was exceeded while waiting for the connection to complete.
     */
    public static final int TIMEOUT = 15;

    /**
     * Connection result instance.
     */
    public static final ConnectionResult connectResult = new ConnectionResult(
            0, null);

    /**
     * Pending intent.
     */
    private final PendingIntent mPendingIntent;

    /**
     * Status code.
     */
    private final int mStatusCode;

    /**
     * Create instance with the specific status and intent
     *
     * @param statusCode
     *            The status code.
     * @param pendingIntent
     *            A pending intent that will resolve the issue when started, or
     *            null.
     */
    public ConnectionResult(int statusCode, PendingIntent pendingIntent) {
        this.mStatusCode = statusCode;
        this.mPendingIntent = pendingIntent;
    }

    /**
     * Whether the connection was successful.
     * 
     * @return true if success
     */
    public boolean isSuccess() {
        return (this.mStatusCode == 0);
    }

    /**
     * Indicates the type of error that interrupted connection.
     * 
     * @return error code
     */
    public int getErrorCode() {
        return this.mStatusCode;
    }

    /**
     * Get readable status message with current status code
     * 
     * @return readable status messages
     */
    private String getStatusMessage() {
        switch (this.mStatusCode) {
        case -1:
            return "SUCCESS_CACHE";
        case 0:
            return "SUCCESS";
        case 1:
            return "SERVICE_MISSING";
        case 2:
            return "SERVICE_VERSION_UPDATE_REQUIRED";
        case 3:
            return "SERVICE_DISABLED";
        case 4:
            return "SIGN_IN_REQUIRED";
        case 5:
            return "INVALID_ACCOUNT";
        case 6:
            return "RESOLUTION_REQUIRED";
        case 7:
            return "NETWORK_ERROR";
        case 8:
            return "INTERNAL_ERROR";
        case 9:
            return "SERVICE_INVALID";
        case 10:
            return "DEVELOPER_ERROR";
        case 11:
            return "LICENSE_CHECK_FAILED";
        case 12:
            return "DATE_INVALID";
        case 13:
            return "CANCELED";
        case 14:
            return "INTERRUPTED";
        case 15:
            return "TIMEOUT";
        }

        return "unknown status code " + this.mStatusCode;
    }

    @Override
    public String toString() {
        return MyStringBuilder.newStringBuilder(this)
                .append("statusCode", getStatusMessage())
                .append("resolution", this.mPendingIntent).toString();
    }

}
