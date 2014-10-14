package tv.matchstick.client.internal;

import tv.matchstick.fling.ApplicationMetadata;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IFlingDeviceControllerListener extends IInterface {

    public abstract void onDisconnected(int statusCode) throws RemoteException;

    public abstract void onApplicationConnected(
            ApplicationMetadata applicationMetadata, String applicationId,
            String sessionId, boolean relaunched) throws RemoteException;

    public abstract void postApplicationConnectionResult(int statusCode)
            throws RemoteException;

    public abstract void notifyApplicationStatusOrVolumeChanged(String status,
            double volume, boolean muted) throws RemoteException;

    public abstract void onMessageReceived(String namespace, String message)
            throws RemoteException;

    public abstract void onReceiveBinary(String namespace,
            byte[] binary) throws RemoteException;

    public abstract void onRequestResult(int result) throws RemoteException;

    public abstract void onRequestStatus(int status) throws RemoteException;

    public abstract void onApplicationDisconnected(int statusCode)
            throws RemoteException;

    public abstract void requestCallback(String namespace, long requestId,
            int statusCode) throws RemoteException;

    public abstract void requestCallback(String namespace, long requestId)
            throws RemoteException;

    public static abstract class Stub extends Binder implements
            IFlingDeviceControllerListener {
        public Stub() {
            attachInterface(this,
                    "tv.matchstick.fling.internal.IFlingDeviceControllerListener");
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
            switch (code) {
            case IBinder.LAST_CALL_TRANSACTION:
                reply.writeString("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
                return true;
            case 1:
                data.enforceInterface("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
                onDisconnected(data.readInt());
                return true;
            case 2:
                data.enforceInterface("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
                ApplicationMetadata metadata;
                if (data.readInt() != 0)
                    metadata = (ApplicationMetadata) ApplicationMetadata.CREATOR
                            .createFromParcel(data);
                else
                    metadata = null;
                String applicationId = data.readString();
                String sessionId = data.readString();
                boolean relaunched = (data.readInt() != 0);
                onApplicationConnected(metadata, applicationId,
                        sessionId, relaunched);
                return true;
            case 3:
                data.enforceInterface("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
                postApplicationConnectionResult(data.readInt());
                return true;
            case 4:
                data.enforceInterface("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
                String status = data.readString();
                double volume = data.readDouble();
                boolean muted = (data.readInt() != 0);
                notifyApplicationStatusOrVolumeChanged(status, volume, muted);
                return true;
            case 5:
                data.enforceInterface("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
                onMessageReceived(data.readString(), data.readString());
                return true;
            case 6:
                data.enforceInterface("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
                onReceiveBinary(data.readString(), data.createByteArray());
                return true;
            case 7:
                data.enforceInterface("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
                onRequestResult(data.readInt());
                return true;
            case 8:
                data.enforceInterface("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
                onRequestStatus(data.readInt());
                return true;
            case 9:
                data.enforceInterface("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
                onApplicationDisconnected(data.readInt());
                return true;
            case 10:
                data.enforceInterface("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
                requestCallback(data.readString(), data.readLong(), data.readInt());
                return true;
            case 11:
                data.enforceInterface("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
                requestCallback(data.readString(), data.readLong());
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }
    }

}
