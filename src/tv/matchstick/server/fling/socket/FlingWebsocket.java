package tv.matchstick.server.fling.socket;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import tv.matchstick.fling.FlingStatusCodes;
import android.os.SystemClock;

public class FlingWebsocket extends WebSocketClient {
    private final FlingSocketListener mSocketListener;
    private int mSocketStatus;

    public FlingWebsocket(FlingSocketListener listener, URI serverURI) {
        super(serverURI);
        mSocketListener = listener;
        mSocketStatus = 0;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        mSocketStatus = 2;
        mSocketListener.onConnected();
        android.util.Log.d("XXXXXXXXXXXXXX", "onOpen");
    }

    public void sendText(String namespace, String senderId, String requestId, String data) {
        if (isOpen())
            send(buildMessage(namespace, senderId, requestId, data));
    }

    private String buildMessage(String namespace, String senderId, String requestId, String data) {
        JSONObject json = new JSONObject();
        try {
            json.put("namespace", namespace);
            json.put("requestId", requestId);
            json.put("data", data);
            android.util.Log.d("QQQQQQQQQQ", "send: " + json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    @Override
    public void onMessage(String message) {
        android.util.Log.d("QQQQQQQQQQ", "receive message = " + message);
        //"{\"namespace\":\"urn:x-cast:com.google.cast.media\",\"senderId\":\"*:*\",\"data\":\"{\\\"type\\\":\\\"PONG\\\"}\"}"
//        try {
//            JSONObject json = new JSONObject(message);
//            if ("PONG") {
                mSocketListener.onMessageReceived(message);
//            }
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        android.util.Log.d("XXXXXXXXXXX", "onClose");
        mSocketListener.onDisconnected(FlingStatusCodes.SUCCESS);
    }

    @Override
    public void onError(Exception ex) {
        android.util.Log.d("XXXXXXXXXXX", "onError");
        mSocketListener.onDisconnected(FlingStatusCodes.NETWORK_ERROR);
    }
}
