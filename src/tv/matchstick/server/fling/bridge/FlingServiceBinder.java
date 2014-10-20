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
    public final void initFlingService(IFlingCallbacks callbacks, int version,
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
        } catch (Exception exception) {
            FlingService.log().e(exception, "Fling device was not valid.",
                    new Object[0]);
            try {
                callbacks.onPostInitComplete(10, null, null);
            } catch (RemoteException remoteexception) {
                FlingService.log().d("client died while brokering service");
            }
        }
    }
}
