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

package tv.matchstick.server.fling.bridge;

import tv.matchstick.client.internal.IFlingCallbacks;
import tv.matchstick.client.internal.IFlingDeviceControllerListener;
import tv.matchstick.client.internal.IFlingServiceBroker;
import tv.matchstick.fling.FlingDevice;
import tv.matchstick.fling.service.FlingService;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * Fling Service's binder
 */
public final class FlingServiceBinder extends IFlingServiceBroker.Stub {
	final FlingService mFlingService;

	private FlingServiceBinder(FlingService flingservice) {
		super();
		mFlingService = flingservice;
	}

	public FlingServiceBinder(FlingService flingservice, byte dummy) {
		this(flingservice);
	}

	/**
	 * Create one fling client
	 * 
	 * @callbacks callback function
	 * @version fling client's versions
	 * @packageName package name
	 * @listener fling device control listener
	 * @bundle data
	 */
	@Override
	public final void init(IFlingCallbacks callbacks, int version,
			String packageName, IBinder listener, Bundle bundle) {

		FlingService.log().d("begin initFlingService!");
		try {
			FlingDevice flingdevice = FlingDevice.getFromBundle(bundle);
			String lastApplicationId = bundle.getString("last_application_id");
			String lastSessionId = bundle.getString("last_session_id");
			long flags = bundle.getLong(
					"tv.matchstick.fling.EXTRA_FLING_FLAGS", 0L);
			FlingService
					.log()
					.d("connecting to device with lastApplicationId=%s, lastSessionId=%s",
							lastApplicationId, lastSessionId);

			IFlingDeviceControllerListener controlListener = IFlingDeviceControllerListener.Stub
					.asInterface(listener);

			/**
			 * Add one fling client to fling service's client list
			 */
			FlingService.getFlingClients(mFlingService).add(
					new FlingConnectedClient(mFlingService, callbacks,
							flingdevice, lastApplicationId, lastSessionId,
							controlListener, packageName, flags));

			FlingService.log().d("end initFlingService!");
		} catch (Exception e) {
			FlingService.log().e(e.toString(), "Fling device was not valid.");
			try {
				callbacks.onPostInitComplete(10, null, null);
			} catch (RemoteException ez) {
				FlingService.log().d("client died while brokering service");
			}
		}
	}
}
