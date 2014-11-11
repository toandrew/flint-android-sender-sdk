/*
 * Copyright (C) 2013-2014, Infthink (Beijing) Technology Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS-IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package tv.matchstick.fling;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tv.matchstick.client.common.internal.safeparcel.ParcelRead;
import tv.matchstick.client.common.internal.safeparcel.ParcelWrite;
import tv.matchstick.client.common.internal.safeparcel.SafeParcelable;
import tv.matchstick.fling.images.WebImage;
import tv.matchstick.server.common.checker.ObjEqualChecker;
import tv.matchstick.server.common.exception.FlingRuntimeException;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Contain all info about a Fling device.
 */
public class FlingDevice implements SafeParcelable {
    public static final String FOUND_SOURCE_SSDP = "sspd";
    public static final String FOUND_SOURCE_MDNS = "mdns";
    public static final Parcelable.Creator<FlingDevice> CREATOR = new Parcelable.Creator<FlingDevice>() {

        @Override
        public FlingDevice createFromParcel(Parcel source) {
            // TODO Auto-generated method stub

            int servicePor = 0;
            ArrayList<WebImage> icons = null;
            int length = ParcelRead.readStart(source);
            String deviceVersion = null;
            String modelName = null;
            String friendlyName = null;
            String hostAddress = null;
            String deviceId = null;
            int versionCode = 0;
            String foundSource = null;

            while (source.dataPosition() < length) {
                int position = source.readInt();
                switch (0xffff & position) {
                default:
                    ParcelRead.skip(source, position);
                    break;

                case 1:
                    versionCode = ParcelRead.readInt(source, position);
                    break;

                case 2:
                    deviceId = ParcelRead.readString(source, position);
                    break;

                case 3:
                    hostAddress = ParcelRead.readString(source, position);
                    break;

                case 4:
                    friendlyName = ParcelRead.readString(source, position);
                    break;

                case 5:
                    modelName = ParcelRead.readString(source, position);
                    break;

                case 6:
                    deviceVersion = ParcelRead.readString(source, position);
                    break;

                case 7:
                    servicePor = ParcelRead.readInt(source, position);
                    break;

                case 8:
                    icons = ParcelRead.readCreatorList(source, position,
                            WebImage.CREATOR);
                    break;
                case 9:
                    foundSource = ParcelRead.readString(source, position);
                    break;
                }
            }

            if (source.dataPosition() != length) {
                throw new FlingRuntimeException("Overread allowed size end="
                        + length, source);
            }

            return new FlingDevice(versionCode, deviceId, hostAddress,
                    friendlyName, modelName, deviceVersion, servicePor, icons, foundSource);
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
    
    /**
     * device found source
     */
    private String mFoundSource;

    private FlingDevice() {
        this(1, null, null, null, null, null, -1, ((List) (new ArrayList())), null);
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
            int servicePor, List<WebImage> icons, String foundSource) {
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
        mFoundSource = foundSource;
    }

    /**
     * Set device's service port.
     *
     * @param device
     *            Fling device
     * @param port
     *            device service port
     * @return port device service port
     */
    public static int setServicePort(FlingDevice device, int port) {
        device.mServicePort = port;
        return port;
    }

    /**
     * Set fling device's id.
     *
     * @param device
     *            Fling device
     * @param deviceID
     *            device Id
     * @return device Id
     */
    public static String setDeviceId(FlingDevice device, String deviceID) {
        device.mDeviceId = deviceID;
        return deviceID;
    }

    /**
     * Get fling device's host.
     *
     * @param device
     *            Fling device
     * @return device's host address.
     */
    public static Inet4Address getHost(FlingDevice device) {
        return device.mHost;
    }

    /**
     * Set device's host.
     *
     * @param device
     *            Fling device
     * @param inet4address
     *            host address
     * @return device address
     */
    public static Inet4Address setHost(FlingDevice device,
            Inet4Address inet4address) {
        device.mHost = inet4address;
        return inet4address;
    }

    /**
     * Set icon list.
     *
     * @param device
     *            Fling device
     * @param list
     *            icon list
     * @return icon list
     */
    public static List<WebImage> setIconList(FlingDevice device,
            List<WebImage> list) {
        device.mIconList = list;
        return list;
    }

    /**
     * Get device from bundle data.
     *
     * @param data
     *            bundle data which contains device info
     * @return Fling device
     */
    public static FlingDevice getFromBundle(Bundle data) {
        if (data == null) {
            return null;
        }

        data.setClassLoader(FlingDevice.class.getClassLoader());

        return (FlingDevice) data
                .getParcelable("tv.matchstick.fling.EXTRA_FLING_DEVICE");
    }

    /**
     * Set friendly name.
     *
     * @param device
     *            Fling device
     * @param name
     *            device name
     * @return device name
     */
    public static String setFriendlyName(FlingDevice device, String name) {
        device.mFriendlyName = name;
        return name;
    }

    /**
     * Set model name.
     *
     * @param device
     *            Fling device
     * @param name
     *            device model name
     * @return device model name
     */
    public static String setModelName(FlingDevice device, String name) {
        device.mModleName = name;
        return name;
    }

    /**
     * Set device's version.
     *
     * @param device
     *            Fling device
     * @param version
     *            device version
     * @return device version
     */
    public static String setDeviceVersion(FlingDevice device, String version) {
        device.mDeviceVersion = version;
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
    
    public final String getFoundSource() {
        return mFoundSource;
    }

    public static String setFoundSource(FlingDevice device, String foundSource) {
        device.mFoundSource = foundSource;
        return foundSource;
    }

    /**
     * Fill device's bundle data with FlingDevice data
     *
     * @param data
     *            FlingDevice bundle data
     */
    public final void putInBundle(Bundle data) {
        if (data == null) {
            return;
        }

        data.putParcelable("tv.matchstick.fling.EXTRA_FLING_DEVICE", this);
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
     * @param device
     *            Fling device
     * @return true for same device, or false
     */
    public final boolean isSameDevice(FlingDevice device) {
        if (device == null) {
            return false;
        }

        if (getDeviceId() == null) {
            return (device.getDeviceId() == null);
        }

        return ObjEqualChecker.isEquals(getDeviceId(), device.getDeviceId());

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
                && ObjEqualChecker.isEquals(mIconList, flingDevice.mIconList)
                && ObjEqualChecker.isEquals(mFoundSource, flingDevice.mFoundSource)) {
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
        return String.format("\"%s\" (%s) : (%s)", mFriendlyName, mDeviceId, mFoundSource);
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        buildParcel(parcel);
    }

    private void buildParcel(Parcel out) {
        int position = ParcelWrite.position(out);
        ParcelWrite.write(out, 1, getVersionCode());
        ParcelWrite.write(out, 2, getDeviceId(), false);
        ParcelWrite.write(out, 3, mHostAddress, false);
        ParcelWrite.write(out, 4, getFriendlyName(), false);
        ParcelWrite.write(out, 5, getModelName(), false);
        ParcelWrite.write(out, 6, getDeviceVersion(), false);
        ParcelWrite.write(out, 7, getServicePort());
        ParcelWrite.write(out, 8, getIcons(), false);
        ParcelWrite.write(out, 9, this.getFoundSource(), false);
        ParcelWrite.writeEnd(out, position);
    }

    /**
     * This is used to create fling device with some device info.
     */
    public static final class Builder {
        /**
         * Create fling device's container.
         *
         * @param deviceId
         *            device Id
         * @param inet4address
         *            device address
         * @return created fling device
         */
        public static FlingDevice create(String deviceId,
                Inet4Address inet4address) {

            FlingDevice device = new FlingDevice();

            device.mDeviceId = deviceId;
            device.mHost = inet4address;
            if (inet4address != null) {
                device.mHostAddress = inet4address.getHostAddress();
            }

            return device;
        }
    }
}
