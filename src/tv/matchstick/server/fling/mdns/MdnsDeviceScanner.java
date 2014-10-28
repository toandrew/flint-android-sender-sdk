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

package tv.matchstick.server.fling.mdns;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import tv.matchstick.fling.FlingDevice;
import tv.matchstick.fling.images.WebImage;
import tv.matchstick.server.fling.mdns.FlingDeviceInfoContainer.FlingDeviceInfo;
import tv.matchstick.server.utils.LogUtil;
import android.content.Context;
import android.net.Uri;
import android.os.SystemClock;

public final class MdnsDeviceScanner extends DeviceScanner {
	private static final LogUtil log = new LogUtil("MdnsDeviceScanner");

	private final List mFlingMdnsClientList = new ArrayList();

	private final Map e = new HashMap();
	private final String mName;
	private Thread mScannerLoopThread;
	private boolean h;

	public MdnsDeviceScanner(Context paramContext) {
		super(paramContext);
		this.mName = "Fling Device";
	}

	void scanLoop() {
		while (!h) {
			try {
				Thread.sleep(3000L);
			} catch (InterruptedException e) {
				if (h) {
					break;
				}
			}
			synchronized (e) {
				long l = SystemClock.elapsedRealtime();
				Iterator localIterator = e.entrySet().iterator();
				while (localIterator.hasNext()) {
					ScannerPrivData localavi = (ScannerPrivData) ((Map.Entry) localIterator
							.next()).getValue();
					int i = 0;
					if (l - localavi.mElapsedRealtime < 60000L) {
						i = 0;
					} else {
						i = 1;
					}
					if (i == 0)
						continue;
					final FlingDevice device = localavi.mFlingDevice;

					mHandler.post(new Runnable() {

						@Override
						public void run() {
							notifyDeviceOffline(device);
						}

					});
					log.d("expired record for %s", device);

					localIterator.remove();
				}
			}
		}

		log.d("refreshLoop exiting");
		return;
	}

