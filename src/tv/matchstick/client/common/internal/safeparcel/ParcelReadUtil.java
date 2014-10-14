package tv.matchstick.client.common.internal.safeparcel;

import java.util.ArrayList;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

/*
 * OK
 */
public class ParcelReadUtil {

    public static int readSingleInt(Parcel parcel) {
        return parcel.readInt();
    }

    public static int halfOf(int i) {
        return (i & 0xFFFF);
    }

    public static int readStart(Parcel parcel, int first) {
        if ((first & 0xFFFF0000) != -65536)
            return (first >> 16 & 0xFFFF);
        return parcel.readInt();
    }

    public static void skip(Parcel parcel, int position) {
        int i = readStart(parcel, position);
        parcel.setDataPosition(parcel.dataPosition() + i);
    }

    private static void readStart(Parcel parcel, int position, int length) {
        int i = readStart(parcel, position);
        if (i == length)
            return;
        throw new SafeParcel("Expected size " + length + " got " + i
                + " (0x" + Integer.toHexString(i) + ")", parcel);
    }

    public static int readStart(Parcel parcel) {
        int first = readSingleInt(parcel);
        int length = readStart(parcel, first);
        int start = parcel.dataPosition();
        if (halfOf(first) != SafeParcelable.SAFE_PARCEL_MAGIC)
            throw new SafeParcel("Expected object header. Got 0x"
                    + Integer.toHexString(first), parcel);
        int end = start + length;
        if ((end < start) || (end > parcel.dataSize()))
            throw new SafeParcel("Size read is invalid start=" + start + " end="
                    + end, parcel);
        return end;
    }

    public static boolean readBool(Parcel parcel, int position) {
        readStart(parcel, position, 4);
        return (parcel.readInt() != 0);
    }

    public static byte readByte(Parcel parcel, int position) {
        readStart(parcel, position, 4);
        return (byte) parcel.readInt();
    }

    public static short readShort(Parcel parcel, int position) {
        readStart(parcel, position, 4);
        return (short) parcel.readInt();
    }

    public static int readInt(Parcel parcel, int position) {
        readStart(parcel, position, 4);
        return parcel.readInt();
    }

    public static long readLong(Parcel parcel, int position) {
        readStart(parcel, position, 8);
        return parcel.readLong();
    }

    public static float readFloat(Parcel parcel, int position) {
        readStart(parcel, position, 4);
        return parcel.readFloat();
    }

    public static double readDouble(Parcel parcel, int position) {
        readStart(parcel, position, 8);
        return parcel.readDouble();
    }

    public static String readString(Parcel parcel, int position) {
        int length = readStart(parcel, position);
        int start = parcel.dataPosition();
        if (length == 0)
            return null;
        String str = parcel.readString();
        parcel.setDataPosition(start + length);
        return str;
    }

    public static IBinder readBinder(Parcel parcel, int position) {
        int length = readStart(parcel, position);
        int start = parcel.dataPosition();
        if (length == 0)
            return null;
        IBinder binder = parcel.readStrongBinder();
        parcel.setDataPosition(start + length);
        return binder;
    }

    public static <T extends Parcelable> T readParcelable(Parcel parcel, int position,
            Parcelable.Creator<T> creator) {
        int length = readStart(parcel, position);
        int start = parcel.dataPosition();
        if (length == 0)
            return null;
        T t = creator.createFromParcel(parcel);
        parcel.setDataPosition(start + length);

        return t;
    }

    public static Bundle readBundle(Parcel parcel, int position) {
        int length = readStart(parcel, position);
        int start = parcel.dataPosition();
        if (length == 0)
            return null;
        Bundle bundle = parcel.readBundle();
        parcel.setDataPosition(start + length);
        return bundle;
    }

    public static ArrayList<String> readStringList(Parcel parcel, int position) {
        int length = readStart(parcel, position);
        int start = parcel.dataPosition();
        if (length == 0)
            return null;
        ArrayList<String> list = parcel.createStringArrayList();
        parcel.setDataPosition(start + length);
        return list;
    }

    public static <T> ArrayList<T> readCreatorList(Parcel parcel, int position,
            Parcelable.Creator<T> creator) {
        int length = readStart(parcel, position);
        int start = parcel.dataPosition();
        if (length == 0)
            return null;
        ArrayList<T> list = parcel
                .createTypedArrayList(creator);
        parcel.setDataPosition(start + length);
        return list;
    }

    public static class SafeParcel extends RuntimeException {
        public SafeParcel(String str, Parcel parcel) {
            super(str + " Parcel: pos=" + parcel.dataPosition()
                    + " size=" + parcel.dataSize());
        }
    }

}
