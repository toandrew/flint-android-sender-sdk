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

    public abstract void onReceiveBinary(String namespace, byte[] binary)
            throws RemoteException;

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

        public static IFlingDeviceControllerListener asInterface(IBinder obj) {
            if (obj == null)
                return null;
            IInterface local = obj
                    .queryLocalInterface("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
            if ((local != null)
                    && (local instanceof IFlingDeviceControllerListener)) {
                return ((IFlingDeviceControllerListener) local);
            }
            return new Proxy(obj);
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
                onApplicationConnected(metadata, applicationId, sessionId,
                        relaunched);
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
                requestCallback(data.readString(), data.readLong(),
                        data.readInt());
                return true;
            case 11:
                data.enforceInterface("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
                requestCallback(data.readString(), data.readLong());
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IFlingDeviceControllerListener {
            private IBinder mRemoted;

            Proxy(IBinder ibinder) {
                mRemoted = ibinder;
            }

            @Override
            public final void onDisconnected(int i) throws RemoteException {
                Parcel parcel = Parcel.obtain();
                try {
                    parcel.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
                    parcel.writeInt(i);
                    mRemoted.transact(1, parcel, null, 1);
                } finally {
                    parcel.recycle();
                }
            }

            @Override
            public final void onApplicationConnected(
                    ApplicationMetadata applicationmetadata,
                    String applicationId, String sessionId, boolean relaunched)
                    throws RemoteException {
                int i;
                Parcel parcel;
                i = 1;
                parcel = Parcel.obtain();
                try {
                    parcel.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
                    if (applicationmetadata == null) {
                        parcel.writeInt(0);
                    } else {
                        parcel.writeInt(1);
                        applicationmetadata.writeToParcel(parcel, 0);
                    }

                    parcel.writeString(applicationId);
                    parcel.writeString(sessionId);
                    if (!relaunched)
                        i = 0;
                    parcel.writeInt(i);
                    mRemoted.transact(2, parcel, null, 1);
                } finally {
                    parcel.recycle();
                }
                return;
            }

            @Override
            public final void notifyApplicationStatusOrVolumeChanged(
                    String status, double volume, boolean muteState)
                    throws RemoteException {
                int i;
                Parcel parcel;
                i = 1;
                parcel = Parcel.obtain();
                try {
                    parcel.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
                    parcel.writeString(status);
                    parcel.writeDouble(volume);
                    if (!muteState)
                        i = 0;
                    parcel.writeInt(i);
                    mRemoted.transact(4, parcel, null, 1);
                } finally {
                    parcel.recycle();
                }
            }

            @Override
            public final void requestCallback(String namespace, long requestId)
                    throws RemoteException {
                Parcel parcel = Parcel.obtain();
                try {
                    parcel.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
                    parcel.writeString(namespace);
                    parcel.writeLong(requestId);
                    mRemoted.transact(11, parcel, null, 1);
                } finally {
                    parcel.recycle();
                }
                return;
            }

            public final void requestCallback(String namespace, long requestId,
                    int result) throws RemoteException {
                Parcel parcel = Parcel.obtain();
                try {
                    parcel.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
                    parcel.writeString(namespace);
                    parcel.writeLong(requestId);
                    parcel.writeInt(result);
                    mRemoted.transact(10, parcel, null, 1);
                } finally {
                    parcel.recycle();
                }
            }

            @Override
            public final void onMessageReceived(String namespace, String message)
                    throws RemoteException {
                Parcel parcel = Parcel.obtain();
                try {
                    parcel.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
                    parcel.writeString(namespace);
                    parcel.writeString(message);
                    mRemoted.transact(5, parcel, null, 1);
                } finally {
                    parcel.recycle();
                }
            }

            @Override
            public final void onReceiveBinary(String s, byte abyte0[])
                    throws RemoteException {
                Parcel parcel = Parcel.obtain();
                try {
                    parcel.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
                    parcel.writeString(s);
                    parcel.writeByteArray(abyte0);
                    mRemoted.transact(6, parcel, null, 1);
                } finally {
                    parcel.recycle();
                }
                return;
            }

            @Override
            public final IBinder asBinder() {
                return mRemoted;
            }

            @Override
            public final void postApplicationConnectionResult(int i)
                    throws RemoteException {
                Parcel parcel = Parcel.obtain();
                try {
                    parcel.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
                    parcel.writeInt(i);
                    mRemoted.transact(3, parcel, null, 1);
                } finally {
                    parcel.recycle();
                }
                return;
            }

            @Override
            public final void onRequestResult(int i) throws RemoteException {
                Parcel parcel = Parcel.obtain();
                try {
                    parcel.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
                    parcel.writeInt(i);
                    mRemoted.transact(7, parcel, null, 1);
                } finally {
                    parcel.recycle();
                }
            }

            public final void onRequestStatus(int result)
                    throws RemoteException {
                Parcel parcel = Parcel.obtain();
                try {
                    parcel.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
                    parcel.writeInt(result);
                    mRemoted.transact(8, parcel, null, 1);
                } finally {
                    parcel.recycle();
                }
                return;
            }

            @Override
            public final void onApplicationDisconnected(int i)
                    throws RemoteException {
                Parcel parcel = Parcel.obtain();
                try {
                    parcel.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
                    parcel.writeInt(i);
                    mRemoted.transact(9, parcel, null, 1);
                } finally {
                    parcel.recycle();
                }
                return;
            }
        }
    }
}
