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

package tv.matchstick.client.common.internal.safeparcel;

import java.util.ArrayList;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

public class ParcelRead {

    public static int readInt(Parcel data) {
        return data.readInt();
    }

    public static int halfOf(int i) {
        return (i & 0xFFFF);
    }

    public static int readStart(Parcel data, int first) {
        if ((first & 0xFFFF0000) != -65536)
            return (first >> 16 & 0xFFFF);
        return data.readInt();
    }

    public static void skip(Parcel data, int position) {
        int i = readStart(data, position);
        data.setDataPosition(data.dataPosition() + i);
    }

    private static void readStart(Parcel data, int position, int length) {
        int i = readStart(data, position);
        if (i == length)
            return;
        throw new ReadParcelException("Expected size " + length + " got " + i
                + " (0x" + Integer.toHexString(i) + ")", data);
    }

    public static int readStart(Parcel data) {
        int first = readInt(data);
        int length = readStart(data, first);
        int start = data.dataPosition();
        if (halfOf(first) != SafeParcelable.SAFE_PARCEL_MAGIC)
            throw new ReadParcelException("Expected object header. Got 0x"
                    + Integer.toHexString(first), data);
        int end = start + length;
        if ((end < start) || (end > data.dataSize()))
            throw new ReadParcelException("Size read is invalid start=" + start
                    + " end=" + end, data);
        return end;
    }

    public static boolean readBool(Parcel data, int position) {
        readStart(data, position, 4);
        return (data.readInt() != 0);
    }

    public static byte readByte(Parcel data, int position) {
        readStart(data, position, 4);
        return (byte) data.readInt();
    }

    public static short readShort(Parcel data, int position) {
        readStart(data, position, 4);
        return (short) data.readInt();
    }

    public static int readInt(Parcel data, int position) {
        readStart(data, position, 4);
        return data.readInt();
    }

    public static long readLong(Parcel data, int position) {
        readStart(data, position, 8);
        return data.readLong();
    }

    public static float readFloat(Parcel data, int position) {
        readStart(data, position, 4);
        return data.readFloat();
    }

    public static double readDouble(Parcel data, int position) {
        readStart(data, position, 8);
        return data.readDouble();
    }

    public static String readString(Parcel data, int position) {
        int length = readStart(data, position);
        int start = data.dataPosition();
        if (length == 0)
            return null;
        String str = data.readString();
        data.setDataPosition(start + length);
        return str;
    }

    public static IBinder readBinder(Parcel data, int position) {
        int length = readStart(data, position);
        int start = data.dataPosition();
        if (length == 0)
            return null;
        IBinder binder = data.readStrongBinder();
        data.setDataPosition(start + length);
        return binder;
    }

    public static <T extends Parcelable> T readParcelable(Parcel data,
            int position, Parcelable.Creator<T> creator) {
        int length = readStart(data, position);
        int start = data.dataPosition();
        if (length == 0)
            return null;
        T t = creator.createFromParcel(data);
        data.setDataPosition(start + length);

        return t;
    }

    public static Bundle readBundle(Parcel data, int position) {
        int length = readStart(data, position);
        int start = data.dataPosition();
        if (length == 0)
            return null;
        Bundle bundle = data.readBundle();
        data.setDataPosition(start + length);
        return bundle;
    }

    public static ArrayList<String> readStringList(Parcel data, int position) {
        int length = readStart(data, position);
        int start = data.dataPosition();
        if (length == 0)
            return null;
        ArrayList<String> list = data.createStringArrayList();
        data.setDataPosition(start + length);
        return list;
    }

    public static <T> ArrayList<T> readCreatorList(Parcel data, int position,
            Parcelable.Creator<T> creator) {
        int length = readStart(data, position);
        int start = data.dataPosition();
        if (length == 0)
            return null;
        ArrayList<T> list = data.createTypedArrayList(creator);
        data.setDataPosition(start + length);
        return list;
    }

    public static class ReadParcelException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public ReadParcelException(String str, Parcel data) {
            super(str + " Parcel: pos=" + data.dataPosition() + " size="
                    + data.dataSize());
        }
    }

}
