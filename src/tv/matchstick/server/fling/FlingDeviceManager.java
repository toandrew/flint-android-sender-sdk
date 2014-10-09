
package tv.matchstick.server.fling;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tv.matchstick.fling.FlingDevice;
import tv.matchstick.server.fling.socket.FlingSocket;
import tv.matchstick.server.fling.socket.FlingSocketListener;
import tv.matchstick.server.fling.socket.data.BinaryPayload;
import tv.matchstick.server.fling.socket.data.FlingMessage;
import tv.matchstick.server.utils.LOG;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

final class FlingDeviceManager implements FlingSocketListener {
    final FlingSocket mFlingSocket;
    final FlingDevice mFlingDevice;
    boolean mNoApp;
    boolean mNoNamespace;
    boolean e;
    final DeviceFilter mDeviceFilter;
    private final String g;
    private final AppInfoHelper h = new AppInfoHelper((byte) 0);
    private final JSONArray i = new JSONArray();

    public FlingDeviceManager(DeviceFilter deviceFilter, FlingDevice flingDevice)
    {
        super();

        mDeviceFilter = deviceFilter;

        mNoApp = true;
        mNoNamespace = false;
        e = true;
        mFlingSocket = new FlingSocket(DeviceFilter.getContext_a(deviceFilter), this);
        mFlingDevice = flingDevice;
        Object aobj[] = new Object[2];
        aobj[0] = DeviceFilter.b(deviceFilter);
        aobj[1] = Long.valueOf(DeviceFilter.a().incrementAndGet());
        g = String.format("%s-%d", aobj);
        if (DeviceFilter.getDiscoveryCriterias(deviceFilter).size() > 0)
        {
            Iterator iterator = DeviceFilter.getDiscoveryCriterias(deviceFilter).iterator();
            boolean noNameSpace = true;
            boolean noApp = true;
            while (iterator.hasNext())
            {
                DiscoveryCriteria criteria = (DiscoveryCriteria) iterator.next();
                Set set = Collections.unmodifiableSet(criteria.mNamespaceList);
                if (criteria.mAppid != null)
                {
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

    private void sendMessage(String namespace, String message) throws Exception
    {
        DeviceFilter.getLogs().d("Sending text message to %s: (ns=%s, dest=%s) %s", mFlingDevice.getFriendlyName(), namespace, "receiver-0", message);
        FlingMessage msg = new FlingMessage();
        msg.setProtocolVersion(0);
        msg.setSourceId(g);
        msg.setDestinationId("receiver-0");
        msg.setNamespace(namespace);
        msg.setPayloadMessage(message);
        byte abyte0[] = msg.buildJson().toString().getBytes("UTF-8");
        mFlingSocket.send(ByteBuffer.wrap(abyte0));
    }

    private void setNoApp()
    {
        mNoApp = true;
        if (mNoApp && mNoNamespace)
            updateStatus();
    }

    private void setNoNameSpace()
    {
        mNoNamespace = true;
        if (mNoApp && mNoNamespace)
            updateStatus();
    }

    private void updateStatus()
    {
        HashSet hashset;
        if (mFlingSocket.isConnected())
        {
            Iterator iterator;
            DiscoveryCriteria aty1;
            AppInfoHelper axa1;
            String s;
            try
            {
                sendMessage("urn:x-cast:com.google.cast.tp.connection",
                        (new JSONObject()).put("type", "CLOSE").toString());
            } catch (IOException ioexception) {
                DeviceFilter.getLogs().d(ioexception, "Failed to send disconnect message",
                        new Object[0]);
            } catch (JSONException jsonexception) {
                DeviceFilter.getLogs().e(jsonexception, "Failed to build disconnect message",
                        new Object[0]);
            } catch (Exception e) {
                e.printStackTrace();
                DeviceFilter.getLogs().d(e, "Failed to send disconnect message",
                        new Object[0]);
            }
            mFlingSocket.disconnect();
        }
        hashset = new HashSet();
        Iterator iterator = DeviceFilter.getDiscoveryCriterias(mDeviceFilter).iterator();
        do
        {
            if (!iterator.hasNext())
                break;
            DiscoveryCriteria criteria = (DiscoveryCriteria) iterator.next();

            boolean flag;
            // if we need setNoApp later, the mAppAvailabityList must contains
            if (/* (criteria.mAppid == null || h.mAppAvailabityList.contains(criteria.mAppid))
                    && */h.mAppNamespaceList.containsAll(Collections
                            .unmodifiableSet(criteria.mNamespaceList)))
                flag = true;
            else
                flag = false;
            if (flag)
                hashset.add(criteria);
        } while (true);

        if (e && hashset.size() > 0)
        {
            acceptDevice(mFlingDevice, hashset);
            return;
        }

        DeviceFilter.getLogs().d("rejected device: %s", mFlingDevice);
        return;
    }

    public final void onConnected()
    {
        try
        {
            sendMessage(
                    "urn:x-cast:com.google.cast.tp.connection",
                    (new JSONObject()).put("type", "CONNECT")
                            .put("origin", new JSONObject().put("package", DeviceFilter.b(mDeviceFilter)))
                            .toString());

            if (!mNoApp)
                sendMessage("urn:x-cast:com.google.cast.receiver",
                        (new JSONObject()).put("requestId", 1)
                                .put("type", "GET_APP_AVAILABILITY").put("appId", i).toString());

            if (!mNoNamespace)
                sendMessage("urn:x-cast:com.google.cast.receiver",
                        (new JSONObject()).put("requestId", 2)
                                .put("type", "GET_STATUS").toString());

            return;
            // } catch (IOException ioexception)
            // {
            // DeviceFilter_awz.b().c(ioexception, "Failed to send messages",
            // new Object[0]);
            // return;
        } catch (JSONException jsonexception)
        {
            DeviceFilter.getLogs()
                    .e(jsonexception, "Failed to build messages", new Object[0]);
        } catch (Exception e) {
            DeviceFilter.getLogs().e(e, "Failed to send messages", new Object[0]);
            e.printStackTrace();
        }
    }

    public final void onConnectionFailed(int j)
    {
        Object aobj[] = new Object[4];
        aobj[0] = mFlingDevice.getIpAddress().toString();
        aobj[1] = Integer.valueOf(mFlingDevice.getServicePort());
        aobj[2] = mFlingDevice.getFriendlyName();
        aobj[3] = Integer.valueOf(j);
        DeviceFilter.getLogs().w("Connection to %s:%d (%s) failed with error %d", aobj);

        DeviceFilter.getHandler(mDeviceFilter).post(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                mDeviceFilter.setDeviceOffline(mFlingDevice);
            }

        });
    }

    final void acceptDevice(final FlingDevice flingdevice, final Set set)
    {
        DeviceFilter.getHandler(mDeviceFilter).post(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                mDeviceFilter.onDeviceAccepted(flingdevice, set);
            }

        });
    }

