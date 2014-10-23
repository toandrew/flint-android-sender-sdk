package tv.matchstick.server.fling.mdns;

import java.util.List;

public final class FlingDeviceInfoContainer {
    FlingDeviceInfo mFlingDeviceInfo;

    public FlingDeviceInfoContainer(String paramString) {
        this.mFlingDeviceInfo = new FlingDeviceInfo(paramString);
    }

    public final FlingDeviceInfoContainer setProto_a(int paramInt) {
        this.mFlingDeviceInfo.mProto = paramInt;
        return this;
    }

    final class FlingDeviceInfo {
        String FQDN_a;
        List mIpV4AddrList;
        List mIpV6AddrList;
        String mName;
        String e;
        String mHost;
        int mProto; // 1:tcp, 2: udp
        int mPort;
        int mPriority;
        int mWeight;
        List mTextStringList;
        long mTTL;

        private FlingDeviceInfo(String paramString) {
            this.FQDN_a = paramString;
            this.mProto = 0;
            this.mTTL = -1L;
        }
    }
}
