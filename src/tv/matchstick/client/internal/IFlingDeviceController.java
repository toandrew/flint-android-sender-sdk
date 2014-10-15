package tv.matchstick.client.internal;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IFlingDeviceController extends IInterface {

    public void disconnect() throws RemoteException;

    public void launchApplication(String applicationId,
            boolean relaunchIfRunning) throws RemoteException;

    public void joinApplication(String applicationId, String sessionId)
            throws RemoteException;

    public void leaveApplication() throws RemoteException;

    public void stopApplication(String sessionId) throws RemoteException;

    public void requestStatus() throws RemoteException;

    public void setVolume(double volume, double originalVolume, boolean isMute)
            throws RemoteException;

    public void setMute(boolean mute, double volume, boolean isMute)
            throws RemoteException;

    public void sendMessage(String namespace, String message, long requestId)
            throws RemoteException;

    public void setMessageReceivedCallbacks(String namespace)
            throws RemoteException;

    public void removeMessageReceivedCallbacks(String namespace)
            throws RemoteException;

    /*************************************************/
    // unused
    /*************************************************/
    public void sendBinaryMessage(String namespace, byte[] message,
            long requestId) throws RemoteException;

    /*
     * An implementation of ds interface
     */
    public static abstract class Stub extends Binder implements
            IFlingDeviceController {
        public Stub() {
            attachInterface(this,
                    "tv.matchstick.fling.internal.IFlingDeviceController");
        }

        @Override
        public IBinder asBinder() {
            return this;
        }
        
        public static IFlingDeviceController asInterface(IBinder obj) {
            if (obj == null)
                return null;
            IInterface local = obj
                    .queryLocalInterface("tv.matchstick.fling.internal.IFlingDeviceController");
            if ((local != null) && (local instanceof IFlingDeviceController)) {
                return ((IFlingDeviceController) local);
            }
            return new Proxy(obj);
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
            switch (code) {
            case IBinder.LAST_CALL_TRANSACTION:
                reply.writeString("tv.matchstick.fling.internal.IFlingDeviceController");
                return true;
            case 1:
                data.enforceInterface("tv.matchstick.fling.internal.IFlingDeviceController");
                disconnect();
                return true;
            case 2:
                data.enforceInterface("tv.matchstick.fling.internal.IFlingDeviceController");
                String applicationId = data.readString();
                boolean relaunchIfRunning = (data.readInt() != 0);
                launchApplication(applicationId, relaunchIfRunning);
                return true;
            case 3:
                data.enforceInterface("tv.matchstick.fling.internal.IFlingDeviceController");
                joinApplication(data.readString(), data.readString());
                return true;
            case 4:
                data.enforceInterface("tv.matchstick.fling.internal.IFlingDeviceController");
                leaveApplication();
                return true;
            case 5:
                data.enforceInterface("tv.matchstick.fling.internal.IFlingDeviceController");
                stopApplication(data.readString());
                return true;
            case 6:
                data.enforceInterface("tv.matchstick.fling.internal.IFlingDeviceController");
                requestStatus();
                return true;
            case 7:
                data.enforceInterface("tv.matchstick.fling.internal.IFlingDeviceController");
                double volume = data.readDouble();
                double originalVolume = data.readDouble();
                boolean isMute = (data.readInt() != 0);
                setVolume(volume, originalVolume, isMute);
                return true;
            case 8:
                data.enforceInterface("tv.matchstick.fling.internal.IFlingDeviceController");
                boolean mute = (data.readInt() != 0);
                double currentVolume = data.readDouble();
                boolean isMuted = (data.readInt() != 0);
                setMute(mute, currentVolume, isMuted);
                return true;
            case 9:
                data.enforceInterface("tv.matchstick.fling.internal.IFlingDeviceController");
                sendMessage(data.readString(), data.readString(), data.readLong());
                return true;
            case 10:
                data.enforceInterface("tv.matchstick.fling.internal.IFlingDeviceController");
                sendBinaryMessage(data.readString(), data.createByteArray(),
                        data.readLong());
                return true;
            case 11:
                data.enforceInterface("tv.matchstick.fling.internal.IFlingDeviceController");
                setMessageReceivedCallbacks(data.readString());
                return true;
            case 12:
                data.enforceInterface("tv.matchstick.fling.internal.IFlingDeviceController");
                removeMessageReceivedCallbacks(data.readString());
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IFlingDeviceController {
            private IBinder mRemoted;

            Proxy(IBinder binder) {
                this.mRemoted = binder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemoted;
            }

            @Override
            public void disconnect() throws RemoteException {
                Parcel localParcel = Parcel.obtain();
                try {
                    localParcel
                            .writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceController");
                    this.mRemoted.transact(1, localParcel, null, 1);
                } finally {
                    localParcel.recycle();
                }
            }

            @Override
            public void launchApplication(String paramString,
                    boolean paramBoolean) throws RemoteException {
                Parcel localParcel = Parcel.obtain();
                try {
                    localParcel
                            .writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceController");
                    localParcel.writeString(paramString);
                    localParcel.writeInt((paramBoolean) ? 1 : 0);
                    this.mRemoted.transact(2, localParcel, null, 1);
                } finally {
                    localParcel.recycle();
                }
            }

            @Override
            public void joinApplication(String paramString1, String paramString2)
                    throws RemoteException {
                Parcel localParcel = Parcel.obtain();
                try {
                    localParcel
                            .writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceController");
                    localParcel.writeString(paramString1);
                    localParcel.writeString(paramString2);
                    this.mRemoted.transact(3, localParcel, null, 1);
                } finally {
                    localParcel.recycle();
                }
            }

            @Override
            public void leaveApplication() throws RemoteException {
                Parcel localParcel = Parcel.obtain();
                try {
                    localParcel
                            .writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceController");
                    this.mRemoted.transact(4, localParcel, null, 1);
                } finally {
                    localParcel.recycle();
                }
            }

            @Override
            public void stopApplication(String paramString)
                    throws RemoteException {
                Parcel localParcel = Parcel.obtain();
                try {
                    localParcel
                            .writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceController");
                    localParcel.writeString(paramString);
                    this.mRemoted.transact(5, localParcel, null, 1);
                } finally {
                    localParcel.recycle();
                }
            }

            @Override
            public void requestStatus() throws RemoteException {
                Parcel localParcel = Parcel.obtain();
                try {
                    localParcel
                            .writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceController");
                    this.mRemoted.transact(6, localParcel, null, 1);
                } finally {
                    localParcel.recycle();
                }
            }

            @Override
            public void setVolume(double paramDouble1, double paramDouble2,
                    boolean paramBoolean) throws RemoteException {
                Parcel localParcel = Parcel.obtain();
                try {
                    localParcel
                            .writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceController");
                    localParcel.writeDouble(paramDouble1);
                    localParcel.writeDouble(paramDouble2);
                    localParcel.writeInt((paramBoolean) ? 1 : 0);
                    this.mRemoted.transact(7, localParcel, null, 1);
                } finally {
                    localParcel.recycle();
                }
            }

            @Override
            public void setMute(boolean paramBoolean1, double paramDouble,
                    boolean paramBoolean2) throws RemoteException {
                Parcel localParcel = Parcel.obtain();
                try {
                    localParcel
                            .writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceController");
                    localParcel.writeInt((paramBoolean1) ? 1 : 0);
                    localParcel.writeDouble(paramDouble);
                    localParcel.writeInt((paramBoolean2) ? 1 : 0);
                    this.mRemoted.transact(8, localParcel, null, 1);
                } finally {
                    localParcel.recycle();
                }
            }

            @Override
            public void sendMessage(String paramString1, String paramString2,
                    long paramLong) throws RemoteException {
                Parcel localParcel = Parcel.obtain();
                try {
                    localParcel
                            .writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceController");
                    localParcel.writeString(paramString1);
                    localParcel.writeString(paramString2);
                    localParcel.writeLong(paramLong);
                    this.mRemoted.transact(9, localParcel, null, 1);
                } finally {
                    localParcel.recycle();
                }
            }

            @Override
            public void sendBinaryMessage(String namespace, byte[] message,
                    long requestId) throws RemoteException {
                Parcel localParcel = Parcel.obtain();
                try {
                    localParcel
                            .writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceController");
                    localParcel.writeString(namespace);
                    localParcel.writeByteArray(message);
                    localParcel.writeLong(requestId);
                    this.mRemoted.transact(10, localParcel, null, 1);
                } finally {
                    localParcel.recycle();
                }
            }

            @Override
            public void setMessageReceivedCallbacks(String namespace)
                    throws RemoteException {
                Parcel localParcel = Parcel.obtain();
                try {
                    localParcel
                            .writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceController");
                    localParcel.writeString(namespace);
                    this.mRemoted.transact(11, localParcel, null, 1);
                } finally {
                    localParcel.recycle();
                }
            }

            @Override
            public void removeMessageReceivedCallbacks(String paramString)
                    throws RemoteException {
                Parcel localParcel = Parcel.obtain();
                try {
                    localParcel
                            .writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceController");
                    localParcel.writeString(paramString);
                    this.mRemoted.transact(12, localParcel, null, 1);
                } finally {
                    localParcel.recycle();
                }
            }
        }
    }

}
