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

package tv.matchstick.flint.service;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import tv.matchstick.server.flint.FlintDialController;
import tv.matchstick.server.flint.FlintMediaRouteProvider;
import tv.matchstick.server.flint.MediaRouteProvider;
import tv.matchstick.server.flint.MediaRouteProviderSrv;
import tv.matchstick.server.flint.mdns.DeviceScanner;
import tv.matchstick.server.flint.service.operation.FlintDeviceScannerOperation;
import tv.matchstick.server.flint.service.operation.FlintOperation;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

/**
 * This class is used to do several things:
 * 
 * <ul>
 * <li>all Flint related operations: connect, start/join/stop application,send
 * messages,etc
 * <li>Flint device scan operations: start, stop device scans
 * <li>contained something for media route device found
 * </ul>
 */
public class FlintDeviceService extends MediaRouteProviderSrv {

    private static final String MEDIA_ROUTE_ACTION = "android.media.MediaRouteProviderService";

    private static final String SERVICE_ACTION_KEY = "what";
    private static final String SERVICE_ACTION_FLINT = "flint";
    private static final String SERVICE_ACTION_SCAN = "scan";

    public final MediaRouteProvider getInstance() {
        return FlintMediaRouteProvider.getInstance(this);
    }

    /**
     * Flint operation queue(launch application,stop application,etc)
     */
    private static final ConcurrentLinkedQueue<FlintOperation> mFlintOperationQueue = new ConcurrentLinkedQueue<FlintOperation>();

    /**
     * Start Flint service to do Flint operation(launch application,etc)
     * 
     * @param context
     * @param operation
     *            Flint operation
     */
    private static void startFlintService(Context context,
            FlintOperation operation) {
        mFlintOperationQueue.offer(operation);
        context.startService(buildIntent(context, MEDIA_ROUTE_ACTION,
                SERVICE_ACTION_FLINT));
    }

    /**
     * Connect to Flint device by using its IP and port
     * 
     * @param context
     * @param controller
     */
    public static void connectFlintDevice(Context context,
            final FlintDialController controller) {
        startFlintService(context, new FlintOperation(controller) {
            @Override
            public void doFlint() throws IOException {
                controller.connectToDeviceInternal();
            }
        });
    }

    /**
     * Set Flint device's volume
     * 
     * @param context
     * @param controller
     * @param level
     * @param mute
     */
    public static void setVolume(Context context,
            final FlintDialController controller, final boolean isSetVolume, final double level,
            final boolean mute) {
        startFlintService(context, new FlintOperation(controller) {

            @Override
            public void doFlint() throws IOException {
                controller.setVolumeInternal(isSetVolume, level, mute);
            }

        });
    }

    /**
     * Do something when connecting to Flint device failed by using Flint socket
     * 
     * @param context
     * @param controller
     * @param socketError
     */
    public static void onSocketConnectionFailed(Context context,
            final FlintDialController controller, final int socketError) {
        startFlintService(context, new FlintOperation(controller) {

            @Override
            public void doFlint() throws IOException {
                controller.onSocketConnectionFailedInternal(socketError);
            }

        });
    }

    /**
     * Stop Flint application
     * 
     * @param context
     * @param controller
     */
    public static void stopApplication(Context context,
            final FlintDialController controller) {
        startFlintService(context, new FlintOperation(controller) {

            @Override
            public void doFlint() throws IOException {
                controller.stopApplicationInternal();
            }

        });
    }

    /**
     * Join Flint application
     * 
     * @param context
     * @param controller
     * @param applicationId
     */
    public static void joinApplication(Context context,
            final FlintDialController controller, final String applicationId, final boolean useIpc) {
        startFlintService(context, new FlintOperation(controller) {

            @Override
            public void doFlint() throws IOException {
                controller.joinApplicationInternal(applicationId, useIpc);
            }

        });
    }

