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
    private long mCreateTime;
    private long mTimeout;
    private boolean isPingSent;
    private Timer mTimer;

    public FlingWebsocket(FlingSocketListener listener, URI serverURI) {
        super(serverURI);
        android.util.Log.d("XXXXXXXXXXXX", "serverURI = " + serverURI.toString());
        mSocketListener = listener;
        mSocketStatus = 0;
        mTimeout = 10000;
        resetHeartbeat();
    }
    
    public final void resetHeartbeat() {
        mCreateTime = SystemClock.elapsedRealtime();
        isPingSent = false;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        mSocketStatus = 2;
        mSocketListener.onConnected();
        android.util.Log.d("XXXXXXXXXXXXXX", "onOpen");
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                long elapsedTime = SystemClock.elapsedRealtime() - mCreateTime;
                if (isPingSent && elapsedTime > mTimeout) {
                    closeSocket();
                    mTimer.cancel();
                }
                if (!isPingSent && elapsedTime > (mTimeout / 2))
                    sendPing();
            }
        },1000, 1000);
    }
    
    private void closeSocket() {
        android.util.Log.d("XXXXXXXXXXX", "closeWebSocket");
        if (mTimer != null)
            mTimer.cancel();
        mSocketListener.onDisconnected(FlingStatusCodes.NETWORK_ERROR);
        close();
    }

    private void sendPing() {
        JSONObject data = new JSONObject();
        try {
            data.put("type", "PING");
            isPingSent = true;
            sendText("urn:x-cast:com.google.cast.system", "SystemSender", "0", data.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
                resetHeartbeat();
                mSocketListener.onMessageReceived(message);
//            }
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        android.util.Log.d("XXXXXXXXXXX", "onClose");
        if (mTimer != null)
            mTimer.cancel();
        mSocketListener.onDisconnected(FlingStatusCodes.NETWORK_ERROR);
    }

    @Override
    public void onError(Exception ex) {
        android.util.Log.d("XXXXXXXXXXX", "onError");
        if (mTimer != null)
            mTimer.cancel();
        mSocketListener.onDisconnected(FlingStatusCodes.NETWORK_ERROR);
    }
}
