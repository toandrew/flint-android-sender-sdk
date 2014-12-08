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

import java.util.Map;

import tv.matchstick.client.common.internal.safeparcel.ParcelRead;
import tv.matchstick.client.common.internal.safeparcel.ParcelWrite;
import tv.matchstick.client.common.internal.safeparcel.SafeParcelable;
import tv.matchstick.client.common.internal.safeparcel.ParcelRead.ReadParcelException;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Application meta data.
 * 
 * Contains media meta data of the receiver application, supplied in
 * {@link Flint.ApplicationConnectionResult}
 */
public final class ApplicationMetadata implements SafeParcelable {
    /**
     * Parcelable creator
     */
    public static final Parcelable.Creator<ApplicationMetadata> CREATOR = new Parcelable.Creator<ApplicationMetadata>() {

        @Override
        public ApplicationMetadata createFromParcel(Parcel source) {

            int size = ParcelRead.readStart(source);
            Map<String, String> add = null;
            while (source.dataPosition() < size) {
                int type = ParcelRead.readInt(source);
                switch (ParcelRead.halfOf(type)) {
                case 1:
                    add = ParcelRead.readHashMap(source, type);
                    break;
                default:
                    ParcelRead.skip(source, type);
                }
            }

            if (source.dataPosition() != size) {
                throw new ReadParcelException("Overread allowed size end="
                        + size, source);
            }

            return new ApplicationMetadata(add);
        }

        @Override
        public ApplicationMetadata[] newArray(int size) {
            // TODO Auto-generated method stub

            return new ApplicationMetadata[size];
        }
    };

    Map<String, String> mAdditionalData;

    /**
     * ApplicationMetadata constructor.
     * 
     * @param additionalData
     *             additional data from receiver
     */
    public ApplicationMetadata(Map<String, String> additionalData) {
        this.mAdditionalData = additionalData;
    }

    public Map<String, String> getData() {
        return mAdditionalData;
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
     * Build application meta parcel data
     * 
     * @param applicationmetadata
     * @param out
     * @param flags
     */
    private void buildParcel(Parcel out, int flags) {
        int i = ParcelWrite.position(out);
        ParcelWrite.write(out, 1, mAdditionalData, false);
        ParcelWrite.writeEnd(out, i);
    }
}