    /**
     * Send Text messages to Flint device
     * 
     * @param context
     * @param controller
     * @param namespace
     * @param message
     * @param id
     * @param transportId
     */
    public static void sendTextMessage(Context context,
            final FlintDialController controller, final String namespace,
            final String message) {
        startFlintService(context, new FlintOperation(controller) {

            @Override
            public void doFlint() throws IOException {
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
     * Launch Flint application
     * 
     * @param context
     * @param controller
     * @param applicationId
     * @param relaunch
     */
    public static void launchApplication(Context context,
            final FlintDialController controller, final String applicationId,
            final boolean relaunch, final boolean useIpc) {
        startFlintService(context, new FlintOperation(controller) {

            @Override
            public void doFlint() throws IOException {
                controller.launchApplicationInternal(applicationId, relaunch, useIpc);
            }

        });
    }

    public static void procReceivedMessage(Context context,
            final FlintDialController controller, final String message) {
        startFlintService(context, new FlintOperation(controller) {

            @Override
            public void doFlint() throws IOException {
                controller.onReceivedMessage(message);
            }

        });
    }

    /**
     * Leave Flint application
     * 
     * @param context
     * @param controller
     */
    public static void leaveApplication(Context context,
            final FlintDialController controller) {
        startFlintService(context, new FlintOperation(controller) {

            @Override
            public void doFlint() throws IOException {
                controller.leaveApplicationInternal();
            }

        });
    }

    /**
     * Process Flint socket disconnected event
     * 
     * @param context
     * @param controller
     * @param socketError
     *            socket error code
     */
    public static void onSocketDisconnected(Context context,
            final FlintDialController controller, final int socketError) {
        startFlintService(context, new FlintOperation(controller) {

            @Override
            public void doFlint() throws IOException {
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
            final FlintDialController controller, final String namespace) {
        startFlintService(context, new FlintOperation(controller) {

            @Override
            public void doFlint() throws IOException {
                controller.addNamespace(namespace);
            }

        });
    }

    /**
     * Request current Flint device's status
     * 
     * @param context
     * @param controller
     */
    public static void requestStatus(Context context,
            final FlintDialController controller) {
        startFlintService(context, new FlintOperation(controller) {

            @Override
            public void doFlint() throws IOException {
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
            final FlintDialController controller, final String namespace) {
        startFlintService(context, new FlintOperation(controller) {

            @Override
            public void doFlint() throws IOException {
                controller.removeNamespace(namespace);
            }

        });
    }

    /**
     * Do something when connected with Flint socket. (device auth,etc)
     * 
     * @param context
     * @param controller
     */
    public static void onSocketConnected(Context context,
            final FlintDialController controller) {
        startFlintService(context, new FlintOperation(controller) {

            @Override
            public void doFlint() throws IOException {
                controller.onSocketConnectedInternal();
            }

        });
    }

    /**
     * Device scan operation queue(start scan device, stop scan device,etc)
     */
    private static final ConcurrentLinkedQueue<FlintDeviceScannerOperation> mDeviceScanOperationQueue = new ConcurrentLinkedQueue<FlintDeviceScannerOperation>();

    /**
     * Start scan Flint device
     * 
     * @param context
     * @param deviceScanner
     */
    public static void startScanFlintDevice(Context context,
            final DeviceScanner deviceScanner) {
        addScanOperation(context,
                new FlintDeviceScannerOperation(deviceScanner) {

                    @Override
                    public void act() {
                        deviceScanner.onAllDevicesOffline();
                        deviceScanner.startScan();
                    }

                });
    }

    /**
     * Stop scan Flint device
     * 
     * @param context
     * @param deviceScanner
     */
    public static void stopScanFlintDevice(Context context,
            final DeviceScanner deviceScanner) {
        addScanOperation(context,
                new FlintDeviceScannerOperation(deviceScanner) {

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
            FlintDeviceScannerOperation operation) {
        mDeviceScanOperationQueue.offer(operation);
        context.startService(buildIntent(context, MEDIA_ROUTE_ACTION,
                SERVICE_ACTION_SCAN));
    }

    /**
     * Do device scan operations
     */
    private void doDeviceScanOperations() {
        FlintDeviceScannerOperation operation;
        long startTime;
        operation = (FlintDeviceScannerOperation) mDeviceScanOperationQueue
                .poll();
        if (operation == null) {
            Log.e("FlintDeviceService", "operation missing");
            return;
        }
        startTime = SystemClock.elapsedRealtime();
        Log.d("FlintDeviceService", "Starting operation: >> "
                + operation.getClass().getSimpleName());

        try {
            operation.act();
        } catch (Exception e) {
            e.printStackTrace();
        }

        long elapsed = SystemClock.elapsedRealtime() - startTime;
        Log.d("FlintDeviceService", "Finished operation: << "
                + operation.getClass().getSimpleName() + " (" + elapsed
                + "ms elapsed)");
    }

    /**
     * Process all operations in Flint operation queue(launch/stop
     * applications,etc).
     */
    private void doFlintOperations() {
        FlintOperation operation = (FlintOperation) mFlintOperationQueue.poll();
        if (operation == null) {
            Log.e("FlintIntentService", "operation missing");
            return;
        }
        try {
            operation.doFlint();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            Log.e("FlintService", "intent is null.ignore it!!!!");
            return;
        }

        String action = intent.getAction();
        if (action.equals(MEDIA_ROUTE_ACTION)) {
            String extras = (String) intent.getStringExtra(SERVICE_ACTION_KEY);
            if (extras == null) {
                Log.e("FlintDeviceService", "Media scan intent?!");
                return;
            }
            if (extras.equals(SERVICE_ACTION_FLINT)) {
                doFlintOperations();
            } else if (extras.equals(SERVICE_ACTION_SCAN)) {
                doDeviceScanOperations();
            } else {
                Log.e("FlintService", "unknown actions!!!![" + intent
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
