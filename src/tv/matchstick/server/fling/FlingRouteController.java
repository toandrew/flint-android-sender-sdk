package tv.matchstick.server.fling;

import tv.matchstick.fling.FlingDevice;
import tv.matchstick.server.fling.media.RouteController;

public final class FlingRouteController extends RouteController {
    private final FlingDevice mFlingDevice;
    private final FlingMediaRouteProvider mFlingMediaRouteProvider;

    public FlingRouteController(FlingMediaRouteProvider provider,
            FlingDevice flingdevice) {
        super();
        mFlingMediaRouteProvider = provider;
        mFlingDevice = flingdevice;
    }

    @Override
    public final void onRelease() {
        FlingMediaRouteProvider.getLogs_a().d("Controller released",
                new Object[0]);
    }

    @Override
    public final void onSelect() {
        FlingMediaRouteProvider.getLogs_a().d("onSelect");
        FlingMediaRouteProvider
                .setDeviceControllerListener(mFlingMediaRouteProvider, this);
    }

    @Override
    public final void onUnselect() {
        FlingMediaRouteProvider.getLogs_a().d("onUnselect");
        FlingMediaRouteProvider.b(mFlingMediaRouteProvider, this);
    }

    public final void g() {
        FlingMediaRouteProvider.b(mFlingMediaRouteProvider, this);
    }
    
    public FlingDevice getFlingDevice() {
        return mFlingDevice;
    }
}