	void onResults(FlingDeviceInfo info) {
		if (log.isDebugEnabled()) {
			log.d("FQDN: %s", info.mFQDN);
			List localList4 = info.mIpV4AddrList;
			if (localList4 != null) {
				Iterator localIterator4 = localList4.iterator();
				while (localIterator4.hasNext()) {
					Inet4Address localInet4Address2 = (Inet4Address) localIterator4
							.next();
					log.d("IPv4 address: %s", localInet4Address2);
				}
			}
			List localList5 = info.mIpV6AddrList;
			if (localList5 != null) {
				Iterator localIterator3 = localList5.iterator();
				while (localIterator3.hasNext()) {
					Inet6Address localInet6Address = (Inet6Address) localIterator3
							.next();
					log.d("IPv6 address: %s", localInet6Address);
				}
			}
			log.d("service name: %s", info.mName);

			log.d("service host: %s", info.mHost);

			log.d("service proto: %d", info.mProto);

			log.d("service port: %d", info.mPort);

			log.d("service priority: %d", info.mPriority);

			log.d("service weight: %d", info.mWeight);

			List localList6 = info.mTextStringList;
			if (localList6 != null) {
				Iterator localIterator2 = localList6.iterator();
				while (localIterator2.hasNext()) {
					String str6 = (String) localIterator2.next();
					log.d("text string: %s", str6);
				}
			}

			log.d("TTL: %d", info.mTTL);
		}
		List localList1 = info.mTextStringList;
		if (localList1 != null) {
			Iterator localIterator1 = localList1.iterator();
			Object localObject1 = null;
			Object localObject2 = null;
			String str1 = null;
			Object deviceId = null;
			Object localObject5;
			while (localIterator1.hasNext()) {
				String str4 = (String) localIterator1.next();
				int k = str4.indexOf('=');
				if (k > 0) {
					String str5 = str4.substring(0, k);
					localObject5 = str4.substring(k + 1);
					if ("id".equalsIgnoreCase(str5))
						deviceId = localObject5;
					else if ("md".equalsIgnoreCase(str5))
						str1 = ((String) localObject5).replaceAll(
								"(Eureka|Chromekey)( Dongle)?", "Dongle");
					else if ("ve".equalsIgnoreCase(str5))
						localObject2 = localObject5;
					else if (!"ic".equalsIgnoreCase(str5)) {
						// break label1011;
						localObject5 = localObject1;
					} else {
						localObject1 = localObject5;
					}
				}
			}
			if (deviceId == null)
				return;
			if (str1 == null)
				str1 = mName;
			final FlingDevice device;
			ScannerPrivData localavi;
			synchronized (e) {
				List localList2 = info.mIpV4AddrList;
				if ((localList2 == null) || (localList2.isEmpty())) {
					e.remove(deviceId);
					return;
				}
				Inet4Address localInet4Address1 = (Inet4Address) localList2
						.get(0);
				ArrayList iconList = new ArrayList();
				if (localObject1 != null) {
					String str2 = localInet4Address1.toString();
					int i = str2.indexOf('/');
					if (i >= 0)
						str2 = str2.substring(i + 1);
					iconList.add(new WebImage(Uri.parse(String.format(
							"http://%s:8008%s", new Object[] { str2,
									localObject1 }))));
				}

				deviceId = deviceId + localInet4Address1.getHostAddress();

				FlingDeviceHelper localatr = FlingDevice.createHelper(
						(String) deviceId, localInet4Address1);
				String str3 = info.e;
				FlingDevice.setFriendlyName(localatr.mFlingDevice, str3);
				FlingDevice.setModelName(localatr.mFlingDevice, str1);
				FlingDevice.setDeviceVersion(localatr.mFlingDevice,
						(String) localObject2);
				int port = info.mPort;
				FlingDevice.setServicePort(localatr.mFlingDevice, port);
				FlingDevice.setIconList(localatr.mFlingDevice, iconList);
				device = localatr.mFlingDevice;
				localavi = (ScannerPrivData) e.get(deviceId);
				if (localavi != null) {
					if (device.equals(localavi.mFlingDevice)) {
						if (!localavi.d) {
							localavi.mElapsedRealtime = SystemClock
									.elapsedRealtime();
						}
						return;
					} else {
						e.remove(deviceId);
					}
				}

				e.put(deviceId, new ScannerPrivData(device, info.mTTL));
			}
			// CastDevice localCastDevice2 = localavi.mCastDevice_a; // localavi
			// will null???? need check this!!!!!!!!!!!!!!!!!!!!!!!!!
			FlingDevice device2 = null;
			if (localavi != null) {
				device2 = localavi.mFlingDevice;
			}
			if (device2 != null)
				notifyDeviceOffline(device2);
			if (device == null)
				return;

			DeviceScanner.log.d("notifyDeviceOnline: %s", device);

			final List listenerList = getDeviceScannerListenerList();
			if (listenerList == null)
				return;

			mHandler.post(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					Iterator localIterator = listenerList.iterator();
					while (localIterator.hasNext())
						((IDeviceScanListener) localIterator.next())
								.onDeviceOnline(device);
				}
			});
		}
	}

	public final void setDeviceOffline(String paramString) {
		FlingDevice device = null;
		synchronized (this.e) {
			ScannerPrivData localavi = (ScannerPrivData) this.e
					.get(paramString);
			if (localavi != null) {
				localavi.mElapsedRealtime = SystemClock.elapsedRealtime();
				localavi.d = true;
				device = localavi.mFlingDevice;
				if (device != null)
					notifyDeviceOffline(device);
			}
		}
	}

	protected final void startScanInternal(List networkInterfaceList) {
		log.d("startScanInternal");
		if (networkInterfaceList.isEmpty()) {
			log.w("No network interfaces to scan on!", new Object[0]);
			return;
		}
		Iterator localIterator = networkInterfaceList.iterator();
		while (localIterator.hasNext()) {
			NetworkInterface networkInterface = (NetworkInterface) localIterator
					.next();

			MdnsClient flingMdnsClient = new MdnsClient(
					"_MatchStick._tcp.local.", networkInterface) {

				@Override
				protected void onScanResults(FlingDeviceInfo info) {
					onResults(info);
				}

			};

			try {
				flingMdnsClient.startScan();
				mFlingMdnsClientList.add(flingMdnsClient);
			} catch (Exception localIOException) { // todo
				log.w("Couldn't start MDNS client for %s", networkInterface);
			}
		}
		// this.g = new Thread(new C_avg(this));
		this.mScannerLoopThread = new Thread(new Runnable() {

			@Override
			public void run() {
				scanLoop();
			}

		});
		this.mScannerLoopThread.start();
	}

	protected final void stopScanInternal() {
		if (!this.mFlingMdnsClientList.isEmpty()) {
			Iterator localIterator = this.mFlingMdnsClientList.iterator();
			while (localIterator.hasNext())
				((MdnsClient) localIterator.next()).stopScan();
			this.mFlingMdnsClientList.clear();
		}
		this.h = true;

		if (this.mScannerLoopThread != null) {
			boolean needWait = true;
			while (needWait) {
				try {
					this.mScannerLoopThread.interrupt();
					this.mScannerLoopThread.join();
					needWait = false;
				} catch (InterruptedException e) {
					e.printStackTrace();
					needWait = true;
				}
			}
		}

		this.mScannerLoopThread = null;
	}

	public final void onAllDevicesOffline() {
		synchronized (this.e) {
			boolean bool = this.e.isEmpty();
			int i = 0;
			if (!bool) {
				this.e.clear();
				i = 1;
			}
			if (i != 0) {
				final List listeners = super.getDeviceScannerListenerList();
				if (listeners != null) {
					this.mHandler.post(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							Iterator localIterator = listeners.iterator();
							while (localIterator.hasNext())
								((IDeviceScanListener) localIterator.next())
										.onAllDevicesOffline();
						}

					});
				}
			}
			return;
		}
	}

	final class ScannerPrivData {
		FlingDevice mFlingDevice;
		long mElapsedRealtime;
		long mTTl;
		boolean d;

		ScannerPrivData(FlingDevice device, long ttl) {
			super();
			mFlingDevice = device;
			mTTl = ttl;
			mElapsedRealtime = SystemClock.elapsedRealtime();
		}
	}
}
