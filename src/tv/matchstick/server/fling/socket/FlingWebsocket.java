package tv.matchstick.server.fling.socket;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import tv.matchstick.client.internal.LOG;
import tv.matchstick.fling.FlingStatusCodes;

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

    public void sendText(String namespace, String payload) {
        if (isOpen())
            send(buildMessage(namespace, payload));
    }

    private String buildMessage(String namespace, String payload) {
        JSONObject json = new JSONObject();
        try {
            json.put("namespace", namespace);
            json.put("payload", payload);
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
