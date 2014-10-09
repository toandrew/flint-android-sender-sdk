
package tv.matchstick.server.fling.channels;

import java.io.UnsupportedEncodingException;

public abstract class DeviceAuthChannel extends FlingChannel {
    private final String mTransId;

    public DeviceAuthChannel(String transId_s, byte abyte0[]) {
        super("urn:x-cast:com.google.cast.tp.deviceauth", "DeviceAuthChannel");
        mTransId = transId_s;
    }

    public final void doDeviceAuth() {
        try {
            sendBinaryMessage("fling".getBytes("UTF-8"), mTransId);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    protected abstract void verifyDevAuthResult(int i);

    public final void onReceivedMessage(byte abyte0[]) {
        verifyDevAuthResult(0);
    }
}
