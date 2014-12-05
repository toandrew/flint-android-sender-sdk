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

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import tv.matchstick.server.fling.FlingDialController;
import tv.matchstick.server.fling.FlingMediaRouteProvider;
import tv.matchstick.server.fling.MediaRouteProvider;
import tv.matchstick.server.fling.MediaRouteProviderSrv;
import tv.matchstick.server.fling.mdns.DeviceScanner;
import tv.matchstick.server.fling.service.operation.FlingDeviceScannerOperation;
import tv.matchstick.server.fling.service.operation.FlingOperation;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

/**
 * This class is used to do several things:
 * 
 * <ul>
 * <li>all fling related operations: connect, start/join/stop application,send
 * messages,etc
 * <li>fling device scan operations: start, stop device scans
 * <li>contained something for media route device found
 * </ul>
 */
public class FlingDeviceService extends MediaRouteProviderSrv {

    private static final String MEDIA_ROUTE_ACTION = "android.media.MediaRouteProviderService";

    private static final String SERVICE_ACTION_KEY = "what";
    private static final String SERVICE_ACTION_FLING = "fling";
    private static final String SERVICE_ACTION_SCAN = "scan";

    public final MediaRouteProvider getInstance() {
        return FlingMediaRouteProvider.getInstance(this);
    }

    /**
     * Fling operation queue(launch application,stop application,etc)
     */
    private static final ConcurrentLinkedQueue<FlingOperation> mFlingOperationQueue = new ConcurrentLinkedQueue<FlingOperation>();

    /**
     * Start fling service to do fling operation(launch application,etc)
     * 
     * @param context
     * @param operation
     *            fling operation
     */
    private static void startFlingService(Context context,
            FlingOperation operation) {
        mFlingOperationQueue.offer(operation);
        context.startService(buildIntent(context, MEDIA_ROUTE_ACTION,
                SERVICE_ACTION_FLING));
    }

    /**
     * Connect to fling device by using its IP and port
     * 
     * @param context
     * @param controller
     */
    public static void connectFlingDevice(Context context,
            final FlingDialController controller) {
        startFlingService(context, new FlingOperation(controller) {
            @Override
            public void doFling() throws IOException {
                controller.connectToDeviceInternal();
            }
        });
    }

    /**
     * Set fling device's volume
     * 
     * @param context
     * @param controller
     * @param level
     * @param mute
     */
    public static void setVolume(Context context,
            final FlingDialController controller, final double level,
            final boolean mute) {
        startFlingService(context, new FlingOperation(controller) {

            @Override
            public void doFling() throws IOException {
                controller.setVolumeInternal(level, mute);
            }

        });
    }

    /**
     * Do something when connecting to fling device failed by using fling socket
     * 
     * @param context
     * @param controller
     * @param socketError
     */
    public static void onSocketConnectionFailed(Context context,
            final FlingDialController controller, final int socketError) {
        startFlingService(context, new FlingOperation(controller) {

            @Override
            public void doFling() throws IOException {
                controller.onSocketConnectionFailedInternal(socketError);
            }

        });
    }

    /**
     * Stop fling application
     * 
     * @param context
     * @param controller
     */
    public static void stopApplication(Context context,
            final FlingDialController controller) {
        startFlingService(context, new FlingOperation(controller) {

            @Override
            public void doFling() throws IOException {
                controller.stopApplicationInternal();
            }

        });
    }

    /**
     * Join fling application
     * 
     * @param context
     * @param controller
     * @param applicationId
     */
    public static void joinApplication(Context context,
            final FlingDialController controller, final String applicationId, final boolean useIpc) {
        startFlingService(context, new FlingOperation(controller) {

            @Override
            public void doFling() throws IOException {
                controller.joinApplicationInternal(applicationId, useIpc);
            }

        });
    }

    /**
     * Send Text messages to fling device
     * 
     * @param context
     * @param controller
     * @param namespace
     * @param message
     * @param id
     * @param transportId
     */
    public static void sendTextMessage(Context context,
            final FlingDialController controller, final String namespace,
            final String message) {
        startFlingService(context, new FlingOperation(controller) {

            @Override
            public void doFling() throws IOException {
                try {
                    controller.sendTextMessage(namespace, message);
                    return;
                } catch (IllegalStateException e) {
                    Log.e(TAG, e.getMessage());
                }
            }

        });
    }

    /**
     * Launch fling application
     * 
     * @param context
     * @param controller
     * @param applicationId
     * @param relaunch
     */
    public static void launchApplication(Context context,
            final FlingDialController controller, final String applicationId,
            final boolean relaunch, final boolean useIpc) {
        startFlingService(context, new FlingOperation(controller) {

            @Override
            public void doFling() throws IOException {
                controller.launchApplicationInternal(applicationId, relaunch, useIpc);
            }

        });
    }

    public static void procReceivedMessage(Context context,
            final FlingDialController controller, final String message) {
        startFlingService(context, new FlingOperation(controller) {

            @Override
            public void doFling() throws IOException {
                controller.onReceivedMessage(message);
            }

        });
    }

    /**
     * Leave fling application
     * 
     * @param context
     * @param controller
     */
    public static void leaveApplication(Context context,
            final FlingDialController controller) {
        startFlingService(context, new FlingOperation(controller) {

            @Override
            public void doFling() throws IOException {
                controller.leaveApplicationInternal();
            }

        });
    }

