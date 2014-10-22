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

		public static IFlingDeviceControllerListener asInterface(IBinder binder) {
			if (binder == null) {
				return null;
			}
			IInterface local = binder
					.queryLocalInterface("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
			if ((local != null)
					&& (local instanceof IFlingDeviceControllerListener)) {
				return ((IFlingDeviceControllerListener) local);
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
			private IBinder mRemote;

			Proxy(IBinder ibinder) {
				mRemote = ibinder;
			}

			@Override
			public final void onDisconnected(int cause) throws RemoteException {
				Parcel data = Parcel.obtain();
				try {
					data.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
					data.writeInt(cause);
					mRemote.transact(1, data, null, 1);
				} finally {
					data.recycle();
				}
			}

			@Override
			public final void onApplicationConnected(
					ApplicationMetadata applicationmetadata,
					String applicationId, String sessionId, boolean relaunched)
					throws RemoteException {
				Parcel data = Parcel.obtain();
				try {
					data.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
					if (applicationmetadata == null) {
						data.writeInt(0);
					} else {
						data.writeInt(1);
						applicationmetadata.writeToParcel(data, 0);
					}

					data.writeString(applicationId);
					data.writeString(sessionId);
					if (!relaunched) {
						data.writeInt(0);
					} else {
						data.writeInt(1);
					}

					mRemote.transact(2, data, null, 1);
				} finally {
					data.recycle();
				}
				return;
			}

			@Override
			public final void notifyApplicationStatusOrVolumeChanged(
					String status, double volume, boolean muteState)
					throws RemoteException {
				Parcel data = Parcel.obtain();
				try {
					data.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
					data.writeString(status);
					data.writeDouble(volume);
					if (!muteState) {
						data.writeInt(0);
					} else {
						data.writeInt(1);
					}
					mRemote.transact(4, data, null, 1);
				} finally {
					data.recycle();
				}
			}

			@Override
			public final void requestCallback(String namespace, long requestId)
					throws RemoteException {
				Parcel data = Parcel.obtain();
				try {
					data.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
					data.writeString(namespace);
					data.writeLong(requestId);
					mRemote.transact(11, data, null, 1);
				} finally {
					data.recycle();
				}
				return;
			}

			public final void requestCallback(String namespace, long requestId,
					int result) throws RemoteException {
				Parcel data = Parcel.obtain();
				try {
					data.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
					data.writeString(namespace);
					data.writeLong(requestId);
					data.writeInt(result);
					mRemote.transact(10, data, null, 1);
				} finally {
					data.recycle();
				}
			}

			@Override
			public final void onMessageReceived(String namespace, String message)
					throws RemoteException {
				Parcel data = Parcel.obtain();
				try {
					data.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
					data.writeString(namespace);
					data.writeString(message);
					mRemote.transact(5, data, null, 1);
				} finally {
					data.recycle();
				}
			}

			@Override
			public final void onReceiveBinary(String namespace, byte message[])
					throws RemoteException {
				Parcel data = Parcel.obtain();
				try {
					data.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
					data.writeString(namespace);
					data.writeByteArray(message);
					mRemote.transact(6, data, null, 1);
				} finally {
					data.recycle();
				}
				return;
			}

			@Override
			public final IBinder asBinder() {
				return mRemote;
			}

			@Override
			public final void postApplicationConnectionResult(int status)
					throws RemoteException {
				Parcel data = Parcel.obtain();
				try {
					data.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
					data.writeInt(status);
					mRemote.transact(3, data, null, 1);
				} finally {
					data.recycle();
				}
				return;
			}

			@Override
			public final void onRequestResult(int result)
					throws RemoteException {
				Parcel data = Parcel.obtain();
				try {
					data.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
					data.writeInt(result);
					mRemote.transact(7, data, null, 1);
				} finally {
					data.recycle();
				}
			}

			public final void onRequestStatus(int result)
					throws RemoteException {
				Parcel data = Parcel.obtain();
				try {
					data.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
					data.writeInt(result);
					mRemote.transact(8, data, null, 1);
				} finally {
					data.recycle();
				}
				return;
			}

			@Override
			public final void onApplicationDisconnected(int status)
					throws RemoteException {
				Parcel data = Parcel.obtain();
				try {
					data.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceControllerListener");
					data.writeInt(status);
					mRemote.transact(9, data, null, 1);
				} finally {
					data.recycle();
				}
				return;
			}
		}
	}
}
