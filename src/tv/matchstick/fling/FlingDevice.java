package tv.matchstick.fling;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tv.matchstick.client.common.internal.safeparcel.ParcelReadUtil;
import tv.matchstick.client.common.internal.safeparcel.ParcelWriteUtil;
import tv.matchstick.client.common.internal.safeparcel.SafeParcelable;
import tv.matchstick.fling.images.WebImage;
import tv.matchstick.server.common.checker.ObjEqualChecker;
import tv.matchstick.server.common.exception.FlingRuntimeException;
import tv.matchstick.server.fling.mdns.FlingDeviceHelper;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Contain all info about a Fling device.
 */
public class FlingDevice implements SafeParcelable {
	public static final Parcelable.Creator<FlingDevice> CREATOR = new Parcelable.Creator<FlingDevice>() {

		@Override
		public FlingDevice createFromParcel(Parcel source) {
			// TODO Auto-generated method stub

			int servicePor = 0;
			ArrayList<WebImage> icons = null;
			int length = ParcelReadUtil.readStart(source);
			String deviceVersion = null;
			String modelName = null;
			String friendlyName = null;
			String hostAddress = null;
			String deviceId = null;
			int versionCode = 0;

			while (source.dataPosition() < length) {
				int position = source.readInt();
				switch (0xffff & position) {
				default:
					ParcelReadUtil.skip(source, position);
					break;

				case 1:
					versionCode = ParcelReadUtil.readInt(source, position);
					break;

				case 2:
					deviceId = ParcelReadUtil.readString(source, position);
					break;

				case 3:
					hostAddress = ParcelReadUtil.readString(source, position);
					break;

				case 4:
					friendlyName = ParcelReadUtil.readString(source, position);
					break;

				case 5:
					modelName = ParcelReadUtil.readString(source, position);
					break;

				case 6:
					deviceVersion = ParcelReadUtil.readString(source, position);
					break;

				case 7:
					servicePor = ParcelReadUtil.readInt(source, position);
					break;

				case 8:
					icons = ParcelReadUtil.readCreatorList(source, position,
							WebImage.CREATOR);
					break;
				}
			}

			if (source.dataPosition() != length) {
				throw new FlingRuntimeException((new StringBuilder(
						"Overread allowed size end=")).append(length)
						.toString(), source);
			}

			return new FlingDevice(versionCode, deviceId, hostAddress,
					friendlyName, modelName, deviceVersion, servicePor, icons);
		}

		@Override
		public FlingDevice[] newArray(int size) {
			// TODO Auto-generated method stub

			return new FlingDevice[size];
		}

	};

	/**
	 * Host address
	 */
	public String mHostAddress;

	/**
	 * current firmware version
	 */
	private final int mVersionCode;

	/**
	 * device id
	 */
	private String mDeviceId;

	/**
	 * host internet address
	 */
	private Inet4Address mHost;

	/**
	 * display name
	 */
	private String mFriendlyName;

	/**
	 * model name
	 */
	private String mModleName; // md=Chromecast

	/**
	 * device version
	 */
	private String mDeviceVersion; // ve=02

	/**
	 * service's port, 8009,etc
	 */
	private int mServicePort;

	/**
	 * icon list
	 */
	private List<WebImage> mIconList;

	private FlingDevice() {
		this(1, null, null, null, null, null, -1, ((List) (new ArrayList())));
	}

	/**
	 * Create fling device.
	 *
	 * @param versionCode
	 *            SDK version code
	 * @param deviceId
	 *            Fling device Id
	 * @param hostAddress
	 *            device's Host Address
	 * @param friendlyName
	 *            device's name
	 * @param modelName
	 *            device's model name
	 * @param deviceVersion
	 *            device's version
	 * @param servicePor
	 *            device's service port
	 * @param icons
	 *            device icons
	 */
	public FlingDevice(int versionCode, String deviceId, String hostAddress,
			String friendlyName, String modelName, String deviceVersion,
			int servicePor, List<WebImage> icons) {
		mVersionCode = versionCode;
		mDeviceId = deviceId;
		mHostAddress = hostAddress;
		if (mHostAddress != null)
			try {
				InetAddress inetaddress = InetAddress.getByName(mHostAddress);
				if (inetaddress instanceof Inet4Address)
					mHost = (Inet4Address) inetaddress;
			} catch (UnknownHostException e) {
				mHost = null;
			}
		mFriendlyName = friendlyName;
		mModleName = modelName;
		mDeviceVersion = deviceVersion;
		mServicePort = servicePor;
		mIconList = icons;
	}

