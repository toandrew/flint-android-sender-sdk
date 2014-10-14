package tv.matchstick.fling;

import java.util.ArrayList;
import java.util.List;

import tv.matchstick.client.common.internal.safeparcel.ParcelReadUtil;
import tv.matchstick.client.common.internal.safeparcel.ParcelWriteUtil;
import tv.matchstick.client.common.internal.safeparcel.ParcelReadUtil.SafeParcel;
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
        int i = ParcelWriteUtil.position(parcel);
        ParcelWriteUtil.write(parcel, 1, applicationmetadata.getVersionCode());
        ParcelWriteUtil.write(parcel, 2, applicationmetadata.getApplicationId(),
                false);
        ParcelWriteUtil.write(parcel, 3, applicationmetadata.getName(), false);
        ParcelWriteUtil.write(parcel, 4, applicationmetadata.getImages(), false);
        ParcelWriteUtil
                .writeStringList(parcel, 5, applicationmetadata.getNamespaces(), false);
        ParcelWriteUtil.write(parcel, 6,
                applicationmetadata.getSenderAppIdentifier(), false);
        ParcelWriteUtil.write(parcel, 7,
                applicationmetadata.getSenderAppLaunchUrl(), flag, false);
        ParcelWriteUtil.writeEnd(parcel, i);
    }

    public ApplicationMetadata createFromParcel(Parcel parcel) {
        int size = ParcelReadUtil.readStart(parcel);
        int versionCode = 0;
        String applicationId = null;
        String name = null;
        ArrayList<WebImage> images = null;
        ArrayList<String> namespaces = null;
        String senderAppIdentifier = null;
        Uri senderAppLaunchUrl = null;
        while (parcel.dataPosition() < size) {
            int type = ParcelReadUtil.readSingleInt(parcel);
            switch (ParcelReadUtil.halfOf(type)) {
            case 1:
                versionCode = ParcelReadUtil.readInt(parcel, type);
                break;
            case 2:
                applicationId = ParcelReadUtil.readString(parcel, type);
                break;
            case 3:
                name = ParcelReadUtil.readString(parcel, type);
                break;
            case 4:
                images = ParcelReadUtil.readCreatorList(parcel, type, WebImage.CREATOR);
                break;
            case 5:
                namespaces = ParcelReadUtil.readStringList(parcel, type);
                break;
            case 6:
                senderAppIdentifier = ParcelReadUtil.readString(parcel, type);
                break;
            case 7:
                senderAppLaunchUrl = (Uri) ParcelReadUtil.readParcelable(parcel, type, Uri.CREATOR);
                break;
            default:
                ParcelReadUtil.skip(parcel, type);
            }
        }
        if (parcel.dataPosition() != size)
            throw new SafeParcel("Overread allowed size end=" + size, parcel);
        ApplicationMetadata applicationMetadata = new ApplicationMetadata(
                versionCode, applicationId, name, images, namespaces, senderAppIdentifier, senderAppLaunchUrl);
        return applicationMetadata;

    }

    @Override
    public ApplicationMetadata[] newArray(int length) {
        return new ApplicationMetadata[length];
    }
}
