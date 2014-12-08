package tv.matchstick.server.flint;

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
import tv.matchstick.flint.ApplicationMetadata;
import tv.matchstick.flint.ConnectionResult;
import tv.matchstick.flint.Flint;
import tv.matchstick.flint.FlintDevice;
import tv.matchstick.flint.service.FlintDeviceService;
import tv.matchstick.server.flint.bridge.IFlintSrvController;
import tv.matchstick.server.flint.socket.FlintSocketListener;
import tv.matchstick.server.flint.socket.FlintWebsocket;
import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;

public class FlintDialController implements FlintSocketListener {
    private static final int NUM_OF_THREADS = 20;
    private static final LOG log = new LOG("FlintDialController");
    private Context mContext;
    private Handler mHandler;
    private FlintDevice mFlintDevice;
    private final IFlintSrvController mFlintSrvController;
    private ApplicationState mApplicationState = new ApplicationState();
    private int mHeartbeatInterval = 1000;
    private Set<String> mNamespaces = new HashSet<String>();
    private FlintWebsocket mFlintWebsocket;
    private String mCurrentReceiverUrl;
    private boolean mUseIpc = true;

    private boolean mDisposed = false;
    private boolean mIsConnected = false;
    private boolean mIsConnecting = false;

    private String mLastToken;
    private String mLastAddress;

