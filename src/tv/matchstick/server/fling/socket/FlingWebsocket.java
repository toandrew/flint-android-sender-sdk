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

import tv.matchstick.client.internal.LOG;
import tv.matchstick.fling.FlingStatusCodes;
import android.os.SystemClock;

public class FlingWebsocket extends WebSocketClient {
    private final LOG log = new LOG("FlingWebsocket");
    private final FlingSocketListener mSocketListener;
    private int mSocketStatus;

    public FlingWebsocket(FlingSocketListener listener, URI serverURI) {
        super(serverURI);
        log.d("url = %s", serverURI.toString());
        mSocketListener = listener;
        mSocketStatus = 0;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        log.d("open");
        mSocketStatus = 2;
        mSocketListener.onConnected();
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
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    @Override
    public void onMessage(String message) {
        mSocketListener.onMessageReceived(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.d("close");
        mSocketListener.onDisconnected(FlingStatusCodes.SUCCESS);
    }

    @Override
    public void onError(Exception ex) {
        log.d("error");
        mSocketListener.onDisconnected(FlingStatusCodes.NETWORK_ERROR);
    }
}
