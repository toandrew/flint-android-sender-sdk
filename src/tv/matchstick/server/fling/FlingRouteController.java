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
        FlingMediaRouteProvider.log().d("Controller released",
                new Object[0]);
    }

    @Override
    public final void onSelect() {
        FlingMediaRouteProvider.log().d("onSelect");
        mFlingMediaRouteProvider
                .setDeviceControllerListener(this);
    }

    @Override
    public final void onUnselect() {
        FlingMediaRouteProvider.log().d("onUnselect");
        mFlingMediaRouteProvider.b(this);
    }

    public final void g() {
        mFlingMediaRouteProvider.b(this);
    }
    
    public FlingDevice getFlingDevice() {
        return mFlingDevice;
    }
}
