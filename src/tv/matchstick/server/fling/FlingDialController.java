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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

import tv.matchstick.client.internal.LOG;
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

public class FlingDialController implements FlingSocketListener {
    private static final int NUM_OF_THREADS = 20;
    private static final LOG log = new LOG("FlingDialController");
    private Context mContext;
    private Handler mHandler;
    private FlingDevice mFlingDevice;
    private final IFlingSrvController mFlingSrvController;
    private ApplicationState mApplicationState = new ApplicationState();
    private int mLaunchStateCounter = 0;
    private int mHeartbeatInterval = 1000;
    private Set<String> mNamespaces = new HashSet<String>();
    private FlingWebsocket mFlingWebsocket;
    private String mCurrentReceiverUrl;
    private Integer controlerId;

    private boolean mDisposed = false;
    private boolean mIsConnected = false;
    private boolean mIsConnecting = false;

    private Executor mExecutor = Executors.newFixedThreadPool(NUM_OF_THREADS,
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread th = new Thread(r);
                    th.setName("DIAL Controller Thread");
                    return th;
                }
            });

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
        mFlingSrvController.onApplicationConnected(new ApplicationMetadata(mApplicationState.additionalData),
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
    public void onDisconnected(int reason) {
        FlingDeviceService.onSocketDisconnected(mContext, this, reason);
    }

    public void connectToDeviceInternal() {
        log.d("connectToDeviceInternal");
        mIsConnecting = true;
        getStatus(new StateCallback() {
            @Override
            void onResult() {
                log.d("connectToDeviceInternal: mApplicationState.state = %s", mApplicationState.state);
                if (mApplicationState.state != null) {
                    onDialConnected();
                } else {
                    onConnectionFailed(0);
                }
                mIsConnecting = false;
            }
        });
    }

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

    public void onSocketConnectionFailedInternal(int socketError) {
        // mFlingSrvController.onDisconnected(socketError);
        // mFlingSrvController.onConnectionFailed();
    }

    public void stopApplicationInternal() {
        log.d("stopApplicationInternal");

        final String url = buildAppUrl();
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

    public void joinApplicationInternal(String url) {
        log.d("joinApplicationInternal");
        if (TextUtils.isEmpty(url))
            launchApplication("join", mCurrentReceiverUrl);
        else
            launchApplication("join", url);
    }

    public void sendTextMessage(String namespace, String message, long id,
            String transportId) {
        log.d("sendTextMessage: message = %s", message);

        if (mFlingWebsocket != null && mFlingWebsocket.isOpen()) {
            mFlingWebsocket.sendText(namespace, "meida", String.valueOf(id),
                    message);
        }
    }

    public void launchApplicationInternal(String url, final boolean relaunch) {
        log.d("launchApplicationInternal: relaunch = %b", relaunch);
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
        log.d("type: type = %s", type);
        final String url = buildAppUrl();
        mCurrentReceiverUrl = receiverUrl;
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
                                mApplicationState.token = object.optString(
                                        "token", "");
                                mHeartbeatInterval = object.optInt("interval",
                                        1000);
                                log.d("token = %s", mApplicationState.token);
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mApplicationState.token.length() > 0) {
                                            mLaunchStateCounter = 0;
                                            requestLaunchState();
                                        } else {
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
                if (mApplicationState.state.equals("running")
                        && !TextUtils.isEmpty(mApplicationState.url)) {
                    if (mFlingWebsocket != null) {
                        mFlingWebsocket.close();
                    }
                    mFlingWebsocket = new FlingWebsocket(
                            FlingDialController.this,
                            URI.create(mApplicationState.url + "/senders/"
                                    + mApplicationState.token));
                    mFlingWebsocket.connect();
                    startHeartbeat();
                    log.d("launch success");
                } else {
                    if (mLaunchStateCounter < 10) {
                        mLaunchStateCounter++;
                        mHandler.postDelayed(mRequestLaunchState, 1000);
                    } else {
                        log.d("launch time out");
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

    public void leaveApplicationInternal() {
        stopHeartbeat();
        // mFlingSrvController.onInvalidRequest();
        mFlingSrvController.onApplicationDisconnected(0);
    }

    public void onSocketDisconnectedInternal(int socketError) {
        mIsConnected = false;
        mIsConnecting = false;
        mFlingSrvController.onDisconnected(socketError);
        mFlingSrvController.onApplicationDisconnected(socketError);
    }

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
                    boolean parseAdditionalData = false;

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
                        } else if ("additionalData".equals(qName)) {
                            parseAdditionalData = true;
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
                        } else if ("additionalData".equals(qName)) {
                            parseAdditionalData = false;
                        }
                        if (parseAdditionalData) {
                            if (mApplicationState.additionalData == null) {
                                mApplicationState.additionalData = new HashMap<String, String>();
                            }
                            mApplicationState.additionalData.put(qName, currentValue);
                        }
                        currentValue = null;
                    }
                };

                SAXParserFactory factory = SAXParserFactory.newInstance();

                SAXParser parser;
                try {
                    URL mURL = new URL(url);
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
                    }
                    InputStream in = new BufferedInputStream(urlConnection
                            .getInputStream());

                    try {
                        if (urlConnection.getResponseCode() == 400) {
                            log.d("token dispose, join");
                            launchApplication("join", mCurrentReceiverUrl);
                        } else {
                            Scanner s = new Scanner(in).useDelimiter("\\A");
                            String xml = s.hasNext() ? s.next() : "";

                            parser = factory.newSAXParser();
                            parser.parse(
                                    new ByteArrayInputStream(xml.getBytes()),
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

    public void getStatus() {
        getStatus(null);
    }

    public void removeNamespace(String namespace) {
        if (TextUtils.isEmpty(namespace)) {
            return;
        }

        synchronized (mNamespaces) {
            mNamespaces.remove(namespace);
        }
    }

    public void onSocketConnectedInternal() {
        log.d("onSocketConnectedInternal");
        if (FlingMediaRouteProvider.getInstance(mContext)
                .getFlingDeviceControllerMap() != null && FlingMediaRouteProvider.getInstance(mContext)
                .getFlingDeviceControllerMap().get(mFlingDevice.getDeviceId()) != null)
            FlingMediaRouteProvider.getInstance(mContext)
                    .getFlingDeviceControllerMap().get(mFlingDevice.getDeviceId()).isConnecting = false;
        else
            log.d("not find connected device");
        mFlingSrvController.onConnected();
        mIsConnected = true;
    }

    public void generateId() {
        synchronized (controlerId) {
            controlerId++;
        }
    }

    public void releaseReference() {
        synchronized (controlerId) {
            boolean flag;
            if (controlerId.intValue() <= 0) {
                flag = false;
            } else {
                controlerId--;
                if (controlerId.intValue() == 0)
                    flag = true;
                else
                    flag = false;
            }

            if (flag) {
                mDisposed = true;
                log.d("releaseReference, disposed");
                stopHeartbeat();
                mDisposed = true;

                if (mFlingWebsocket != null && mFlingWebsocket.isOpen())
                    mFlingWebsocket.close();
            }
        }
    }

    public boolean isDisposed() {
        return mDisposed;
    }

    public boolean isConnected() {
        return mIsConnected;
    }

    public boolean isConnecting() {
        return mIsConnecting;
    }

    public void reconnectToDevice(String lastAppId) {
        // connectDevice();
    }

    public void launchApplication(String applicationId,
            boolean relaunchIfRunning) {
        FlingDeviceService.launchApplication(mContext, this, applicationId,
                relaunchIfRunning);
    }

    public void joinApplication(String url) {
        FlingDeviceService.joinApplication(mContext, this, url);
    }

    public void leaveApplication() {
        FlingDeviceService.leaveApplication(mContext, this);
    }

    public void stopApplication() {
        stopHeartbeat();
        FlingDeviceService.stopApplication(mContext, this);
    }

    public void requestStatus() {
        FlingDeviceService.requestStatus(mContext, this);
    }

    public void setVolume(double volume, boolean mute) {
        FlingDeviceService.setVolume(mContext, this, volume, mute);
    }

    public void sendMessageInternal(String namespace, String message,
            long requestId) {
        FlingDeviceService.sendTextMessage(mContext, this, namespace, message,
                requestId, "");
    }

    public void setMessageReceivedCallbacks(String namespace) {
        FlingDeviceService.setMessageReceivedCallbacks(mContext, this,
                namespace);
    }

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
        Map<String, String> additionalData;
    }

    @Override
    public void onMessageReceived(String message) {
        FlingDeviceService.procReceivedMessage(mContext, this, message);
    }

    public void onReceivedMessage(String message) {
        try {
            log.d("onReceivedMessage, message = %s", message);
            JSONObject json = new JSONObject(message);
            String namespace = json.optString("namespace", "");
            String data = json.optString("data", "");
            mFlingSrvController.notifyOnMessageReceived(namespace, data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
