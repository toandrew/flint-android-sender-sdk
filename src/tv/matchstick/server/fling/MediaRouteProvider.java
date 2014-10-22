package tv.matchstick.server.fling;

import tv.matchstick.server.fling.media.RouteController;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

/**
 * @author jianminz
 */
public abstract class MediaRouteProvider {
    private static final int MSG_DELIVER_DESCRIPTOR_CHANGED = 1;
    private static final int MSG_DELIVER_DISCOVERY_REQUEST_CHANGED = 2;

    public final Context mContext;
    final ComponentName mProviderComponentName;

    public final Handler mHandler = new Handler() {
        public final void handleMessage(Message message) {
            switch (message.what) {
            case MSG_DELIVER_DESCRIPTOR_CHANGED:
                mPendingDescriptorChange = false;
                if (mDescriptorChangedListener != null) {
                    mDescriptorChangedListener
                            .onDescriptorChanged(mMediaRouteProviderDescriptor);
                    return;
                }
                break;
            case MSG_DELIVER_DISCOVERY_REQUEST_CHANGED: // device discovery
                                                        // request
                mPendingDiscoveryRequestChange = false;
                onDiscoveryRequestChanged(mDiscoveryRequest); // call
                                                              // FlingMediaRouteProvider_awb.a
                break;
            }
        }
    };

    DescriptorChangedListener mDescriptorChangedListener;
    public DiscoveryRequest mDiscoveryRequest;
    boolean mPendingDiscoveryRequestChange;
    public MediaRouteProviderDescriptor mMediaRouteProviderDescriptor;
    public boolean mPendingDescriptorChange;

    public MediaRouteProvider(Context context) {
        if (context == null)
            throw new IllegalArgumentException("context must not be null");
        this.mContext = context;

        this.mProviderComponentName = new ComponentName(context, getClass());
    }

    public RouteController getRouteController(String routeId) {
        return null;
    }

    public void onDiscoveryRequestChanged(DiscoveryRequest request) {
    }
}
