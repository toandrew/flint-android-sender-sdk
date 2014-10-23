package tv.matchstick.server.fling;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tv.matchstick.fling.FlingDevice;
import tv.matchstick.server.fling.socket.FlingSocket;
import tv.matchstick.server.fling.socket.FlingSocketListener;
import tv.matchstick.server.fling.socket.data.FlingMessage;
import tv.matchstick.server.utils.LOG;

abstract class DeviceFilter {
    private static final LOG mLogs = new LOG("DeviceFilter");
    private static AtomicLong b = new AtomicLong(0L);
    private final Context mContext;
    private final String d;
    private final List e = new ArrayList();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Set mDiscoveryCriterias;

    public DeviceFilter(Context context, Set set, String s) {
        mContext = context;
        mDiscoveryCriterias = new HashSet(set);
        d = s;
    }

    static Context getContext_a(DeviceFilter awz1) {
        return awz1.mContext;
    }

    static AtomicLong a() {
        return b;
    }

    static LOG getLogs() {
        return mLogs;
    }

    static String b(DeviceFilter awz1) {
        return awz1.d;
    }

    static Set getDiscoveryCriterias(DeviceFilter awz1) {
        return awz1.mDiscoveryCriterias;
    }

    static Handler getHandler(DeviceFilter awz1) {
        return awz1.mHandler;
    }

    protected abstract void setDeviceOffline(FlingDevice flingdevice);

    protected abstract void onDeviceAccepted(FlingDevice flingdevice, Set set);

    public final void reset(Set set) {
        for (Iterator iterator = e.iterator(); iterator.hasNext();)
            ((FlingDeviceManager) iterator.next()).e = false;

        e.clear();
        mDiscoveryCriterias = new HashSet(set);
    }

    public final void connectOrAcceptDevice(FlingDevice flingdevice) {
        FlingDeviceManager axb1 = new FlingDeviceManager(this, flingdevice);
        if (!axb1.mNoApp || !axb1.mNoNamespace) {
            try {
                mLogs.d("connecting to: %s:%d (%s)", axb1.mFlingDevice
                        .getIpAddress().toString(), axb1.mFlingDevice
                        .getServicePort(), axb1.mFlingDevice.getFriendlyName());

                axb1.mFlingSocket.connect(axb1.mFlingDevice.getIpAddress(),
                        axb1.mFlingDevice.getServicePort());
            } catch (Exception ioexception) {
                mLogs.e(ioexception, "Exception while connecting socket",
                        new Object[0]);
            }
        } else {
            mLogs.d("accept device to: %s:%d (%s)", axb1.mFlingDevice
                    .getIpAddress().toString(), axb1.mFlingDevice
                    .getServicePort(), axb1.mFlingDevice.getFriendlyName());

            axb1.acceptDevice(axb1.mFlingDevice,
                    axb1.mDeviceFilter.mDiscoveryCriterias);
        }
        e.add(axb1);
    }

    final class FlingDeviceManager implements FlingSocketListener {
        final FlingSocket mFlingSocket;
        final FlingDevice mFlingDevice;
        boolean mNoApp;
        boolean mNoNamespace;
        boolean e;
        final DeviceFilter mDeviceFilter;
        private final String g;
        private final AppInfoHelper h = new AppInfoHelper();
        private final JSONArray i = new JSONArray();

        public FlingDeviceManager(DeviceFilter deviceFilter,
                FlingDevice flingDevice) {
            super();

            mDeviceFilter = deviceFilter;

            mNoApp = true;
            mNoNamespace = false;
            e = true;
            mFlingSocket = new FlingSocket(
                    DeviceFilter.getContext_a(deviceFilter), this);
            mFlingDevice = flingDevice;
            Object aobj[] = new Object[2];
            aobj[0] = DeviceFilter.b(deviceFilter);
            aobj[1] = Long.valueOf(DeviceFilter.a().incrementAndGet());
            g = String.format("%s-%d", aobj);
            if (DeviceFilter.getDiscoveryCriterias(deviceFilter).size() > 0) {
                Iterator iterator = DeviceFilter.getDiscoveryCriterias(
                        deviceFilter).iterator();
                boolean noNameSpace = true;
                boolean noApp = true;
                while (iterator.hasNext()) {
                    DiscoveryCriteria criteria = (DiscoveryCriteria) iterator
                            .next();
                    Set set = Collections
                            .unmodifiableSet(criteria.mNamespaceList);
                    if (criteria.mAppid != null) {
                        i.put(criteria.mAppid);
                        noApp = false;
                    }
                    if (set.size() > 0)
                        noNameSpace = false;
                }
                if (noApp)
                    setNoApp();
                if (noNameSpace)
                    setNoNameSpace();
            }
        }

