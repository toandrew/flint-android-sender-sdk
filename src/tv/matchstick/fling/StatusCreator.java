package tv.matchstick.fling;

import tv.matchstick.client.common.internal.safeparcel.ParcelReadUtil;
import tv.matchstick.client.common.internal.safeparcel.ParcelWriteUtil;
import android.app.PendingIntent;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Internal Status Creator
 */
class StatusCreator implements Parcelable.Creator<Status> {

	public static final int CONTENT_DESCRIPTION = 0;

	@Override
	public Status createFromParcel(Parcel parcel) {
		int i = ParcelReadUtil.readStart(parcel);
		int j = 0;
		int k = 0;
		String str = null;
		PendingIntent localPendingIntent = null;
		while (parcel.dataPosition() < i) {
			int l = ParcelReadUtil.readSingleInt(parcel);
			switch (ParcelReadUtil.halfOf(l)) {
			case 1:
				k = ParcelReadUtil.readInt(parcel, l);
				break;
			case 1000:
				j = ParcelReadUtil.readInt(parcel, l);
				break;
			case 2:
				str = ParcelReadUtil.readString(parcel, l);
				break;
			case 3:
				localPendingIntent = (PendingIntent) ParcelReadUtil.readParcelable(parcel,
						l, PendingIntent.CREATOR);
				break;
			default:
				ParcelReadUtil.skip(parcel, l);
			}
		}
		if (parcel.dataPosition() != i)
			throw new ParcelReadUtil.SafeParcel("Overread allowed size end="
					+ i, parcel);
		return new Status(j, k, str, localPendingIntent);
	}

	@Override
	public Status[] newArray(int size) {
		return new Status[size];
	}

	/**
	 * Build parcel data
	 * 
	 * @param paramStatus
	 * @param paramParcel
	 * @param paramInt
	 */
	static void buildParcel(Status paramStatus, Parcel paramParcel, int paramInt) {
		int i = ParcelWriteUtil.position(paramParcel);
		ParcelWriteUtil.write(paramParcel, 1, paramStatus.getStatusCode());
		ParcelWriteUtil.write(paramParcel, 1000, paramStatus.getVersionCode());
		ParcelWriteUtil
				.write(paramParcel, 2, paramStatus.getStatusMessage(), false);
		ParcelWriteUtil.write(paramParcel, 3, paramStatus.getPendingIntent(),
				paramInt, false);
		ParcelWriteUtil.writeEnd(paramParcel, i);
	}
}
