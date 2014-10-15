package tv.matchstick.server.fling.channels;

import java.io.IOException;

import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

import tv.matchstick.client.internal.FlingChannel;

public final class ConnectionControlChannel extends FlingChannel {
    private static final String mUserAgent;
    private final String mPackage;

    public ConnectionControlChannel(String pName) {
        super("urn:x-cast:com.google.cast.tp.connection",
                "ConnectionControlChannel");
        mPackage = pName;
    }

    public final void connect(String transportId) throws IOException {
        JSONObject jsonobject = new JSONObject();
        try {
            jsonobject.put("type", "CONNECT");
            JSONObject jsonobject1 = new JSONObject();
            jsonobject1.put("package", mPackage);
            jsonobject.put("origin", jsonobject1);
            jsonobject.put("userAgent", mUserAgent);
        } catch (JSONException jsonexception) {
        }
        sendTextMessage(jsonobject.toString(), 0L, transportId);
    }

    public final void close(String s) throws IOException {
        JSONObject jsonobject = new JSONObject();
        try {
            jsonobject.put("type", "CLOSE");
        } catch (JSONException jsonexception) {
        }
        sendTextMessage(jsonobject.toString(), 0L, s);
    }

    static {
        Object aobj[] = new Object[4];
        aobj[0] = Integer.valueOf(0x40be38);
        aobj[1] = Build.MODEL;
        aobj[2] = Build.PRODUCT;
        aobj[3] = android.os.Build.VERSION.RELEASE;
        mUserAgent = String.format("Android FlingSDK,%d,%s,%s,%s", aobj);
    }
}
