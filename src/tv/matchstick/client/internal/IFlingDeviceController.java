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

	/**
	 * An implementation of IFlingDeviceController interface
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

		public static IFlingDeviceController asInterface(IBinder binder) {
			if (binder == null) {
				return null;
			}
			IInterface local = binder
					.queryLocalInterface("tv.matchstick.fling.internal.IFlingDeviceController");
			if ((local != null) && (local instanceof IFlingDeviceController)) {
				return ((IFlingDeviceController) local);
			}
			return new Proxy(binder);
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
				sendMessage(data.readString(), data.readString(),
						data.readLong());
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
			private IBinder mRemote;

			Proxy(IBinder binder) {
				this.mRemote = binder;
			}

			@Override
			public IBinder asBinder() {
				return this.mRemote;
			}

			@Override
			public void disconnect() throws RemoteException {
				Parcel data = Parcel.obtain();
				try {
					data.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceController");
					this.mRemote.transact(1, data, null, 1);
				} finally {
					data.recycle();
				}
			}

			@Override
			public void launchApplication(String applicationId, boolean relaunch)
					throws RemoteException {
				Parcel data = Parcel.obtain();
				try {
					data.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceController");
					data.writeString(applicationId);
					data.writeInt((relaunch) ? 1 : 0);
					this.mRemote.transact(2, data, null, 1);
				} finally {
					data.recycle();
				}
			}

			@Override
			public void joinApplication(String applicationId, String sessionId)
					throws RemoteException {
				Parcel data = Parcel.obtain();
				try {
					data.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceController");
					data.writeString(applicationId);
					data.writeString(sessionId);
					this.mRemote.transact(3, data, null, 1);
				} finally {
					data.recycle();
				}
			}

			@Override
			public void leaveApplication() throws RemoteException {
				Parcel data = Parcel.obtain();
				try {
					data.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceController");
					this.mRemote.transact(4, data, null, 1);
				} finally {
					data.recycle();
				}
			}

			@Override
			public void stopApplication(String sessionId)
					throws RemoteException {
				Parcel data = Parcel.obtain();
				try {
					data.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceController");
					data.writeString(sessionId);
					this.mRemote.transact(5, data, null, 1);
				} finally {
					data.recycle();
				}
			}

			@Override
			public void requestStatus() throws RemoteException {
				Parcel data = Parcel.obtain();
				try {
					data.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceController");
					this.mRemote.transact(6, data, null, 1);
				} finally {
					data.recycle();
				}
			}

			@Override
			public void setVolume(double volume, double expected, boolean muted)
					throws RemoteException {
				Parcel data = Parcel.obtain();
				try {
					data.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceController");
					data.writeDouble(volume);
					data.writeDouble(expected);
					data.writeInt((muted) ? 1 : 0);
					this.mRemote.transact(7, data, null, 1);
				} finally {
					data.recycle();
				}
			}

			@Override
			public void setMute(boolean mute, double volume, boolean isMute)
					throws RemoteException {
				Parcel data = Parcel.obtain();
				try {
					data.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceController");
					data.writeInt((mute) ? 1 : 0);
					data.writeDouble(volume);
					data.writeInt((isMute) ? 1 : 0);
					this.mRemote.transact(8, data, null, 1);
				} finally {
					data.recycle();
				}
			}

			@Override
			public void sendMessage(String namespace, String message,
					long requestId) throws RemoteException {
				Parcel data = Parcel.obtain();
				try {
					data.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceController");
					data.writeString(namespace);
					data.writeString(message);
					data.writeLong(requestId);
					this.mRemote.transact(9, data, null, 1);
				} finally {
					data.recycle();
				}
			}

			@Override
			public void sendBinaryMessage(String namespace, byte[] message,
					long requestId) throws RemoteException {
				Parcel data = Parcel.obtain();
				try {
					data.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceController");
					data.writeString(namespace);
					data.writeByteArray(message);
					data.writeLong(requestId);
					this.mRemote.transact(10, data, null, 1);
				} finally {
					data.recycle();
				}
			}

			@Override
			public void setMessageReceivedCallbacks(String namespace)
					throws RemoteException {
				Parcel data = Parcel.obtain();
				try {
					data.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceController");
					data.writeString(namespace);
					this.mRemote.transact(11, data, null, 1);
				} finally {
					data.recycle();
				}
			}

			@Override
			public void removeMessageReceivedCallbacks(String namespace)
					throws RemoteException {
				Parcel data = Parcel.obtain();
				try {
					data.writeInterfaceToken("tv.matchstick.fling.internal.IFlingDeviceController");
					data.writeString(namespace);
					this.mRemote.transact(12, data, null, 1);
				} finally {
					data.recycle();
				}
			}
		}
	}

}
