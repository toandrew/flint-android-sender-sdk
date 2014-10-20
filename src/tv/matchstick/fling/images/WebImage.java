package tv.matchstick.fling.images;

import org.json.JSONException;
import org.json.JSONObject;

import tv.matchstick.client.common.internal.safeparcel.ParcelReadUtil;
import tv.matchstick.client.common.internal.safeparcel.ParcelWriteUtil;
import tv.matchstick.client.common.internal.safeparcel.SafeParcelable;
import tv.matchstick.client.internal.MyStringBuilder;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Fling Image
 */
public class WebImage implements SafeParcelable {

	public static final Parcelable.Creator<WebImage> CREATOR = new Parcelable.Creator<WebImage>() {

		@Override
		public WebImage createFromParcel(Parcel source) {
			// TODO Auto-generated method stub

			int size = ParcelReadUtil.readStart(source);
			int version = 0;
			Uri url = null;
			int width = 0;
			int height = 0;
			while (source.dataPosition() < size) {
				int type = ParcelReadUtil.readSingleInt(source);
				switch (ParcelReadUtil.halfOf(type)) {
				case 1:
					version = ParcelReadUtil.readInt(source, type);
					break;
				case 2:
					url = (Uri) ParcelReadUtil.readParcelable(source, type,
							Uri.CREATOR);
					break;
				case 3:
					width = ParcelReadUtil.readInt(source, type);
					break;
				case 4:
					height = ParcelReadUtil.readInt(source, type);
					break;
				default:
					ParcelReadUtil.skip(source, type);
				}
			}

			if (source.dataPosition() != size) {
				throw new ParcelReadUtil.SafeParcel(
						"Overread allowed size end=" + size, source);
			}

			return new WebImage(version, url, width, height);
		}

		@Override
		public WebImage[] newArray(int size) {
			// TODO Auto-generated method stub

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
		int i = ParcelWriteUtil.position(out);
		ParcelWriteUtil.write(out, 1, getVersionCode());
		ParcelWriteUtil.write(out, 2, getUrl(), flags, false);
		ParcelWriteUtil.write(out, 3, getWidth());
		ParcelWriteUtil.write(out, 4, getHeight());
		ParcelWriteUtil.writeEnd(out, i);
	}
}
