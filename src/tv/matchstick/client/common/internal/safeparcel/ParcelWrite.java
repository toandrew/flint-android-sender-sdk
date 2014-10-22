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

import java.util.List;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

public class ParcelWrite {
	private static void writeStart(Parcel data, int position, int length) {
		if (length >= 65535) {
			data.writeInt(0xFFFF0000 | position);
			data.writeInt(length);
		} else {
			data.writeInt(length << 16 | position);
		}
	}

	private static int writeStart(Parcel data, int position) {
		data.writeInt(0xFFFF0000 | position);
		data.writeInt(0);
		return data.dataPosition();
	}

	public static void writeEnd(Parcel data, int start) {
		int end = data.dataPosition();
		int length = end - start;
		data.setDataPosition(start - 4);
		data.writeInt(length);
		data.setDataPosition(end);
	}

	public static int position(Parcel data) {
		return writeStart(data, SafeParcelable.SAFE_PARCEL_MAGIC);
	}

	public static void write(Parcel data, int position, boolean val) {
		writeStart(data, position, 4);
		data.writeInt((val) ? 1 : 0);
	}

	public static void write(Parcel data, int position, byte val) {
		writeStart(data, position, 4);
		data.writeInt(val);
	}

	public static void write(Parcel data, int position, short val) {
		writeStart(data, position, 4);
		data.writeInt(val);
	}

	public static void write(Parcel data, int position, int val) {
		writeStart(data, position, 4);
		data.writeInt(val);
	}

	public static void write(Parcel data, int position, long val) {
		writeStart(data, position, 8);
		data.writeLong(val);
	}

	public static void write(Parcel data, int position, float val) {
		writeStart(data, position, 4);
		data.writeFloat(val);
	}

	public static void write(Parcel data, int position, double val) {
		writeStart(data, position, 8);
		data.writeDouble(val);
	}

	public static void write(Parcel data, int position, String val,
			boolean mayNull) {
		if (val == null) {
			if (mayNull)
				writeStart(data, position, 0);
			return;
		}
		int start = writeStart(data, position);
		data.writeString(val);
		writeEnd(data, start);
	}

	public static void write(Parcel data, int position, IBinder val,
			boolean mayNull) {
		if (val == null) {
			if (mayNull)
				writeStart(data, position, 0);
			return;
		}
		int start = writeStart(data, position);
		data.writeStrongBinder(val);
		writeEnd(data, start);
	}

	public static void write(Parcel data, int position, Parcelable val,
			int flags, boolean mayNull) {
		if (val == null) {
			if (mayNull)
				writeStart(data, position, 0);
			return;
		}
		int start = writeStart(data, position);
		val.writeToParcel(data, flags);
		writeEnd(data, start);
	}

	public static void write(Parcel data, int position, Bundle val,
			boolean mayNull) {
		if (val == null) {
			if (mayNull)
				writeStart(data, position, 0);
			return;
		}
		int start = writeStart(data, position);
		data.writeBundle(val);
		writeEnd(data, start);
	}

	public static void writeStringList(Parcel data, int position,
			List<String> val, boolean mayNull) {
		if (val == null) {
			if (mayNull)
				writeStart(data, position, 0);
			return;
		}
		int start = writeStart(data, position);
		data.writeStringList(val);
		writeEnd(data, start);
	}

	public static <T extends Parcelable> void write(Parcel data, int position,
			List<T> val, boolean mayNull) {
		if (val == null) {
			if (mayNull)
				writeStart(data, position, 0);
			return;
		}
		int start = writeStart(data, position);
		data.writeInt(val.size());
		for (T t : val) {
			if (t == null)
				data.writeInt(0);
			else
				writeArrayPart(data, t, 0);
		}
		writeEnd(data, start);
	}

	private static <T extends Parcelable> void writeArrayPart(Parcel data,
			T val, int flags) {
		int before = data.dataPosition();
		data.writeInt(1);
		int start = data.dataPosition();
		val.writeToParcel(data, flags);
		int end = data.dataPosition();
		data.setDataPosition(before);
		data.writeInt(end - start);
		data.setDataPosition(end);
	}
}
