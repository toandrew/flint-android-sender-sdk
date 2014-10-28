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

package tv.matchstick.fling.service;

import java.util.ArrayList;
import java.util.List;

import tv.matchstick.client.internal.IFlingCallbacks;
import tv.matchstick.client.internal.IFlingDeviceControllerListener;
import tv.matchstick.client.internal.IFlingServiceBroker;
import tv.matchstick.client.internal.LOG;
import tv.matchstick.fling.FlingDevice;
import tv.matchstick.server.fling.bridge.FlingConnectedClient;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;

/**
 * This service used to control all fling media actions
 * 
 * @author jim
 *
 */
public class FlingService extends Service {
	private static final LOG log = new LOG("FlingService");

	private static final String DEVICE_CONTROL_ACTION = "tv.matchstick.fling.service.FLING";

	private Handler mHandler;

	/**
	 * Contained all connect fling client.
	 */
	private List<FlingConnectedClient> mFlingClients;

	public static LOG log() {
		return log;
	}

	private synchronized void removeFlingClient(FlingConnectedClient client) {
		mFlingClients.remove(client);
	}

	/**
	 * Remove client from connected fling client list
	 * 
	 * @param client
	 *            client which will be removed
	 */
	public static void removeFlingClient(FlingService flingservice,
			FlingConnectedClient client) {
		flingservice.removeFlingClient(client);
	}

	/**
	 * Get Client main Handler
	 * 
	 * @param flingservice
	 * @return service handler
	 */
	public static Handler getHandler(FlingService flingservice) {
		return flingservice.mHandler;
	}

	@Override
	public IBinder onBind(Intent intent) {
		if (!DEVICE_CONTROL_ACTION.equals(intent.getAction())) {
			return null;
		}

		log.d("onBind!!!!");

		return mBinder.asBinder();
	}

	@Override
	public void onCreate() {
		mFlingClients = new ArrayList<FlingConnectedClient>();
		mHandler = new Handler(Looper.getMainLooper());
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return Service.START_STICKY;
	}

	private final IFlingServiceBroker.Stub mBinder = new IFlingServiceBroker.Stub() {

		@Override
		public void init(IFlingCallbacks callbacks, int version,
				String packageName, IBinder binder, Bundle bundle)
				throws RemoteException {
			// TODO Auto-generated method stub

			log.d("begin init fling service binder!");

			try {
				FlingDevice flingdevice = FlingDevice.getFromBundle(bundle);
				String lastApplicationId = bundle
						.getString("last_application_id");
				String lastSessionId = bundle.getString("last_session_id");
				long flags = bundle.getLong(
						"tv.matchstick.fling.EXTRA_FLING_FLAGS", 0L);

				log.d("connecting to device with lastApplicationId=%s, lastSessionId=%s",
						lastApplicationId, lastSessionId);

				IFlingDeviceControllerListener listener = IFlingDeviceControllerListener.Stub
						.asInterface(binder);

				/**
				 * Add one fling client to fling service's client list
				 */
				mFlingClients.add(new FlingConnectedClient(FlingService.this,
						callbacks, flingdevice, lastApplicationId,
						lastSessionId, listener, packageName, flags));

				log.d("end init fling service binder!");
			} catch (Exception e) {
				log.e(e, "Failed to init fling service binder!");

				try {
					callbacks.onPostInitComplete(10, null, null);
				} catch (RemoteException re) {
					log.d("client died while brokering service");
				}
			}
		}

	};
}
