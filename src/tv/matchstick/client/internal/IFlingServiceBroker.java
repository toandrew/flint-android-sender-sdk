/*
 * Copyright (C) 2013-2014, Infthink (Beijing) Technology Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS-IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package tv.matchstick.client.internal;

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
	public void initFlingService(IFlingCallbacks callbacks, int version,
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
			if (binder == null) {
				return null;
			}

			IInterface broker = binder
					.queryLocalInterface("tv.matchstick.common.internal.IFlingServiceBroker");
			if ((broker != null) && (broker instanceof IFlingServiceBroker)) {
				return ((IFlingServiceBroker) broker);
			}

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
				Bundle bundle = null;

				if (data.readInt() != 0) {
					bundle = (Bundle) Bundle.CREATOR.createFromParcel(data);
				}

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

			@Override
			public void initFlingService(IFlingCallbacks callbacks,
					int version, String pakcageName, IBinder binder,
					Bundle bundle) throws RemoteException {
				Parcel data = Parcel.obtain();
				Parcel reply = Parcel.obtain();
				try {
					data.writeInterfaceToken("tv.matchstick.common.internal.IFlingServiceBroker");
					data.writeStrongBinder((callbacks != null) ? callbacks
							.asBinder() : null);
					data.writeInt(version);
					data.writeString(pakcageName);
					data.writeStrongBinder(binder);
					if (bundle != null) {
						data.writeInt(1);
						bundle.writeToParcel(data, 0);
					} else {
						data.writeInt(0);
					}
					mRemote.transact(19, data, reply, 0);
					reply.readException();
				} finally {
					reply.recycle();
					data.recycle();
				}
			}
		}
	}

}
