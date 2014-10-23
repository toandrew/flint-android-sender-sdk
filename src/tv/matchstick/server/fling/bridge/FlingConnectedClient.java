package tv.matchstick.server.fling.bridge;

import tv.matchstick.client.internal.IFlingCallbacks;
import tv.matchstick.client.internal.IFlingDeviceControllerListener;
import tv.matchstick.client.internal.ValueChecker;
import tv.matchstick.fling.ApplicationMetadata;
import tv.matchstick.fling.FlingDevice;
import tv.matchstick.fling.FlingStatusCodes;
import tv.matchstick.fling.service.FlingService;
import tv.matchstick.server.fling.FlingDeviceController;
import android.os.IBinder.DeathRecipient;
import android.os.RemoteException;

/**
 * This class used to interact with app about current status(application,fling
 * device,etc)
 * 
 */
public final class FlingConnectedClient implements IFlingSrvController {
	final FlingService mFlingService;
	private FlingDeviceControllerStubImpl mStubImpl;
	private final IFlingDeviceControllerListener mFlingDeviceControllerListener;
	private final DeathRecipient mFlingCallbackDeathHandler;
	private final DeathRecipient mListenerDeathHandler;
	private final FlingDevice mFlingDevice;
	private String mLastAppId;
	private String mLastSessionId;
	private FlingDeviceController mFlingDeviceController;
	private final IFlingCallbacks mFlingCallbacks;
	private final String mPackageName;
	private final long mFlags;

	/**
	 * Fling Client which will be interacted with app side to do all media
	 * control works
	 * 
	 * @param flingservice
	 * @param callbacks
	 * @param flingdevice
	 * @param lastApplicationId
	 * @param lastSessionId
	 * @param listener
	 * @param packageName
	 * @param flags
	 */
	FlingConnectedClient(FlingService flingservice, IFlingCallbacks callbacks,
			FlingDevice flingdevice, String lastApplicationId,
			String lastSessionId, IFlingDeviceControllerListener listener,
			String packageName, long flags) {
		super();

		mFlingService = flingservice;

		mFlingCallbacks = (IFlingCallbacks) ValueChecker
				.checkNullPointer(callbacks);
		mFlingDevice = flingdevice;
		mLastAppId = lastApplicationId;
		mLastSessionId = lastSessionId;
		mFlingDeviceControllerListener = listener;
		mStubImpl = null;
		mPackageName = packageName;
		mFlags = flags;

		mFlingCallbackDeathHandler = new DeathRecipient() {

			@Override
			public void binderDied() {
				handleBinderDeath(FlingConnectedClient.this);
			}

		};

		mListenerDeathHandler = new DeathRecipient() {

			@Override
			public void binderDied() {
				handleBinderDeath(FlingConnectedClient.this);
			}

		};

		/**
		 * In case the device controller's listener(in app side) is dead
		 */
		try {
			mFlingDeviceControllerListener.asBinder().linkToDeath(
					mListenerDeathHandler, 0);
		} catch (RemoteException remoteexception) {
			FlingService.log().e("client disconnected before listener was set",
					new Object[0]);
			if (!mFlingDeviceController.isDisposed())
				mFlingDeviceController.releaseReference();
		}

		FlingService.log().d("acquireDeviceController by %s", mPackageName);

		FlingService.log().d("Create one fling device controller!");
		mFlingDeviceController = FlingDeviceController.create(mFlingService,
				FlingService.getHandler(mFlingService), mPackageName,
				mFlingDevice, mFlags, this);

		mStubImpl = new FlingDeviceControllerStubImpl(mFlingService,
				mFlingDeviceController);

		/**
		 * already connected?
		 */
		if (mFlingDeviceController.isConnected()) {
			try {
				mFlingCallbacks.onPostInitComplete(FlingStatusCodes.SUCCESS,
						mStubImpl.asBinder(), null);
			} catch (RemoteException remoteexception2) {
				FlingService.log().d("client died while brokering service");
			}
			return;
		}

		/**
		 * is not busy on connecting to device?
		 */
		if (!mFlingDeviceController.isConnecting()) {
			FlingService
					.log()
					.d("reconnecting to device with applicationId=%s, sessionId=%s",
							mLastAppId, mLastSessionId);
			if (mLastAppId != null)
				mFlingDeviceController.reconnectToDevice(mLastAppId,
						mLastSessionId);
			else
				mFlingDeviceController.connectDevice();
		}

		/**
		 * in case, the flingcallback binder in app side is dead.
		 */
		try {
			mFlingCallbacks.asBinder().linkToDeath(mFlingCallbackDeathHandler,
					0);
		} catch (RemoteException e) {
			FlingService.log().w("Unable to link listener reaper",
					new Object[0]);
		}
	}

