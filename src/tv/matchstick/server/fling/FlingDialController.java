package tv.matchstick.server.fling;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import tv.matchstick.fling.ApplicationMetadata;
import tv.matchstick.fling.Fling;
import tv.matchstick.fling.FlingDevice;
import tv.matchstick.fling.FlingStatusCodes;
import tv.matchstick.fling.service.FlingDeviceService;
import tv.matchstick.server.fling.bridge.IFlingSrvController;
import tv.matchstick.server.fling.socket.FlingSocketListener;
import tv.matchstick.server.fling.socket.FlingWebsocket;
import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;

public class FlingDialController implements FlingSocketListener, IController {
    private static final int NUM_OF_THREADS = 20;

    private Context mContext;
    private Handler mHandler;
    private FlingDevice mFlingDevice;
    private final IFlingSrvController mFlingSrvController;
    private ApplicationState mApplicationState = new ApplicationState();
    private int mLaunchStateCounter = 0;
    private int mHeartbeatInterval = 1000;
    private Set<String> mNamespaces = new HashSet<String>();
    private Executor mExecutor = Executors.newFixedThreadPool(NUM_OF_THREADS,
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread th = new Thread(r);
                    th.setName("DIAL Controller Thread");
                    return th;
                }
            });
    private FlingWebsocket mFlingWebsocket;
    private String mCurrentReceiverUrl;

    public FlingDialController(Context context, Handler handler,
            FlingDevice flingDevice, IFlingSrvController controller) {
        controlerId = 0;
        mContext = context;
        mHandler = handler;
        mFlingDevice = flingDevice;
        mFlingSrvController = controller;
    }

    public final void connectDevice() {
        FlingDeviceService.connectFlingDevice(mContext, this);
    }

    @Override
    public void onConnected() {
        mFlingSrvController.onApplicationConnected(new ApplicationMetadata(),
                "", "", false);
    }

    private void onDialConnected() {
        FlingDeviceService.onSocketConnected(mContext, this);
    }

    @Override
    public void onConnectionFailed(int reason) {
        mFlingSrvController.onConnectionFailed();
    }

    @Override
    public void onMessageReceived(ByteBuffer message) {

    }

    @Override
    public void onDisconnected(int reason) {
        android.util.Log.d("XXXXXXXXXXX", "onDisconnected");
        FlingDeviceService.onSocketDisconnected(mContext, this, reason);
    }

    @Override
    public void connectToDeviceInternal() {
        mIsConnecting = true;
        getStatus(new StateCallback() {
            @Override
            void onResult() {
                if (mApplicationState.state != null) {
                    onDialConnected();
                } else {
                    onConnectionFailed(0);
                }
                mIsConnecting = false;
            }
        });
    }

    @Override
    public void setVolumeInternal(final double level, final boolean mute) {
        final String url = buildSystemUrl();
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    URL mURL = new URL(url);
                    HttpURLConnection urlConnection = (HttpURLConnection) mURL
                            .openConnection();
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Content-Type",
                            "application/json");
                    if (mApplicationState != null
                            && !TextUtils.isEmpty(mApplicationState.token)) {
                        urlConnection.setRequestProperty("Authorization",
                                mApplicationState.token);
                    }
                    urlConnection.setConnectTimeout(10 * 1000);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("type", "SET_VOLUME");
                    jsonObject.put("level", level);
                    jsonObject.put("muted", mute);
                    urlConnection.setDoOutput(true);

                    DataOutputStream wr = new DataOutputStream(urlConnection
                            .getOutputStream());
                    wr.writeBytes(jsonObject.toString());
                    wr.flush();
                    wr.close();

                    InputStream in = new BufferedInputStream(urlConnection
                            .getInputStream());
                    try {
                        if (urlConnection.getResponseCode() == 200
                                || urlConnection.getResponseCode() == 201) {
                            Scanner s = new Scanner(in).useDelimiter("\\A");
                            String json = s.hasNext() ? s.next() : "";
                            if (json.length() > 0) {
                                JSONObject object = new JSONObject(json);
                                final boolean success = object.optBoolean(
                                        "success", false);
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (success) {
                                        } else {
                                        }
                                    }
                                });
                            }
                        }
                    } finally {
                        in.close();
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onSocketConnectionFailedInternal(int socketError) {
        mFlingSrvController.onDisconnected(socketError);
//        mFlingSrvController.onConnectionFailed();
    }

    @Override
    public void stopApplicationInternal() {
        final String url = buildAppUrl();
        android.util.Log.d("XXXXXXXXXXX", "stopApplicationInternal: url = "
                + url);
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    URL mURL = new URL(TextUtils
                            .isEmpty(mApplicationState.appAddress) ? url : url
                            + "/" + mApplicationState.appAddress);
                    HttpURLConnection urlConnection = (HttpURLConnection) mURL
                            .openConnection();
                    urlConnection.setRequestMethod("DELETE");
                    if (mApplicationState != null
                            && !TextUtils.isEmpty(mApplicationState.token)) {
                        urlConnection.setRequestProperty("Authorization",
                                mApplicationState.token);
                    }
                    urlConnection.connect();
                    final int response = urlConnection.getResponseCode();

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (response == 200) {
                                mFlingSrvController.onRequestStatus(0);
                                stopHeartbeat();
                            } else {
                                mFlingSrvController.onRequestStatus(1);
                            }
                        }
                    });

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void joinApplicationInternal(String url) {
        if (TextUtils.isEmpty(url))
            launchApplication("join", mCurrentReceiverUrl);
        else
            launchApplication("join", url);
    }

    @Override
    public void sendTextMessage(String namespace, String message, long id,
            String transportId) {
        android.util.Log.d("QQQQQQQQQQ", "sendTextMessage: namespace = "
                + namespace + "; message = " + message + "; id = " + id);
        if (mFlingWebsocket != null && mFlingWebsocket.isOpen()) {
            mFlingWebsocket.sendText(namespace, "meida", String.valueOf(id),
                    message);
        }
    }

    @Override
    public void launchApplicationInternal(String url,
            final boolean relaunch) {
        if (mApplicationState != null
                && !"stopped".equals(mApplicationState.state)) {
            if (relaunch) {
                launchApplication("relaunch", url);
            } else {
                joinApplication(url);
            }
        } else {
            launchApplication("launch", url);
        }
    }

    private void launchApplication(final String type, final String receiverUrl) {
        final String url = buildAppUrl();
        mCurrentReceiverUrl = receiverUrl;
        android.util.Log.d("XXXXXXXXXXX", "launchApplication: url = " + url);
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    URL mURL = new URL(url);
                    HttpURLConnection urlConnection = (HttpURLConnection) mURL
                            .openConnection();
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Content-Type",
                            "application/json");
                    urlConnection.setConnectTimeout(10 * 1000);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("type", type);
                    JSONObject sub = new JSONObject();
                    sub.put("url", receiverUrl);
                    sub.put("useIpc", true);
                    jsonObject.put("app_info", sub);
                    android.util.Log.d("XXXXXXXXXXX",
                            "jsonObject.toString() = " + jsonObject.toString());
                    urlConnection.setDoOutput(true);

                    DataOutputStream wr = new DataOutputStream(urlConnection
                            .getOutputStream());
                    wr.writeBytes(jsonObject.toString());
                    wr.flush();
                    wr.close();

                    InputStream in = new BufferedInputStream(urlConnection
                            .getInputStream());
                    try {
                        android.util.Log.d("XXXXXXXXXXX",
                                "urlConnection.getResponseCode() = "
                                        + urlConnection.getResponseCode());
                        if (urlConnection.getResponseCode() == 200
                                || urlConnection.getResponseCode() == 201) {
                            Scanner s = new Scanner(in).useDelimiter("\\A");
                            String json = s.hasNext() ? s.next() : "";
                            android.util.Log
                                    .d("XXXXXXXXXXXX", "json = " + json);
                            if (json.length() > 0) {
                                JSONObject object = new JSONObject(json);
                                mApplicationState.token = object.optString(
                                        "token", "");
                                mHeartbeatInterval = object.optInt("interval",
                                        1000);
                                android.util.Log.d("XXXXXXXXXXX", "token = "
                                        + mApplicationState.token);
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mApplicationState.token.length() > 0) {
                                            mLaunchStateCounter = 0;
                                            requestLaunchState();
                                        } else {
                                            android.util.Log.d("XXXXXXXXXXX",
                                                    "no token error");
                                            mFlingSrvController
                                                    .onApplicationConnectionFailed(0);
                                        }
                                    }
                                });
                            }
                        }
                    } finally {
                        in.close();
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void requestLaunchState() {
        getStatus(new StateCallback() {
            @Override
            void onResult() {
                if (mApplicationState.state.equals("running") && !TextUtils.isEmpty(mApplicationState.url)) {
                    android.util.Log.d("XXXXXXXXXXX", "running");
                    if (mFlingWebsocket != null) {
                        mFlingWebsocket.close();
                    }
                    mFlingWebsocket = new FlingWebsocket(
                            FlingDialController.this,
                            URI.create(mApplicationState.url + "/senders/"
                                    + mApplicationState.token));
                    mFlingWebsocket.connect();
                    startHeartbeat();
                } else {
                    if (mLaunchStateCounter < 10) {
                        mLaunchStateCounter++;
                        mHandler.postDelayed(mRequestLaunchState, 1000);
                    } else {
                        android.util.Log.d("XXXXXXXXXXX", "no running error");
                        mFlingSrvController.onApplicationConnectionFailed(1);
                    }
                }
            }
        });
    }

    private Runnable mRequestLaunchState = new Runnable() {
        @Override
        public void run() {
            requestLaunchState();
        }
    };

    private void startHeartbeat() {
        mHandler.postDelayed(mTimeoutRunnable,
                Math.max(mHeartbeatInterval * 2, 10000));
        getStatus(new StateCallback() {
            @Override
            void onResult() {
                if ("stopped".equals(mApplicationState.state)) {
                    stopHeartbeat();
                    onDisconnected(FlingStatusCodes.NETWORK_ERROR);
                } else {
                    mHandler.removeCallbacks(mTimeoutRunnable);
                    mHandler.postDelayed(mHeartbeatRunnable, mHeartbeatInterval);
                    
                }
             }
        });
    }

    private Runnable mHeartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            startHeartbeat();
        }
    };

    private Runnable mTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            android.util.Log.d("XXXXXXXXXXXXX", "dial timeout");
            onSocketError(FlingStatusCodes.NETWORK_ERROR);
        }
    };

    private void stopHeartbeat() {
        mHandler.removeCallbacks(mRequestLaunchState);
        mHandler.removeCallbacks(mTimeoutRunnable);
        mHandler.removeCallbacks(mHeartbeatRunnable);
    }

    private String buildAppUrl() {
        String id = Fling.FlingApi.getApplicationId();
        String address = mFlingDevice.getIpAddress().getHostAddress();
        int port = mFlingDevice.getServicePort();
        String url = "http://" + address + ":" + port + "/apps/" + id;
        return url;
    }

    private String buildSystemUrl() {
        String address = mFlingDevice.getIpAddress().getHostAddress();
        int port = mFlingDevice.getServicePort();
        String url = "http://" + address + ":" + port + "system/control";
        return url;
    }

    @Override
    public void onReceivedMessage(ByteBuffer message) {

    }

    @Override
    public void leaveApplicationInternal() {
        stopHeartbeat();
        // mFlingSrvController.onInvalidRequest();
        mFlingSrvController.onApplicationDisconnected(0);
    }

    @Override
    public void onSocketDisconnectedInternal(int socketError) {
        android.util.Log.d("XXXXXXXXXXXXXX", "onSocketDisconnectedInternal");
        mIsConnected = false;
        mIsConnecting = false;
        mFlingSrvController.onDisconnected(socketError);
        mFlingSrvController.onApplicationDisconnected(socketError);
    }

    @Override
    public void addNamespace(String namespace) {
        if (TextUtils.isEmpty(namespace)) {
            return;
        }

        synchronized (mNamespaces) {
            mNamespaces.add(namespace);
        }
    }

    private void getStatus(final StateCallback callback) {
        final String url = buildAppUrl();
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                DefaultHandler dh = new DefaultHandler() {
                    String currentValue = null;

                    @Override
                    public void characters(char[] ch, int start, int length)
                            throws SAXException {
                        if (currentValue == null) {
                            currentValue = new String(ch, start, length);
                        } else {
                            currentValue += new String(ch, start, length);
                        }
                    }

                    @Override
                    public void startElement(String uri, String localName,
                            String qName, Attributes attributes)
                            throws SAXException {
                        if ("link".equals(qName)) {
                            mApplicationState.appAddress = attributes
                                    .getValue("href");
                        }
                        currentValue = null;
                    }

                    @Override
                    public void endElement(String uri, String localName,
                            String qName) throws SAXException {
                        if ("state".equals(qName)) {
                            mApplicationState.state = currentValue;
                        } else if ("serverId".equals(qName)) {
                            mApplicationState.url = currentValue;
                        } else if ("name".equals(qName)) {
                            mApplicationState.appName = currentValue;
                        }
                        currentValue = null;
                    }
                };

                SAXParserFactory factory = SAXParserFactory.newInstance();

                SAXParser parser;
                try {
                    URL mURL = new URL(url);
                    android.util.Log.d("XXXXXXXXXX", "getstatus = " + mURL);
                    HttpURLConnection urlConnection = (HttpURLConnection) mURL
                            .openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.setReadTimeout(3000);
                    urlConnection.setRequestProperty("Accept",
                            "application/xml; charset=utf-8");
                    if (mApplicationState != null
                            && !TextUtils.isEmpty(mApplicationState.token)) {
                        urlConnection.setRequestProperty("Authorization",
                                mApplicationState.token);
                        android.util.Log.d("XXXXXXXXXX", "getstatus: token = " + mApplicationState.token);
                    }
                    InputStream in = new BufferedInputStream(urlConnection
                            .getInputStream());

                    try {
                        if (urlConnection.getResponseCode() == 400) {
                            launchApplication("join", mCurrentReceiverUrl);
                        } else {
                            Scanner s = new Scanner(in).useDelimiter("\\A");
                            String xml = s.hasNext() ? s.next() : "";
    
                            parser = factory.newSAXParser();
                            parser.parse(new ByteArrayInputStream(xml.getBytes()),
                                    dh);
                        }
                    } finally {
                        in.close();
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (callback != null) {
                    callback.onResult();
                }

            }
        });
    }

    @Override
    public void getStatus() {
        getStatus(null);
    }

    @Override
    public void removeNamespace(String namespace) {
        if (TextUtils.isEmpty(namespace)) {
            return;
        }

        synchronized (mNamespaces) {
            mNamespaces.remove(namespace);
        }
    }

    @Override
    public void onSocketConnectedInternal() {
        FlingMediaRouteProvider.getInstance(mContext)
                .getFlingDeviceControllerMap().get(mFlingDevice.getDeviceId()).isConnecting = false;
        mFlingSrvController.onConnected();
        mIsConnected = true;
    }

    private Integer controlerId;

    @Override
    public void generateId() {
        synchronized (controlerId) {
            controlerId = Integer.valueOf(1 + controlerId.intValue());
        }
    }

    @Override
    public void releaseReference() {
        synchronized (controlerId) {
            boolean flag;
            if (controlerId.intValue() <= 0) {
                flag = false;
            } else {
                controlerId = Integer.valueOf(-1 + controlerId.intValue());
                if (controlerId.intValue() == 0)
                    flag = true;
                else
                    flag = false;
            }

            if (flag) {
                mDisposed = true;

                android.util.Log.d("XXXXXXXXX", "[" + mFlingDevice
                        + "] *** disposing ***");
                stopHeartbeat();
                mDisposed = true;

                if (mFlingWebsocket != null && mFlingWebsocket.isOpen())
                    mFlingWebsocket.close();
            }
        }
    }

    private boolean mDisposed = false;
    private boolean mIsConnected = false;
    private boolean mIsConnecting = false;

    @Override
    public boolean isDisposed() {
        return mDisposed;
    }

    @Override
    public boolean isConnected() {
        return mIsConnected;
    }

    @Override
    public boolean isConnecting() {
        return mIsConnecting;
    }

    @Override
    public void reconnectToDevice(String lastAppId) {
        connectDevice();
    }

    @Override
    public void launchApplication(String applicationId,
            boolean relaunchIfRunning) {
        FlingDeviceService.launchApplication(mContext, this, applicationId, relaunchIfRunning);
    }

    @Override
    public void joinApplication(String url) {
        FlingDeviceService.joinApplication(mContext, this, url);
    }

    @Override
    public void leaveApplication() {
        FlingDeviceService.leaveApplication(mContext, this);
    }

    @Override
    public void stopApplication() {
        stopHeartbeat();
        FlingDeviceService.stopApplication(mContext, this);
    }

    @Override
    public void requestStatus() {
        FlingDeviceService.requestStatus(mContext, this);
    }

    @Override
    public void setVolume(double volume, boolean mute) {
        FlingDeviceService.setVolume(mContext, this, volume, mute);
    }

    @Override
    public void sendMessageInternal(String namespace, String message,
            long requestId) {
        FlingDeviceService.sendTextMessage(mContext, this, namespace, message,
                requestId, "");
    }

    @Override
    public void setMessageReceivedCallbacks(String namespace) {
        FlingDeviceService.setMessageReceivedCallbacks(mContext, this,
                namespace);
    }

    @Override
    public void removeMessageReceivedCallbacks(String namespace) {
        FlingDeviceService.removeMessageReceivedCallbacks(mContext, this,
                namespace);
    }

    private void onSocketError(int socketError) {
        mIsConnected = false;
        mFlingSrvController.onDisconnected(socketError);
        mFlingSrvController.onApplicationDisconnected(socketError);
    }

    abstract class StateCallback {
        abstract void onResult();
    }

    class ApplicationState {
        String url;
        String appAddress;
        String token;
        String appName;
        String state;
        List<String> additionalData;
    }

    @Override
    public void onMessageReceived(String message) {
        FlingDeviceService.procReceivedMessage(mContext, this, message);
    }

    @Override
    public void onReceivedMessage(String message) {
        try {
            android.util.Log.d("QQQQQQQQQQQQQ", "dial onReceivedMessage = " + message);
            JSONObject json = new JSONObject(message);
            String namespace = json.optString("namespace", "");
            String data = json.optString("data", "");
            mFlingSrvController.notifyOnMessageReceived(namespace, data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