    public final void onMessageReceived(ByteBuffer receivedMessage)
    {
        long requestId;
        DeviceFilter.getLogs().d("onMessageReceived:in[%s]", g);

        FlingMessage flingMessage = new FlingMessage(receivedMessage.array());

        if (flingMessage.getPayloadType() != 0) {
            return;
        }
        
        String message = flingMessage.getMessage();
        Log.d("DeviceFilter","onMessageReceived:" + message);
        try
        {
            JSONObject jsonobject = new JSONObject(message);
            requestId = jsonobject.optLong("requestId", -1L);
            if (requestId == -1L) {
                return;
            }
            if (requestId != 1L) {
                if (requestId != 2L) {
                    DeviceFilter.getLogs().d("Unrecognized request ID: " + requestId);
                    return;
                }
                h.fillNamespaceList(jsonobject);
                setNoNameSpace();
                return;
            }
            h.fillAppAvailabityList(jsonobject);
            setNoApp();
        } catch (JSONException jsonexception)
        {
            Object aobj3[] = new Object[1];
            aobj3[0] = jsonexception.getMessage();
            DeviceFilter.getLogs().e("Failed to parse response: %s", aobj3);
        }
        
    }

    public final void onDisconnected(int statusCode)
    {
        DeviceFilter.getLogs().d("Device filter disconnected");
    }
}