	/**
	 * Set device's service port.
	 *
	 * @param flingdevice
	 *            Fling device
	 * @param port
	 *            device service port
	 * @return port device service port
	 */
	public static int setServicePort(FlingDevice flingdevice, int port) {
		flingdevice.mServicePort = port;
		return port;
	}

	/**
	 * Create fling device's container.
	 *
	 * @param deviceId
	 *            device Id
	 * @param inet4address
	 *            device address
	 * @return fling device's container
	 */
	public static FlingDeviceHelper createHelper(String deviceId,
			Inet4Address inet4address) {
		FlingDevice flingdevice = new FlingDevice();
		flingdevice.getClass();
		return new FlingDeviceHelper(flingdevice, deviceId, inet4address);
	}

	/**
	 * Set fling device's id.
	 *
	 * @param flingdevice
	 *            Fling device
	 * @param deviceID
	 *            device Id
	 * @return device Id
	 */
	public static String setDeviceId(FlingDevice flingdevice, String deviceID) {
		flingdevice.mDeviceId = deviceID;
		return deviceID;
	}

	/**
	 * Get fling device's host.
	 *
	 * @param flingdevice
	 *            Fling device
	 * @return device's host address.
	 */
	public static Inet4Address getHost(FlingDevice flingdevice) {
		return flingdevice.mHost;
	}

	/**
	 * Set device's host.
	 *
	 * @param flingdevice
	 *            Fling device
	 * @param inet4address
	 *            host address
	 * @return device address
	 */
	public static Inet4Address setHost(FlingDevice flingdevice,
			Inet4Address inet4address) {
		flingdevice.mHost = inet4address;
		return inet4address;
	}

	/**
	 * Set icon list.
	 *
	 * @param flingdevice
	 *            Fling device
	 * @param list
	 *            icon list
	 * @return icon list
	 */
	public static List<WebImage> setIconList(FlingDevice flingdevice,
			List<WebImage> list) {
		flingdevice.mIconList = list;
		return list;
	}

	/**
	 * Get device from bundle data.
	 *
	 * @param bundle
	 *            bundle data which contains device info
	 * @return Fling device
	 */
	public static FlingDevice getFromBundle(Bundle bundle) {
		if (bundle == null) {
			return null;
		}

		bundle.setClassLoader(FlingDevice.class.getClassLoader());

		return (FlingDevice) bundle
				.getParcelable("tv.matchstick.fling.EXTRA_FLING_DEVICE");
	}

	/**
	 * Set friendly name.
	 *
	 * @param flingdevice
	 *            Fling device
	 * @param name
	 *            device name
	 * @return device name
	 */
	public static String setFriendlyName(FlingDevice flingdevice, String name) {
		flingdevice.mFriendlyName = name;
		return name;
	}

	/**
	 * Set model name.
	 *
	 * @param flingdevice
	 *            Fling device
	 * @param name
	 *            device model name
	 * @return device model name
	 */
	public static String setModelName(FlingDevice flingdevice, String name) {
		flingdevice.mModleName = name;
		return name;
	}

	/**
	 * Set device's version.
	 *
	 * @param flingdevice
	 *            Fling device
	 * @param version
	 *            device version
	 * @return device version
	 */
	public static String setDeviceVersion(FlingDevice flingdevice,
			String version) {
		flingdevice.mDeviceVersion = version;
		return version;
	}

	/**
	 * Get SDK's version.
	 *
	 * @return SDK's version
	 */
	public final int getVersionCode() {
		return mVersionCode;
	}

	/**
	 * Fill device's bundle data with FlingDevice data
	 *
	 * @param bundle
	 *            FlingDevice bundle data
	 */
	public final void putInBundle(Bundle bundle) {
		if (bundle == null) {
			return;
		}

		bundle.putParcelable("tv.matchstick.fling.EXTRA_FLING_DEVICE", this);
	}

	/**
	 * Get device Id.
	 *
	 * @return device Id
	 */
	public final String getDeviceId() {
		return mDeviceId;
	}

	/**
	 * Get device's IP address.
	 *
	 * @return device's IP address
	 */
	public final Inet4Address getIpAddress() {
		return mHost;
	}

	/**
	 * Check whether there's icon.
	 *
	 * @return true for non-empty list, otherwise false
	 */
	public final boolean hasIcons() {
		return (!(mIconList.isEmpty()));
	}

