package tv.matchstick.server.fling.bridge;

import tv.matchstick.client.internal.IFlingCallbacks;
import tv.matchstick.client.internal.IFlingServiceBroker;
import android.os.Bundle;
import android.os.IBinder;

public abstract class FlingServiceBrokerStub extends IFlingServiceBroker.Stub {
    @Override
    public void initFlingService(IFlingCallbacks callbacks, int version,
            String packageName, IBinder listener, Bundle bundle) {
        throw new IllegalArgumentException("Fling service not supported");
    }
}
