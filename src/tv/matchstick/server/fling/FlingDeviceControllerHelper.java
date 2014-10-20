
package tv.matchstick.server.fling;

import java.util.ArrayList;
import java.util.List;

final class FlingDeviceControllerHelper {
    public boolean b;
    public boolean c;
    public boolean d;
    public final List<FlingRouteController> e = new ArrayList<FlingRouteController>();
    final FlingMediaRouteProvider f;

    public FlingDeviceControllerHelper(FlingMediaRouteProvider provider)
    {
        super();

        f = provider;
        b = true;
    }

    public final boolean isEmpty()
    {
        return e.isEmpty();
    }
    
    public FlingDeviceController getController() {
        return FlingDeviceController.getCurrentController();
    }
}
