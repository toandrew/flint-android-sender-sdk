package tv.matchstick.client.internal;

import tv.matchstick.client.internal.IFlingCallbacks;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * a bridge to Fling Service
 */
public interface IFlingServiceBroker extends IInterface {
    public void initFlingService(IFlingCallbacks flingCallbacks, int requestVersion,
            String packageName, IBinder binder, Bundle bundle)
            throws RemoteException;

    public static abstract class Stub extends Binder implements
            IFlingServiceBroker {
        public Stub() {
            attachInterface(this,
                    "tv.matchstick.common.internal.IFlingServiceBroker");
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        public static IFlingServiceBroker asInterface(IBinder binder) {
            if (binder == null)
                return null;
            IInterface flingServiceBroker = binder
                    .queryLocalInterface("tv.matchstick.common.internal.IFlingServiceBroker");
            if ((flingServiceBroker != null)
                    && (flingServiceBroker instanceof IFlingServiceBroker))
                return ((IFlingServiceBroker) flingServiceBroker);
            return new Proxy(binder);
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
            switch (code) {
            case IBinder.LAST_CALL_TRANSACTION:
                reply.writeString("tv.matchstick.common.internal.IFlingServiceBroker");
                return true;

            case 19:
                data.enforceInterface("tv.matchstick.common.internal.IFlingServiceBroker");
                IFlingCallbacks callbacks = IFlingCallbacks.Stub
                        .asInterface(data.readStrongBinder());
                int version = data.readInt();
                String pakcageName = data.readString();
                IBinder flingDeviceControllerListener = data.readStrongBinder();
                Bundle bundle;
                if (data.readInt() != 0)
                    bundle = (Bundle) Bundle.CREATOR.createFromParcel(data);
                else
                    bundle = null;
                initFlingService(callbacks, version, pakcageName,
                        flingDeviceControllerListener, bundle);
                reply.writeNoException();
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IFlingServiceBroker {
            private IBinder mRemote;

            Proxy(IBinder binder) {
                mRemote = binder;
            }

            @Override
            public IBinder asBinder() {
                return mRemote;
            }

            public void initFlingService(IFlingCallbacks flingCallbacks,
                    int version, String pakcageName, IBinder binder,
                    Bundle bundle) throws RemoteException {
                Parcel parcel = Parcel.obtain();
                Parcel parcel1 = Parcel.obtain();
                try {
                    parcel.writeInterfaceToken("tv.matchstick.common.internal.IFlingServiceBroker");
                    parcel.writeStrongBinder((flingCallbacks != null) ? flingCallbacks
                            .asBinder() : null);
                    parcel.writeInt(version);
                    parcel.writeString(pakcageName);
                    parcel.writeStrongBinder(binder);
                    if (bundle != null) {
                        parcel.writeInt(1);
                        bundle.writeToParcel(parcel, 0);
                    } else {
                        parcel.writeInt(0);
                    }
                    mRemote.transact(19, parcel, parcel1, 0);
                    parcel1.readException();
                } finally {
                    parcel1.recycle();
                    parcel.recycle();
                }
            }
        }
    }

}