        private void sendMessage(String namespace, String message)
                throws Exception {
            DeviceFilter.getLogs().d(
                    "Sending text message to %s: (ns=%s, dest=%s) %s",
                    mFlingDevice.getFriendlyName(), namespace, "receiver-0",
                    message);
            FlingMessage msg = new FlingMessage();
            msg.setProtocolVersion(0);
            msg.setSourceId(g);
            msg.setDestinationId("receiver-0");
            msg.setNamespace(namespace);
            msg.setPayloadMessage(message);
            byte abyte0[] = msg.buildJson().toString().getBytes("UTF-8");
            mFlingSocket.send(ByteBuffer.wrap(abyte0));
        }

        private void setNoApp() {
            mNoApp = true;
            if (mNoApp && mNoNamespace)
                updateStatus();
        }

        private void setNoNameSpace() {
            mNoNamespace = true;
            if (mNoApp && mNoNamespace)
                updateStatus();
        }

        private void updateStatus() {
            HashSet hashset;
            if (mFlingSocket.isConnected()) {
                Iterator iterator;
                DiscoveryCriteria aty1;
                AppInfoHelper axa1;
                String s;
                try {
                    sendMessage("urn:x-cast:com.google.cast.tp.connection",
                            (new JSONObject()).put("type", "CLOSE").toString());
                } catch (IOException ioexception) {
                    DeviceFilter.getLogs().d(ioexception,
                            "Failed to send disconnect message", new Object[0]);
                } catch (JSONException jsonexception) {
                    DeviceFilter.getLogs()
                            .e(jsonexception,
                                    "Failed to build disconnect message",
                                    new Object[0]);
                } catch (Exception e) {
                    e.printStackTrace();
                    DeviceFilter.getLogs().d(e,
                            "Failed to send disconnect message", new Object[0]);
                }
                mFlingSocket.disconnect();
            }
            hashset = new HashSet();
            Iterator iterator = DeviceFilter.getDiscoveryCriterias(
                    mDeviceFilter).iterator();
            do {
                if (!iterator.hasNext())
                    break;
                DiscoveryCriteria criteria = (DiscoveryCriteria) iterator
                        .next();

                boolean flag;
                // if we need setNoApp later, the mAppAvailabityList must
                // contains
                if (/*
                     * (criteria.mAppid == null ||
                     * h.mAppAvailabityList.contains(criteria.mAppid)) &&
                     */h.mAppNamespaceList.containsAll(Collections
                        .unmodifiableSet(criteria.mNamespaceList)))
                    flag = true;
                else
                    flag = false;
                if (flag)
                    hashset.add(criteria);
            } while (true);

            if (e && hashset.size() > 0) {
                acceptDevice(mFlingDevice, hashset);
                return;
            }

            DeviceFilter.getLogs().d("rejected device: %s", mFlingDevice);
            return;
        }

        public final void onConnected() {
            try {
                sendMessage(
                        "urn:x-cast:com.google.cast.tp.connection",
                        (new JSONObject())
                                .put("type", "CONNECT")
                                .put("origin",
                                        new JSONObject().put("package",
                                                DeviceFilter.b(mDeviceFilter)))
                                .toString());

                if (!mNoApp)
                    sendMessage(
                            "urn:x-cast:com.google.cast.receiver",
                            (new JSONObject()).put("requestId", 1)
                                    .put("type", "GET_APP_AVAILABILITY")
                                    .put("appId", i).toString());

                if (!mNoNamespace)
                    sendMessage(
                            "urn:x-cast:com.google.cast.receiver",
                            (new JSONObject()).put("requestId", 2)
                                    .put("type", "GET_STATUS").toString());

                return;
                // } catch (IOException ioexception)
                // {
                // DeviceFilter_awz.b().c(ioexception,
                // "Failed to send messages",
                // new Object[0]);
                // return;
            } catch (JSONException jsonexception) {
                DeviceFilter.getLogs().e(jsonexception,
                        "Failed to build messages", new Object[0]);
            } catch (Exception e) {
                DeviceFilter.getLogs().e(e, "Failed to send messages",
                        new Object[0]);
                e.printStackTrace();
            }
        }