    private Executor mExecutor = Executors.newFixedThreadPool(NUM_OF_THREADS,
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread th = new Thread(r);
                    th.setName("DIAL Controller Thread");
                    return th;
                }
            });

    public FlintDialController(Context context, Handler handler,
            FlintDevice flintDevice, IFlintSrvController controller) {
        mContext = context;
        mHandler = handler;
        mFlintDevice = flintDevice;
        mFlintSrvController = controller;
    }

    public final void connectDevice() {
        FlintDeviceService.connectFlintDevice(mContext, this);
    }

    @Override
    public void onConnected() {
        mFlintSrvController.onApplicationConnected(new ApplicationMetadata(
                mApplicationState.additionalData), "", "", false);
    }

    private void onDialConnected() {
        FlintDeviceService.onSocketConnected(mContext, this);
    }

    @Override
    public void onDisconnected(int reason) {
        FlintDeviceService.onSocketDisconnected(mContext, this, reason);
    }

    public void connectToDeviceInternal() {
        log.d("connectToDeviceInternal");
        mIsConnecting = true;
        getStatus(new StateCallback() {
            @Override
            void onResult() {
                log.d("connectToDeviceInternal: mApplicationState.state = %s",
                        mApplicationState.state);
                if (mApplicationState.state != null) {
                    onDialConnected();
                } else {
                    onConnectionFailed();
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
        // mFlintSrvController.onDisconnected(socketError);
        // mFlintSrvController.onConnectionFailed();
    }

    public void stopApplicationInternal() {
        log.d("stopApplicationInternal");

        final String url = buildAppUrl();
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    URL mURL;
                    if (!TextUtils.isEmpty(mApplicationState.appAddress)) {
                        mURL = new URL(url + "/" + mApplicationState.appAddress);
                    } else if (!TextUtils.isEmpty(mLastAddress)) {
                        mURL = new URL(url + "/" + mLastAddress);
                    } else {
                        mURL = new URL(url);
                    }

                    HttpURLConnection urlConnection = (HttpURLConnection) mURL
                            .openConnection();
                    urlConnection.setRequestMethod("DELETE");

                    if (!TextUtils.isEmpty(mApplicationState.token)) {
                        urlConnection.setRequestProperty("Authorization",
                                mApplicationState.token);
                    } else if (!TextUtils.isEmpty(mLastToken)) {
                        urlConnection.setRequestProperty("Authorization",
                                mLastToken);
                    }
                    urlConnection.connect();
                    final int response = urlConnection.getResponseCode();

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (response == 200) {
                                mFlintSrvController.onRequestStatus(0);
                                release();
                            } else {
                                mFlintSrvController.onRequestStatus(1);
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

    public void joinApplicationInternal(String url, boolean useIpc) {
        log.d("joinApplicationInternal");
        if (TextUtils.isEmpty(url))
            launchApplication("join", mCurrentReceiverUrl, useIpc);
        else
            launchApplication("join", url, useIpc);
    }

    public void sendTextMessage(String namespace, String message) {
        log.d("sendTextMessage: namespace = %s; message = %s", namespace,
                message);

        if (mFlintWebsocket != null && mFlintWebsocket.isOpen()) {
            mFlintWebsocket.sendText(namespace, message);
        }
    }

    public void launchApplicationInternal(final String url,
            final boolean relaunch, final boolean useIpc) {
        log.d("launchApplicationInternal: relaunch = %b; state = %s", relaunch,
                mApplicationState.state);
        if (TextUtils.isEmpty(mApplicationState.state)) {
            log.d("status is null, get status first");
            getStatus(new StateCallback() {
                @Override
                void onResult() {
                    if (TextUtils.isEmpty(mApplicationState.state)) {
                        mFlintSrvController.onApplicationConnectionFailed(0);
                    } else {
                        launchApplicationInternal(url, relaunch, useIpc);
                    }
                }
            });
        } else {
            if (!"stopped".equals(mApplicationState.state)) {
                if (relaunch) {
                    launchApplication("relaunch", url, useIpc);
                } else {
                    joinApplication(url);
                }
            } else {
                launchApplication("launch", url, useIpc);
            }
        }
    }

    private void launchApplication(final String type, final String receiverUrl,
            final boolean useIpc) {
        log.d("type: type = %s", type);
        final String url = buildAppUrl();
        mCurrentReceiverUrl = receiverUrl;
        mUseIpc = useIpc;
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
                    sub.put("useIpc", useIpc);
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
                                            requestLaunchState();
                                        } else {
                                            mFlintSrvController
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
                if (mApplicationState.state.equals("running")) {
                    if (!TextUtils.isEmpty(mApplicationState.url)) {
                        if (mFlintWebsocket != null && mFlintWebsocket.isOpen()) {
                            mFlintWebsocket.close();
                        }
                        mFlintWebsocket = new FlintWebsocket(
                                FlintDialController.this,
                                URI.create(mApplicationState.url + "/senders/"
                                        + mApplicationState.token));
                        mFlintWebsocket.connect();
                        mDisposed = false;
                        startHeartbeat();
                        log.d("launch success");
                        return;
                    } else if (!mUseIpc) {
                        if (mFlintWebsocket != null && mFlintWebsocket.isOpen()) {
                            mFlintWebsocket.close();
                        }
                        mDisposed = false;
                        startHeartbeat();
                        log.d("launch success");
                        return;
                    }
                } else if (mApplicationState.state.equals("stopped")) {
                    mFlintSrvController.onApplicationConnectionFailed(1);
                    log.d("launch time out");
                } else {
                    mHandler.postDelayed(mRequestLaunchState, mHeartbeatInterval);
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
                if ("stopped".equals(mApplicationState.state)
                        || !Flint.FlintApi.getApplicationId().equals(
                                mApplicationState.appName)) {
                    release();
                    onDisconnected(ConnectionResult.NETWORK_ERROR);
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
            log.d("heartbeat time out");
            onSocketDisconnectedInternal(ConnectionResult.NETWORK_ERROR);
        }
    };

    private void stopHeartbeat() {
        mHandler.removeCallbacks(mRequestLaunchState);
        mHandler.removeCallbacks(mTimeoutRunnable);
        mHandler.removeCallbacks(mHeartbeatRunnable);
    }

    public void release() {
        log.d("release: disposed = %b", mDisposed);
        if (!mDisposed) {
            stopHeartbeat();
            mLastToken = mApplicationState.token;
            mLastAddress = mApplicationState.appAddress;
            mApplicationState.reset();
            mDisposed = true;
            if (mFlintWebsocket != null && mFlintWebsocket.isOpen())
                mFlintWebsocket.close();
        }
    }

    private String buildAppUrl() {
        String id = Flint.FlintApi.getApplicationId();
        String address = mFlintDevice.getIpAddress().getHostAddress();
        int port = mFlintDevice.getServicePort();
        String url = "http://" + address + ":" + port + "/apps/" + id;
        return url;
    }

    private String buildSystemUrl() {
        String address = mFlintDevice.getIpAddress().getHostAddress();
        int port = mFlintDevice.getServicePort();
        String url = "http://" + address + ":" + port + "/system/control";
        return url;
    }

    public void leaveApplicationInternal() {
        release();
        // mFlintSrvController.onInvalidRequest();
        mFlintSrvController.onApplicationDisconnected(0);
    }

    public void onSocketDisconnectedInternal(int socketError) {
        log.d("Socket disconnected: " + socketError);
        mIsConnected = false;
        mIsConnecting = false;
        release();
        mFlintSrvController.onDisconnected(socketError);
        mFlintSrvController.onApplicationDisconnected(socketError);
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
                        } else if ("channelBaseUrl".equals(qName)) {
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
                            mApplicationState.additionalData.put(qName,
                                    currentValue);
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
                            launchApplication("join", mCurrentReceiverUrl,
                                    mUseIpc);
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
        if (FlintMediaRouteProvider.getInstance(mContext)
                .getFlintDeviceControllerMap() != null
                && FlintMediaRouteProvider.getInstance(mContext)
                        .getFlintDeviceControllerMap()
                        .get(mFlintDevice.getDeviceId()) != null)
            FlintMediaRouteProvider.getInstance(mContext)
                    .getFlintDeviceControllerMap()
                    .get(mFlintDevice.getDeviceId()).isConnecting = false;
        else
            log.d("not find connected device");
        mFlintSrvController.onConnected();
        mIsConnected = true;
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
            boolean relaunchIfRunning, boolean useIpc) {
        FlintDeviceService.launchApplication(mContext, this, applicationId,
                relaunchIfRunning, useIpc);
    }

    public void joinApplication(String url) {
        FlintDeviceService.joinApplication(mContext, this, url, mUseIpc);
    }

    public void joinApplication(String url, boolean useIpc) {
        FlintDeviceService.joinApplication(mContext, this, url, useIpc);
    }

    public void leaveApplication() {
        FlintDeviceService.leaveApplication(mContext, this);
    }

    public void stopApplication() {
        FlintDeviceService.stopApplication(mContext, this);
    }

    public void requestStatus() {
        FlintDeviceService.requestStatus(mContext, this);
    }

    public void setVolume(double volume, boolean mute) {
        FlintDeviceService.setVolume(mContext, this, volume, mute);
    }

    public void sendMessageInternal(String namespace, String message) {
        FlintDeviceService.sendTextMessage(mContext, this, namespace, message);
    }

    public void setMessageReceivedCallbacks(String namespace) {
        FlintDeviceService.setMessageReceivedCallbacks(mContext, this,
                namespace);
    }

    public void removeMessageReceivedCallbacks(String namespace) {
        FlintDeviceService.removeMessageReceivedCallbacks(mContext, this,
                namespace);
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

        private void reset() {
            url = "";
            appAddress = "";
            token = "";
            appName = "";
            state = "";
            if (additionalData != null) {
                additionalData.clear();
            }
        }
    }

    @Override
    public void onMessageReceived(String message) {
        FlintDeviceService.procReceivedMessage(mContext, this, message);
    }

    public void onReceivedMessage(String message) {
        try {
            log.d("onReceivedMessage, message = %s", message);
            JSONObject json = new JSONObject(message);
            String namespace = json.optString("namespace", "");
            String payload = json.optString("payload", "");
            mFlintSrvController.notifyOnMessageReceived(namespace, payload);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionFailed() {
        mFlintSrvController.onConnectionFailed();
    }
}
