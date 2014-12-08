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

package tv.matchstick.flint.images;

import org.json.JSONException;
import org.json.JSONObject;

import tv.matchstick.client.common.internal.safeparcel.ParcelRead;
import tv.matchstick.client.common.internal.safeparcel.ParcelWrite;
import tv.matchstick.client.common.internal.safeparcel.SafeParcelable;
import tv.matchstick.client.internal.MyStringBuilder;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Flint Image
 */
public class WebImage implements SafeParcelable {

    public static final Parcelable.Creator<WebImage> CREATOR = new Parcelable.Creator<WebImage>() {

        @Override
        public WebImage createFromParcel(Parcel source) {
            int size = ParcelRead.readStart(source);
            int version = 0;
            Uri url = null;
            int width = 0;
            int height = 0;
            while (source.dataPosition() < size) {
                int type = ParcelRead.readInt(source);
                switch (ParcelRead.halfOf(type)) {
                case 1:
                    version = ParcelRead.readInt(source, type);
                    break;
                case 2:
                    url = (Uri) ParcelRead.readParcelable(source, type,
                            Uri.CREATOR);
                    break;
                case 3:
                    width = ParcelRead.readInt(source, type);
                    break;
                case 4:
                    height = ParcelRead.readInt(source, type);
                    break;
                default:
                    ParcelRead.skip(source, type);
                }
            }

            if (source.dataPosition() != size) {
                throw new ParcelRead.ReadParcelException(
                        "Overread allowed size end=" + size, source);
            }

            return new WebImage(version, url, width, height);
        }

        @Override
        public WebImage[] newArray(int size) {
            return new WebImage[size];
        }

    };

    private final int mVersionCode;
    private final Uri mUrl;
    private final int mWidth;
    private final int mHeight;

    WebImage(int versionCode, Uri url, int width, int height) {
        this.mVersionCode = versionCode;
        this.mUrl = url;
        this.mWidth = width;
        this.mHeight = height;
    }

    public WebImage(Uri url, int width, int height)
            throws IllegalArgumentException {
        this(1, url, width, height);
        if (url == null) {
            throw new IllegalArgumentException("url cannot be null");
        }

        if ((width >= 0) && (height >= 0)) {
            return;
        }

        throw new IllegalArgumentException(
                "width and height must not be negative");
    }

    public WebImage(Uri url) throws IllegalArgumentException {
        this(url, 0, 0);
    }

    public WebImage(JSONObject json) throws IllegalArgumentException {
        this(getUriFromJson(json), json.optInt("width", 0), json.optInt(
                "height", 0));
    }

    int getVersionCode() {
        return this.mVersionCode;
    }

    private static Uri getUriFromJson(JSONObject data) {
        Uri url = null;

        if (data.has("url")) {
            try {
                url = Uri.parse(data.getString("url"));
            } catch (JSONException localJSONException) {
            }
        }

        return url;
    }

    public Uri getUrl() {
        return this.mUrl;
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    @Override
    public String toString() {
        return String.format("Image %dx%d %s", this.mWidth, this.mHeight,
                this.mUrl.toString());
    }

    public JSONObject buildJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("url", this.mUrl.toString());
            json.put("width", this.mWidth);
            json.put("height", this.mHeight);
        } catch (JSONException e) {
        }
        return json;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if ((other == null) || (!(other instanceof WebImage))) {
            return false;
        }

        WebImage localWebImage = (WebImage) other;
        return ((MyStringBuilder.compare(this.mUrl, localWebImage.mUrl))
                && (this.mWidth == localWebImage.mWidth) && (this.mHeight == localWebImage.mHeight));
    }

    @Override
    public int hashCode() {
        return MyStringBuilder.hashCode(new Object[] { this.mUrl,
                Integer.valueOf(this.mWidth), Integer.valueOf(this.mHeight) });
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        buildParcel(out, flags);
    }

    private void buildParcel(Parcel out, int flags) {
        int i = ParcelWrite.position(out);
        ParcelWrite.write(out, 1, getVersionCode());
        ParcelWrite.write(out, 2, getUrl(), flags, false);
        ParcelWrite.write(out, 3, getWidth());
        ParcelWrite.write(out, 4, getHeight());
        ParcelWrite.writeEnd(out, i);
    }
}