        public final void onConnectionFailed(int j) {
            Object aobj[] = new Object[4];
            aobj[0] = mFlingDevice.getIpAddress().toString();
            aobj[1] = Integer.valueOf(mFlingDevice.getServicePort());
            aobj[2] = mFlingDevice.getFriendlyName();
            aobj[3] = Integer.valueOf(j);
            DeviceFilter.getLogs().w(
                    "Connection to %s:%d (%s) failed with error %d", aobj);

            DeviceFilter.getHandler(mDeviceFilter).post(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    mDeviceFilter.setDeviceOffline(mFlingDevice);
                }

            });
        }

        final void acceptDevice(final FlingDevice flingdevice, final Set set) {
            DeviceFilter.getHandler(mDeviceFilter).post(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    mDeviceFilter.onDeviceAccepted(flingdevice, set);
                }

            });
        }

        public final void onMessageReceived(ByteBuffer receivedMessage) {
            long requestId;
            DeviceFilter.getLogs().d("onMessageReceived:in[%s]", g);

            FlingMessage flingMessage = new FlingMessage(
                    receivedMessage.array());

            if (flingMessage.getPayloadType() != 0) {
                return;
            }

            String message = flingMessage.getMessage();
            Log.d("DeviceFilter", "onMessageReceived:" + message);
            try {
                JSONObject jsonobject = new JSONObject(message);
                requestId = jsonobject.optLong("requestId", -1L);
                if (requestId == -1L) {
                    return;
                }
                if (requestId != 1L) {
                    if (requestId != 2L) {
                        DeviceFilter.getLogs().d(
                                "Unrecognized request ID: " + requestId);
                        return;
                    }
                    h.fillNamespaceList(jsonobject);
                    setNoNameSpace();
                    return;
                }
                h.fillAppAvailabityList(jsonobject);
                setNoApp();
            } catch (JSONException jsonexception) {
                Object aobj3[] = new Object[1];
                aobj3[0] = jsonexception.getMessage();
                DeviceFilter.getLogs().e("Failed to parse response: %s", aobj3);
            }

        }

        public final void onDisconnected(int statusCode) {
            DeviceFilter.getLogs().d("Device filter disconnected");
        }

        final class AppInfoHelper {
            final Set mAppNamespaceList;
            final Set mAppAvailabityList;

            private AppInfoHelper() {
                mAppNamespaceList = new HashSet();
                mAppAvailabityList = new HashSet();
            }

            public final void fillNamespaceList(JSONObject jsonobject) {
                try {
                    JSONArray jsonarray = jsonobject.getJSONObject("status")
                            .getJSONArray("applications");
                    int i = 0;
                    while (i < jsonarray.length()) {
                        JSONArray jsonarray1 = jsonarray.getJSONObject(i)
                                .getJSONArray("namespaces");
                        int j = 0;
                        while (j < jsonarray1.length()) {
                            String name = jsonarray1.getJSONObject(j)
                                    .getString("name");
                            mAppNamespaceList.add(name);
                            j++;
                        }
                        i++;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();

                    DeviceFilter.getLogs().d(
                            "No namespaces found in receiver response: %s",
                            e.getMessage());
                }
            }

            public final void fillAppAvailabityList(JSONObject jsonobject) {
                try {
                    JSONObject jsonobject1 = jsonobject
                            .getJSONObject("availability");
                    Iterator iterator = jsonobject1.keys();
                    do {
                        if (!iterator.hasNext())
                            break;
                        String appId = (String) iterator.next();
                        if ("APP_AVAILABLE"
                                .equals(jsonobject1.optString(appId)))
                            mAppAvailabityList.add(appId);
                    } while (true);
                } catch (JSONException jsonexception) {
                    DeviceFilter
                            .getLogs()
                            .d("No app availabities found in receiver response: %s",
                                    jsonexception.getMessage());
                }
            }
        }
    }

}
