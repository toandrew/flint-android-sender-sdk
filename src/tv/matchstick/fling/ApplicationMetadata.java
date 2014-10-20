package tv.matchstick.fling;

import java.util.ArrayList;
import java.util.List;

import tv.matchstick.client.common.internal.safeparcel.ParcelReadUtil;
import tv.matchstick.client.common.internal.safeparcel.ParcelWriteUtil;
import tv.matchstick.client.common.internal.safeparcel.SafeParcelable;
import tv.matchstick.client.common.internal.safeparcel.ParcelReadUtil.SafeParcel;
import tv.matchstick.fling.images.WebImage;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Application meta data.
 * 
 * Contains media meta data of the receiver application, supplied in
 * {@link Fling.ApplicationConnectionResult}
 */
public final class ApplicationMetadata implements SafeParcelable {
	/**
	 * Parcelable creator
	 */
	public static final Parcelable.Creator<ApplicationMetadata> CREATOR = new Parcelable.Creator<ApplicationMetadata>() {

		@Override
		public ApplicationMetadata createFromParcel(Parcel source) {
			// TODO Auto-generated method stub

			int size = ParcelReadUtil.readStart(source);
			int versionCode = 0;
			String applicationId = null;
			String name = null;
			ArrayList<WebImage> images = null;
			ArrayList<String> namespaces = null;
			String senderAppIdentifier = null;
			Uri senderAppLaunchUrl = null;
			while (source.dataPosition() < size) {
				int type = ParcelReadUtil.readSingleInt(source);
				switch (ParcelReadUtil.halfOf(type)) {
				case 1:
					versionCode = ParcelReadUtil.readInt(source, type);
					break;
				case 2:
					applicationId = ParcelReadUtil.readString(source, type);
					break;
				case 3:
					name = ParcelReadUtil.readString(source, type);
					break;
				case 4:
					images = ParcelReadUtil.readCreatorList(source, type,
							WebImage.CREATOR);
					break;
				case 5:
					namespaces = ParcelReadUtil.readStringList(source, type);
					break;
				case 6:
					senderAppIdentifier = ParcelReadUtil.readString(source,
							type);
					break;
				case 7:
					senderAppLaunchUrl = (Uri) ParcelReadUtil.readParcelable(
							source, type, Uri.CREATOR);
					break;
				default:
					ParcelReadUtil.skip(source, type);
				}
			}

			if (source.dataPosition() != size) {
				throw new SafeParcel("Overread allowed size end=" + size,
						source);
			}

			return new ApplicationMetadata(versionCode, applicationId, name,
					images, namespaces, senderAppIdentifier, senderAppLaunchUrl);
		}

		@Override
		public ApplicationMetadata[] newArray(int size) {
			// TODO Auto-generated method stub

			return new ApplicationMetadata[size];
		}
	};

	/**
	 * Version code
	 */
	private final int mVersionCode;

	/**
	 * Fling Application Id
	 */
	String mApplicationId;

	/**
	 * Application name
	 */
	String mName;

	/**
	 * Web images
	 */
	List<WebImage> mImages;

	/**
	 * Related namespace
	 */
	List<String> mNamespaces;

	/**
	 * Sender application's identifier
	 */
	String mSenderAppIdentifier;

	/**
	 * Sender application's url
	 */
	Uri mSenderAppLaunchUrl;

	/**
	 * ApplicationMetadata constructor.
	 * 
	 * @param versionCode
	 *            sdk's version code
	 * @param applicationId
	 *            application Id
	 * @param name
	 *            application name
	 * @param images
	 *            icons
	 * @param namespaces
	 *            namespace list
	 * @param senderAppIdentifier
	 *            sender application's indentifier
	 * @param senderAppLaunchUrl
	 *            sender application's url
	 */
	// TODO: need public?
	public ApplicationMetadata(int versionCode, String applicationId,
			String name, List<WebImage> images, List<String> namespaces,
			String senderAppIdentifier, Uri senderAppLaunchUrl) {
		this.mVersionCode = versionCode;
		this.mApplicationId = applicationId;
		this.mName = name;
		this.mImages = images;
		this.mNamespaces = namespaces;
		this.mSenderAppIdentifier = senderAppIdentifier;
		this.mSenderAppLaunchUrl = senderAppLaunchUrl;
	}

	/**
	 * default constructor.
	 */
	private ApplicationMetadata() {
		this.mVersionCode = 1;
		this.mImages = new ArrayList<WebImage>();
		this.mNamespaces = new ArrayList<String>();
	}

	/**
	 * Version code.
	 * 
	 * @return version code
	 */
	int getVersionCode() {
		return this.mVersionCode;
	}

	/**
	 * Get related application Id.
	 * 
	 * @return application Id
	 */
	public String getApplicationId() {
		return this.mApplicationId;
	}

	/**
	 * Get related application name.
	 * 
	 * @return application name
	 */
	public String getName() {
		return this.mName;
	}

	/**
	 * Check whether the specific namespace is supported by this application.
	 * 
	 * @param namespace
	 *            the specific namespace
	 * @return true for supported
	 */
	public boolean isNamespaceSupported(String namespace) {
		return ((this.mNamespaces != null) && (this.mNamespaces
				.contains(namespace)));
	}

	/**
	 * Check whether the specific namespaces are supported by the application.
	 * 
	 * @param namespaces
	 *            namespace list
	 * @return true for supported
	 */
	public boolean areNamespacesSupported(List<String> namespaces) {
		return ((this.mNamespaces != null) && (this.mNamespaces
				.containsAll(namespaces)));
	}

	/**
	 * Get sender application's identifier.
	 * 
	 * @return application's identifier
	 */
	public String getSenderAppIdentifier() {
		return this.mSenderAppIdentifier;
	}

	/**
	 * Get sender application's launch url.
	 * 
	 * @return launch url
	 */
	public Uri getSenderAppLaunchUrl() {
		return this.mSenderAppLaunchUrl;
	}

	/**
	 * Get applications images(icons,etc).
	 * 
	 * @return WebImage list object
	 */
	public List<WebImage> getImages() {
		return this.mImages;
	}

	public List<String> getNamespaces() {
		return this.mNamespaces;
	}

	@Override
	public String toString() {
		return this.mName;
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
	 * Build application meta parcel data
	 * 
	 * @param applicationmetadata
	 * @param out
	 * @param flags
	 */
	private void buildParcel(Parcel out, int flags) {
		int i = ParcelWriteUtil.position(out);
		ParcelWriteUtil.write(out, 1, getVersionCode());
		ParcelWriteUtil.write(out, 2, getApplicationId(), false);
		ParcelWriteUtil.write(out, 3, getName(), false);
		ParcelWriteUtil.write(out, 4, getImages(), false);
		ParcelWriteUtil.writeStringList(out, 5, getNamespaces(), false);
		ParcelWriteUtil.write(out, 6, getSenderAppIdentifier(), false);
		ParcelWriteUtil.write(out, 7, getSenderAppLaunchUrl(), flags, false);
		ParcelWriteUtil.writeEnd(out, i);
	}
}
