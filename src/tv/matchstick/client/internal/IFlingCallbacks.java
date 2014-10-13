package tv.matchstick.client.internal;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IFlingCallbacks extends IInterface {

    public abstract void onPostInitComplete(int statusCode, IBinder binder,
            Bundle bundle) throws RemoteException;

    public static abstract class Stub extends Binder implements
            IFlingCallbacks {
        public Stub() {
            attachInterface(this,
                    "tv.matchstick.common.internal.IFlingCallbacks");
        }

        public static IFlingCallbacks asInterface(IBinder binder) {
            if (binder == null)
                return null;
            IInterface callbacks = binder
                    .queryLocalInterface("tv.matchstick.common.internal.IFlingCallbacks");
            if ((callbacks != null)
                    && (callbacks instanceof IFlingCallbacks))
                return ((IFlingCallbacks) callbacks);
            return new Proxy(binder);
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
            switch (code) {
            case IBinder.LAST_CALL_TRANSACTION:
                reply.writeString("tv.matchstick.common.internal.IFlingCallbacks");
                return true;
            case 1:
                data.enforceInterface("tv.matchstick.common.internal.IFlingCallbacks");
                int statusCode = data.readInt();
                IBinder binder = data.readStrongBinder();
                Bundle bundle;
                if (data.readInt() != 0)
                    bundle = (Bundle) Bundle.CREATOR.createFromParcel(data);
                else
                    bundle = null;
                onPostInitComplete(statusCode, binder, bundle);
                reply.writeNoException();
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IFlingCallbacks {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                mRemote = iBinder;
            }

            public IBinder asBinder() {
                return mRemote;
            }

            public void onPostInitComplete(int statusCode, IBinder binder,
                    Bundle bundle) throws RemoteException {
                Parcel parcel1 = Parcel.obtain();
                Parcel parcel2 = Parcel.obtain();
                try {
                    parcel1
                            .writeInterfaceToken("tv.matchstick.common.internal.IFlingCallbacks");
                    parcel1.writeInt(statusCode);
                    parcel1.writeStrongBinder(binder);
                    if (bundle != null) {
                        parcel1.writeInt(1);
                        bundle.writeToParcel(parcel1, 0);
                    } else {
                        parcel1.writeInt(0);
                    }
                    mRemote.transact(1, parcel1, parcel2, 0);
                    parcel2.readException();
                } finally {
                    parcel2.recycle();
                    parcel1.recycle();
                }
            }
        }
    }

}