    /**
     * Process fling socket disconnected event
     * 
     * @param context
     * @param controller
     * @param socketError
     *            socket error code
     */
    public static void onSocketDisconnected(Context context,
            final FlingDialController controller, final int socketError) {
        startFlingService(context, new FlingOperation(controller) {

            @Override
            public void doFling() throws IOException {
                controller.onSocketDisconnectedInternal(socketError);
            }

        });
    }

    /**
     * Process message received callback. only add it into our namespace list?
     * 
     * @param context
     * @param controller
     * @param namespace
     */
    public static void setMessageReceivedCallbacks(Context context,
            final FlingDialController controller, final String namespace) {
        startFlingService(context, new FlingOperation(controller) {

            @Override
            public void doFling() throws IOException {
                controller.addNamespace(namespace);
            }

        });
    }

    /**
     * Request current fling device's status
     * 
     * @param context
     * @param controller
     */
    public static void requestStatus(Context context,
            final FlingDialController controller) {
        startFlingService(context, new FlingOperation(controller) {

            @Override
            public void doFling() throws IOException {
                controller.getStatus();
            }

        });
    }

    /**
     * Process remove message received callback event. Just remove namespace
     * from list?
     * 
     * @param context
     * @param controller
     * @param namespace
     */
    public static void removeMessageReceivedCallbacks(Context context,
            final FlingDialController controller, final String namespace) {
        startFlingService(context, new FlingOperation(controller) {

            @Override
            public void doFling() throws IOException {
                controller.removeNamespace(namespace);
            }

        });
    }

    /**
     * Do something when connected with fling socket. (device auth,etc)
     * 
     * @param context
     * @param controller
     */
    public static void onSocketConnected(Context context,
            final FlingDialController controller) {
        startFlingService(context, new FlingOperation(controller) {

            @Override
            public void doFling() throws IOException {
                controller.onSocketConnectedInternal();
            }

        });
    }

    /**
     * Device scan operation queue(start scan device, stop scan device,etc)
     */
    private static final ConcurrentLinkedQueue<FlingDeviceScannerOperation> mDeviceScanOperationQueue = new ConcurrentLinkedQueue<FlingDeviceScannerOperation>();

    /**
     * Start scan fling device
     * 
     * @param context
     * @param deviceScanner
     */
    public static void startScanFlingDevice(Context context,
            final DeviceScanner deviceScanner) {
        addScanOperation(context,
                new FlingDeviceScannerOperation(deviceScanner) {

                    @Override
                    public void act() {
                        deviceScanner.onAllDevicesOffline();
                        deviceScanner.startScan();
                    }

                });
    }

    /**
     * Stop scan fling device
     * 
     * @param context
     * @param deviceScanner
     */
    public static void stopScanFlingDevice(Context context,
            final DeviceScanner deviceScanner) {
        addScanOperation(context,
                new FlingDeviceScannerOperation(deviceScanner) {

                    @Override
                    public void act() {
                        deviceScanner.stopScan();
                    }

                });
    }

    /**
     * Add scan operation(start/stop)
     * 
     * @param context
     * @param operation
     */
    private static void addScanOperation(Context context,
            FlingDeviceScannerOperation operation) {
        mDeviceScanOperationQueue.offer(operation);
        context.startService(buildIntent(context, MEDIA_ROUTE_ACTION,
                SERVICE_ACTION_SCAN));
    }

    /**
     * Do device scan operations
     */
    private void doDeviceScanOperations() {
        FlingDeviceScannerOperation operation;
        long startTime;
        operation = (FlingDeviceScannerOperation) mDeviceScanOperationQueue
                .poll();
        if (operation == null) {
            Log.e("FlingDeviceService", "operation missing");
            return;
        }
        startTime = SystemClock.elapsedRealtime();
        Log.d("FlingDeviceService", "Starting operation: >> "
                + operation.getClass().getSimpleName());

        try {
            operation.act();
        } catch (Exception e) {
            e.printStackTrace();
        }

        long elapsed = SystemClock.elapsedRealtime() - startTime;
        Log.d("FlingDeviceService", "Finished operation: << "
                + operation.getClass().getSimpleName() + " (" + elapsed
                + "ms elapsed)");
    }

    /**
     * Process all operations in fling operation queue(launch/stop
     * applications,etc).
     */
    private void doFlingOperations() {
        FlingOperation operation = (FlingOperation) mFlingOperationQueue.poll();
        if (operation == null) {
            Log.e("FlingIntentService", "operation missing");
            return;
        }
        try {
            operation.doFling();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            Log.e("FlingService", "intent is null.ignore it!!!!");
            return;
        }

        String action = intent.getAction();
        if (action.equals(MEDIA_ROUTE_ACTION)) {
            String extras = (String) intent.getStringExtra(SERVICE_ACTION_KEY);
            if (extras == null) {
                Log.e("FlingDeviceService", "Media scan intent?!");
                return;
            }
            if (extras.equals(SERVICE_ACTION_FLING)) {
                doFlingOperations();
            } else if (extras.equals(SERVICE_ACTION_SCAN)) {
                doDeviceScanOperations();
            } else {
                Log.e("FlingService", "unknown actions!!!![" + intent
                        + "]action[" + extras + "]");
            }
        }
    }

    private static Intent buildIntent(final Context context, String action,
            String data) {
        Intent intent = new Intent(action);
        intent.putExtra("what", data);
        intent.setPackage(context.getPackageName());
        return intent;
    }
}
