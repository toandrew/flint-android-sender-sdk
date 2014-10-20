package tv.matchstick.fling;

import tv.matchstick.client.common.api.CommonStatusCodes;
import tv.matchstick.client.common.internal.safeparcel.ParcelReadUtil;
import tv.matchstick.client.common.internal.safeparcel.ParcelWriteUtil;
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

			int i = ParcelReadUtil.readStart(source);
			int j = 0;
			int k = 0;
			String str = null;
			PendingIntent localPendingIntent = null;
			while (source.dataPosition() < i) {
				int l = ParcelReadUtil.readSingleInt(source);
				switch (ParcelReadUtil.halfOf(l)) {
				case 1:
					k = ParcelReadUtil.readInt(source, l);
					break;
				case 1000:
					j = ParcelReadUtil.readInt(source, l);
					break;
				case 2:
					str = ParcelReadUtil.readString(source, l);
					break;
				case 3:
					localPendingIntent = (PendingIntent) ParcelReadUtil
							.readParcelable(source, l, PendingIntent.CREATOR);
					break;
				default:
					ParcelReadUtil.skip(source, l);
				}
			}

			if (source.dataPosition() != i) {
				throw new ParcelReadUtil.SafeParcel(
						"Overread allowed size end=" + i, source);
			}

			return new Status(j, k, str, localPendingIntent);
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
		Status localStatus = (Status) obj;
		return ((this.mVersionCode == localStatus.mVersionCode)
				&& (this.mStatusCode == localStatus.mStatusCode)
				&& (MyStringBuilder.compare(this.mStatusMessage,
						localStatus.mStatusMessage)) && (MyStringBuilder
					.compare(this.mPendingIntent, localStatus.mPendingIntent)));
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
		return CommonStatusCodes.getStatusMessage(this.mStatusCode);
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
	 * @param paramParcel
	 * @param paramInt
	 */
	private void buildParcel(Parcel paramParcel, int paramInt) {
		int i = ParcelWriteUtil.position(paramParcel);
		ParcelWriteUtil.write(paramParcel, 1, getStatusCode());
		ParcelWriteUtil.write(paramParcel, 1000, getVersionCode());
		ParcelWriteUtil.write(paramParcel, 2, getStatusMessage(), false);
		ParcelWriteUtil.write(paramParcel, 3, getPendingIntent(), paramInt,
				false);
		ParcelWriteUtil.writeEnd(paramParcel, i);
	}
}
