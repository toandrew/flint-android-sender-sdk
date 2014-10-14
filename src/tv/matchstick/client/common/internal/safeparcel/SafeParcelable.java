
package tv.matchstick.client.common.internal.safeparcel;

import android.os.Parcelable;

/*
 * OK
 */
public interface SafeParcelable extends Parcelable {
    public static final String NULL = "SAFE_PARCELABLE_NULL_STRING";
    public static final int SAFE_PARCEL_MAGIC = 20293;
}