	/**
	 * Do some cleanup work when the app is dead
	 * 
	 * @param client
	 */
	static void handleBinderDeath(FlingConnectedClient client) {
		if (client.mFlingDeviceController != null
				&& !client.mFlingDeviceController.isDisposed()) {
			FlingService.log().w(
					"calling releaseReference from handleBinderDeath()",
					new Object[0]);
			client.mFlingDeviceController.releaseReference();
			FlingService.log().d("Released controller.");
		}
		FlingService.log().d("Removing ConnectedClient.");

		// remove connected client in fling service
		FlingService.removeFlingClient(client.mFlingService, client);
	}

	/**
	 * Invoked when connected with fling device. The app side will be notified
	 * the current connected status(0)
	 * 
	 */
	@Override
	public final void onConnected() {
		try {
			mFlingCallbacks.onPostInitComplete(FlingStatusCodes.SUCCESS,
					mStubImpl.asBinder(), null);
			FlingService.log().d("Connected to device.");
		} catch (RemoteException remoteexception) {
			FlingService.log().w(remoteexception,
					"client died while brokering service");
		}
	}

	/**
	 * Invoked when disconnected with fling device. The app side will be
	 * notified the current disconnected status(0)
	 * 
	 * @status the disconnected reason
	 */
	@Override
	public final void onDisconnected(int status) {
		FlingService.log().d("onDisconnected: status=%d", status);
		try {
			mFlingDeviceControllerListener.onDisconnected(status);
		} catch (RemoteException remoteexception) {
			FlingService.log().d(remoteexception,
					"client died while brokering service", new Object[0]);
		}

		/**
		 * release resources in controller instance
		 */
		if (!mFlingDeviceController.isDisposed()) {
			FlingService
					.log()
					.w("calling releaseReference from ConnectedClient.onDisconnected",
							new Object[0]);
			mFlingDeviceController.releaseReference();
		}
	}

	/**
	 * Called when disconnected with application
	 */
	@Override
	public final void onApplicationConnected(
			ApplicationMetadata applicationmetadata, String applicationId,
			String sessionId, boolean relaunched) {
		mLastAppId = applicationmetadata.getApplicationId();
		mLastSessionId = sessionId;
		try {
			mFlingDeviceControllerListener.onApplicationConnected(
					applicationmetadata, applicationId, sessionId, relaunched);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Called when fling device's volume changed
	 */
	@Override
	public final void onVolumeChanged(String status, double volume,
			boolean muteState) {
		try {
			mFlingDeviceControllerListener
					.notifyApplicationStatusOrVolumeChanged(status, volume,
							muteState);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * called for request callback
	 */
	@Override
	public final void onRequestCallback(String namespace, long requestId) {
		try {
			mFlingDeviceControllerListener.requestCallback(namespace,
					requestId, 0);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * called for request callback
	 */
	@Override
	public final void onRequestCallback(String namespace, long requestId,
			int result) {
		try {
			mFlingDeviceControllerListener.requestCallback(namespace,
					requestId, result);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * notified app when message received
	 */
	@Override
	public final void notifyOnMessageReceived(String namespace, String message) {
		try {
			mFlingDeviceControllerListener
					.onMessageReceived(namespace, message);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * notified app when binary message is received
	 */
	@Override
	public final void onReceiveBinary(String namespace, byte message[]) {
		try {
			mFlingDeviceControllerListener.onReceiveBinary(namespace, message);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * notify app when connected to device without app
	 */
	@Override
	public final void onConnectedWithoutApp() {
		try {
			mFlingCallbacks
					.onPostInitComplete(1001, mStubImpl.asBinder(), null);
			FlingService.log().d("Connected to device without app.");
		} catch (RemoteException e) {
			e.printStackTrace();
			FlingService.log().d(e, "client died while brokering service",
					new Object[0]);
		}
	}

	/**
	 * notify app when failed connected with application
	 */
	@Override
	public final void onApplicationConnectionFailed(int reason) {
		try {
			mFlingDeviceControllerListener
					.postApplicationConnectionResult(reason);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * notify app when failed to connect to device
	 */
	@Override
	public final void onConnectionFailed() {
		try {
			mFlingCallbacks.onPostInitComplete(FlingStatusCodes.NETWORK_ERROR,
					null, null);
		} catch (RemoteException e) {
			e.printStackTrace();
			FlingService.log().d(e, "client died while brokering service",
					new Object[0]);
		}
	}

	/**
	 * notify app the current request status
	 */
	@Override
	public final void onRequestStatus(int result) {
		try {
			mFlingDeviceControllerListener.onRequestStatus(result);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * notify app that the request is invalid
	 */
	@Override
	public final void onInvalidRequest() {
		try {
			mFlingDeviceControllerListener
					.onRequestResult(FlingStatusCodes.INVALID_REQUEST); // 2001
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * notify app that application is disconnected for some reason
	 */
	@Override
	public final void onApplicationDisconnected(int reason) {
		try {
			mFlingDeviceControllerListener.onApplicationDisconnected(reason);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
}
