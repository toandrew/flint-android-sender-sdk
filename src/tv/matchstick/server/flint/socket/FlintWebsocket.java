package tv.matchstick.server.flint.socket;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import tv.matchstick.client.internal.LOG;
import tv.matchstick.flint.ConnectionResult;

public class FlintWebsocket extends WebSocketClient {
    private final LOG log = new LOG("FlintWebsocket");
    private final FlintSocketListener mSocketListener;
    private boolean mOnlyCloseWebSocket = false;

    public FlintWebsocket(FlintSocketListener listener, URI serverURI) {
        super(serverURI);
        log.d("url = %s", serverURI.toString());
        mSocketListener = listener;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        log.d("open");
        mSocketListener.onConnected();
    }

    public void sendText(String namespace, String payload) {
        if (isOpen())
            send(buildMessage(namespace, payload));
    }

    public void onlyClose() {
        mOnlyCloseWebSocket = true;
        close();
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
        log.d("close: " + code + "; " + reason);
        if (!mOnlyCloseWebSocket)
            mSocketListener.onDisconnected(ConnectionResult.SUCCESS);
    }

    @Override
    public void onError(Exception ex) {
        log.d("error");
        mSocketListener.onDisconnected(ConnectionResult.NETWORK_ERROR);
    }
}
