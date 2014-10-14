package tv.matchstick.fling;

import java.util.ArrayList;
import java.util.List;

import tv.matchstick.client.common.internal.safeparcel.ParcelReadUtil;
import tv.matchstick.fling.images.WebImage;
import tv.matchstick.server.common.exception.FlingRuntimeException;
import tv.matchstick.server.common.internal.safeparcel.ParcelWritter;
import android.os.Parcel;
import android.os.Parcelable.Creator;

/**
 * Internal Fling device creator
 */
final class FlingDeviceCreator implements Creator {

    public FlingDeviceCreator() {
    }

    public static void buildParcel(FlingDevice flingdevice, Parcel parcel) {
        int i = ParcelWritter.a(parcel, 20293);
        ParcelWritter.b(parcel, 1, flingdevice.getVersionCode());
        ParcelWritter.a(parcel, 2, flingdevice.getDeviceId(), false);
        ParcelWritter.a(parcel, 3, flingdevice.mHostAddress, false);
        ParcelWritter.a(parcel, 4, flingdevice.getFriendlyName(), false);
        ParcelWritter.a(parcel, 5, flingdevice.getModelName(), false);
        ParcelWritter.a(parcel, 6, flingdevice.getDeviceVersion(), false);
        ParcelWritter.b(parcel, 7, flingdevice.getServicePort());
        ParcelWritter.b(parcel, 8, flingdevice.getIcons(), false);
        ParcelWritter.b(parcel, i);
    }

    public final Object createFromParcel(Parcel parcel) {
        int servicePor = 0;
        ArrayList<WebImage> icons = null;
        int length = ParcelReadUtil.readStart(parcel);
        String deviceVersion = null;
        String modelName = null;
        String friendlyName = null;
        String hostAddress = null;
        String deviceId = null;
        int versionCode = 0;
        do
            if (parcel.dataPosition() < length) {
                int position = parcel.readInt();
                switch (0xffff & position) {
                default:
                    ParcelReadUtil.skip(parcel, position);
                    break;

                case 1: // '\001'
                    versionCode = ParcelReadUtil.readInt(parcel, position);
                    break;

                case 2: // '\002'
                    deviceId = ParcelReadUtil.readString(parcel, position);
                    break;

                case 3: // '\003'
                    hostAddress = ParcelReadUtil.readString(parcel, position);
                    break;

                case 4: // '\004'
                    friendlyName = ParcelReadUtil.readString(parcel, position);
                    break;

                case 5: // '\005'
                    modelName = ParcelReadUtil.readString(parcel, position);
                    break;

                case 6: // '\006'
                    deviceVersion = ParcelReadUtil.readString(parcel, position);
                    break;

                case 7: // '\007'
                    servicePor = ParcelReadUtil.readInt(parcel, position);
                    break;

                case 8: // '\b'
                    icons = ParcelReadUtil.readCreatorList(parcel, position,
                            WebImage.CREATOR);
                    break;
                }
            } else if (parcel.dataPosition() != length)
                throw new FlingRuntimeException((new StringBuilder(
                        "Overread allowed size end=")).append(length).toString(),
                        parcel);
            else
                return new FlingDevice(versionCode, deviceId, hostAddress,
                        friendlyName, modelName, deviceVersion, servicePor,
                        icons);
        while (true);
    }

    public final Object[] newArray(int i) {
        return new FlingDevice[i];
    }
}
