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

import tv.matchstick.client.common.api.StatusCodes;
import tv.matchstick.client.common.internal.safeparcel.ParcelRead;
import tv.matchstick.client.common.internal.safeparcel.ParcelWrite;
import tv.matchstick.client.common.internal.safeparcel.SafeParcelable;
import tv.matchstick.client.internal.MyStringBuilder;
import android.app.PendingIntent;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents the results of work.
 */
public class Status implements Result, SafeParcelable {
    /**
     * Success Status
     */
    public static final Status SuccessStatus = new Status(0, null, null);

    /**
     * Interrupted Status
     */
    public static final Status InterruptedStatus = new Status(14, null, null);

    /**
     * Timeout Status
     */
    public static final Status TimeOutStatus = new Status(15, null, null);

    /**
     * Status Creator
     */
    public static final Parcelable.Creator<Status> CREATOR = new Parcelable.Creator<Status>() {

        @Override
        public Status createFromParcel(Parcel source) {
            // TODO Auto-generated method stub

            int size = ParcelRead.readStart(source);
            int version = 0;
            int status = 0;
            String statusMessage = null;
            PendingIntent intent = null;
            while (source.dataPosition() < size) {
                int type = ParcelRead.readInt(source);
                switch (ParcelRead.halfOf(type)) {
                case 1:
                    status = ParcelRead.readInt(source, type);
                    break;
                case 1000:
                    version = ParcelRead.readInt(source, type);
                    break;
                case 2:
                    statusMessage = ParcelRead.readString(source, type);
                    break;
                case 3:
                    intent = (PendingIntent) ParcelRead.readParcelable(source,
                            type, PendingIntent.CREATOR);
                    break;
                default:
                    ParcelRead.skip(source, type);
                }
            }

            if (source.dataPosition() != size) {
                throw new ParcelRead.ReadParcelException(
                        "Overread allowed size end=" + size, source);
            }

            return new Status(version, status, statusMessage, intent);
        }

        @Override
        public Status[] newArray(int size) {
            // TODO Auto-generated method stub

            return new Status[size];
        }

    };

    /**
     * Client's version code
     */
    private final int mVersionCode;

    /**
     * Status code
     */
    private final int mStatusCode;

    /**
     * Status message
     */
    private final String mStatusMessage;

    /**
     * Pending intent
     */
    private final PendingIntent mPendingIntent;

    /**
     * Fling Status constructor
     *
     * @param versionCode
     * @param statusCode
     * @param statusMessage
     * @param pendingIntent
     */
    Status(int versionCode, int statusCode, String statusMessage,
            PendingIntent pendingIntent) {
        this.mVersionCode = versionCode;
        this.mStatusCode = statusCode;
        this.mStatusMessage = statusMessage;
        this.mPendingIntent = pendingIntent;
    }

    /**
     * Create status object
     *
     * @param statusCode
     *            status code
     */
    public Status(int statusCode) {
        this(1, statusCode, null, null);
    }

    /**
     * Create Status object
     *
     * @param statusCode
     *            status code
     * @param statusMessage
     *            status message
     * @param pendingIntent
     *            pending intent
     */
    public Status(int statusCode, String statusMessage,
            PendingIntent pendingIntent) {
        this(1, statusCode, statusMessage, pendingIntent);
    }

    /**
     * Get current pending intent
     *
     * @return pending intent
     */
    PendingIntent getPendingIntent() {
        return mPendingIntent;
    }

    /**
     * Get status message
     *
     * @return status message
     */
    public String getStatusMessage() {
        return mStatusMessage;
    }

    /**
     * Get version code
     *
     * @return version code
     */
    public int getVersionCode() {
        return mVersionCode;
    }

    /**
     * Whether operation was successful
     *
     * @return status code
     */
    public boolean isSuccess() {
        return (this.mStatusCode <= 0);
    }

    /**
     * Whether operation was interrupted
     *
     * @return
     */
    public boolean isInterrupted() {
        return (this.mStatusCode == 14);
    }

    /**
     * Indicates the status of the operation.
     *
     * @return Status code
     */
    public int getStatusCode() {
        return this.mStatusCode;
    }

    @Override
    public int hashCode() {
        return MyStringBuilder.hashCode(new Object[] {
                Integer.valueOf(this.mVersionCode),
                Integer.valueOf(this.mStatusCode), this.mStatusMessage,
                this.mPendingIntent });
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Status))
            return false;
        Status status = (Status) obj;
        return ((this.mVersionCode == status.mVersionCode)
                && (this.mStatusCode == status.mStatusCode)
                && (MyStringBuilder.compare(this.mStatusMessage,
                        status.mStatusMessage)) && (MyStringBuilder.compare(
                this.mPendingIntent, status.mPendingIntent)));
    }

    /**
     * Get status message
     *
     * @return
     */
    private String getStatusMessageInternal() {
        if (this.mStatusMessage != null) {
            return this.mStatusMessage;
        }
        return StatusCodes.getStatusMessage(this.mStatusCode);
    }

    @Override
    public String toString() {
        return MyStringBuilder.newStringBuilder(this)
                .append("statusCode", getStatusMessageInternal())
                .append("resolution", this.mPendingIntent).toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        buildParcel(out, flags);
    }

    /**
     * Get status.
     *
     * implementation of Result
     */
    @Override
    public Status getStatus() {
        return this;
    }

    /**
     * Build parcel status data
     * 
     * @param out
     * @param flags
     */
    private void buildParcel(Parcel out, int flags) {
        int i = ParcelWrite.position(out);
        ParcelWrite.write(out, 1, getStatusCode());
        ParcelWrite.write(out, 1000, getVersionCode());
        ParcelWrite.write(out, 2, getStatusMessage(), false);
        ParcelWrite.write(out, 3, getPendingIntent(), flags, false);
        ParcelWrite.writeEnd(out, i);
    }
}