	/**
	 * Get device friendly name.
	 *
	 * @return device name
	 */
	public final String getFriendlyName() {
		return mFriendlyName;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	/**
	 * Get device model name
	 *
	 * @return device model name
	 */
	public final String getModelName() {
		return mModleName;
	}

	/**
	 * Check whether they are the same Fling device.
	 *
	 * @param flingDevice
	 *            Fling device
	 * @return true for same device, or false
	 */
	public final boolean isSameDevice(FlingDevice flingDevice) {
		if (flingDevice == null) {
			return false;
		}

		if (getDeviceId() == null) {
			return (flingDevice.getDeviceId() == null);
		}

		return ObjEqualChecker.isEquals(getDeviceId(),
				flingDevice.getDeviceId());

	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}

		if (!(other instanceof FlingDevice)) {
			return false;
		}

		FlingDevice flingDevice = (FlingDevice) other;

		if (mDeviceId == null) {
			if (flingDevice.mDeviceId == null) {
				return true;
			}
			return false;
		}

		if (ObjEqualChecker.isEquals(mDeviceId, flingDevice.mDeviceId)
				&& ObjEqualChecker.isEquals(mHost, flingDevice.mHost)
				&& ObjEqualChecker.isEquals(mModleName, flingDevice.mModleName)
				&& ObjEqualChecker.isEquals(mFriendlyName,
						flingDevice.mFriendlyName)
				&& ObjEqualChecker.isEquals(mDeviceVersion,
						flingDevice.mDeviceVersion)
				&& mServicePort == flingDevice.mServicePort
				&& ObjEqualChecker.isEquals(mIconList, flingDevice.mIconList)) {
			return true;
		}

		return false;
	}

	/**
	 * get device's version.
	 *
	 * @return device version
	 */
	public final String getDeviceVersion() {
		return mDeviceVersion;
	}

	/**
	 * Get service's port.
	 *
	 * @return service port
	 */
	public final int getServicePort() {
		return mServicePort;
	}

	/**
	 * Get preferred icon.
	 *
	 * @param preferredWidth
	 *            preferred icon width
	 * @param preferredHeight
	 *            preferred icon height
	 * @return icon
	 */
	public final WebImage getIcon(int preferredWidth, int preferredHeight) {
		if (mIconList != null && !mIconList.isEmpty()) {
			if ((preferredWidth <= 0) || (preferredHeight <= 0)) {
				return ((WebImage) this.mIconList.get(0));
			}

			WebImage image1 = null;
			WebImage image2 = null;
			for (int i = 0; i < mIconList.size(); i++) {
				WebImage image = (WebImage) mIconList.get(i);
				int width = image.getWidth();
				int height = image.getHeight();
				if ((width >= preferredWidth) && (height >= height)) {
					if ((image1 == null)
							|| ((image1.getWidth() > width) && (image1
									.getHeight() > height))) {
						image1 = image;
					}
				} else if ((width < preferredWidth)
						&& (height < preferredHeight)
						&& (((image2 == null) || ((image2.getWidth() < width) && (image2
								.getHeight() < height))))) {
					image2 = image;
				}
			}
			if (image1 != null) {
				return image1;
			}
			if (image2 != null) {
				return image2;
			}

			return (WebImage) mIconList.get(0);
		}

		return null;
	}

	/**
	 * Get all icons.
	 *
	 * @return icon list
	 */
	public final List<WebImage> getIcons() {
		return Collections.unmodifiableList(mIconList);
	}

	@Override
	public int hashCode() {
		if (mDeviceId == null) {
			return 0;
		}

		return mDeviceId.hashCode();
	}

	@Override
	public String toString() {
		return String.format("\"%s\" (%s)", mFriendlyName, mDeviceId);
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		buildParcel(parcel);
	}

	private void buildParcel(Parcel out) {
		int position = ParcelWriteUtil.position(out);
		ParcelWriteUtil.write(out, 1, getVersionCode());
		ParcelWriteUtil.write(out, 2, getDeviceId(), false);
		ParcelWriteUtil.write(out, 3, mHostAddress, false);
		ParcelWriteUtil.write(out, 4, getFriendlyName(), false);
		ParcelWriteUtil.write(out, 5, getModelName(), false);
		ParcelWriteUtil.write(out, 6, getDeviceVersion(), false);
		ParcelWriteUtil.write(out, 7, getServicePort());
		ParcelWriteUtil.write(out, 8, getIcons(), false);
		ParcelWriteUtil.writeEnd(out, position);
	}

}
