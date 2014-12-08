package tv.matchstick.server.flint.socket;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import tv.matchstick.client.internal.LOG;
import tv.matchstick.flint.ConnectionResult;
import tv.matchstick.flint.FlintStatusCodes;

public class FlintWebsocket extends WebSocketClient {
    private final LOG log = new LOG("FlintWebsocket");
    private final FlintSocketListener mSocketListener;
    private int mSocketStatus;

    public FlintWebsocket(FlintSocketListener listener, URI serverURI) {
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
        mSocketListener.onDisconnected(ConnectionResult.SUCCESS);
    }

    @Override
    public void onError(Exception ex) {
        log.d("error");
        mSocketListener.onDisconnected(ConnectionResult.NETWORK_ERROR);
    }
}
