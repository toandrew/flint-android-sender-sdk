package tv.matchstick.fling;

import java.util.ArrayList;
import java.util.List;

import tv.matchstick.client.common.internal.safeparcel.ParcelReadUtil;
import tv.matchstick.client.common.internal.safeparcel.ParcelWriteUtil;
import tv.matchstick.client.common.internal.safeparcel.ParcelReadUtil.SafeParcelA;
import tv.matchstick.fling.images.WebImage;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Internal Application Meta data Creator
 */
class ApplicationMetadataCreator implements
        Parcelable.Creator<ApplicationMetadata> {
    static void buildParcel(ApplicationMetadata applicationmetadata,
            Parcel parcel, int flag) {
        int i = ParcelWriteUtil.p(parcel);
        ParcelWriteUtil.c(parcel, 1, applicationmetadata.getVersionCode());
        ParcelWriteUtil.a(parcel, 2, applicationmetadata.getApplicationId(),
                false);
        ParcelWriteUtil.a(parcel, 3, applicationmetadata.getName(), false);
        ParcelWriteUtil.b(parcel, 4, applicationmetadata.getImages(), false);
        ParcelWriteUtil
                .a(parcel, 5, applicationmetadata.getNamespaces(), false);
        ParcelWriteUtil.a(parcel, 6,
                applicationmetadata.getSenderAppIdentifier(), false);
        ParcelWriteUtil.a(parcel, 7,
                applicationmetadata.getSenderAppLaunchUrl(), flag, false);
        ParcelWriteUtil.D(parcel, i);
    }

    public ApplicationMetadata createFromParcel(Parcel parcel) {
        int size = ParcelReadUtil.o(parcel);
        int versionCode = 0;
        String applicationId = null;
        String name = null;
        ArrayList<WebImage> images = null;
        ArrayList<String> namespaces = null;
        String senderAppIdentifier = null;
        Uri senderAppLaunchUrl = null;
        while (parcel.dataPosition() < size) {
            int type = ParcelReadUtil.readInt_n(parcel);
            switch (ParcelReadUtil.S(type)) {
            case 1:
                versionCode = ParcelReadUtil.g(parcel, type);
                break;
            case 2:
                applicationId = ParcelReadUtil.m(parcel, type);
                break;
            case 3:
                name = ParcelReadUtil.m(parcel, type);
                break;
            case 4:
                images = ParcelReadUtil.c(parcel, type, WebImage.CREATOR);
                break;
            case 5:
                namespaces = ParcelReadUtil.y(parcel, type);
                break;
            case 6:
                senderAppIdentifier = ParcelReadUtil.m(parcel, type);
                break;
            case 7:
                senderAppLaunchUrl = (Uri) ParcelReadUtil.a(parcel, type, Uri.CREATOR);
                break;
            default:
                ParcelReadUtil.b(parcel, type);
            }
        }
        if (parcel.dataPosition() != size)
            throw new SafeParcelA("Overread allowed size end=" + size, parcel);
        ApplicationMetadata applicationMetadata = new ApplicationMetadata(
                versionCode, applicationId, name, images, namespaces, senderAppIdentifier, senderAppLaunchUrl);
        return applicationMetadata;

    }

    @Override
    public ApplicationMetadata[] newArray(int length) {
        return new ApplicationMetadata[length];
    }
}
