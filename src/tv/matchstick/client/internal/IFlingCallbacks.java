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

public interface IFlingCallbacks extends IInterface {

	public abstract void onPostInitComplete(int statusCode, IBinder binder,
			Bundle bundle) throws RemoteException;

	public static abstract class Stub extends Binder implements IFlingCallbacks {

		public Stub() {
			attachInterface(this,
					"tv.matchstick.common.internal.IFlingCallbacks");
		}

		public static IFlingCallbacks asInterface(IBinder binder) {
			if (binder == null)
				return null;
			IInterface callbacks = binder
					.queryLocalInterface("tv.matchstick.common.internal.IFlingCallbacks");
			if ((callbacks != null) && (callbacks instanceof IFlingCallbacks)) {
				return ((IFlingCallbacks) callbacks);
			}

			return new Proxy(binder);
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
				reply.writeString("tv.matchstick.common.internal.IFlingCallbacks");
				return true;
			case 1:
				data.enforceInterface("tv.matchstick.common.internal.IFlingCallbacks");
				int statusCode = data.readInt();
				IBinder binder = data.readStrongBinder();
				Bundle bundle;
				if (data.readInt() != 0) {
					bundle = (Bundle) Bundle.CREATOR.createFromParcel(data);
				} else {
					bundle = null;
				}

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

			@Override
			public IBinder asBinder() {
				return mRemote;
			}

			public void onPostInitComplete(int statusCode, IBinder binder,
					Bundle bundle) throws RemoteException {
				Parcel data = Parcel.obtain();
				Parcel replay = Parcel.obtain();
				try {
					data.writeInterfaceToken("tv.matchstick.common.internal.IFlingCallbacks");
					data.writeInt(statusCode);
					data.writeStrongBinder(binder);
					if (bundle != null) {
						data.writeInt(1);
						bundle.writeToParcel(data, 0);
					} else {
						data.writeInt(0);
					}
					mRemote.transact(1, data, replay, 0);
					replay.readException();
				} finally {
					replay.recycle();
					data.recycle();
				}
			}
		}
	}

}
