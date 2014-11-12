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

package tv.matchstick.server.fling.bridge;

import tv.matchstick.client.internal.IFlingDeviceController;
import tv.matchstick.fling.service.FlingService;
import tv.matchstick.server.fling.FlingDialController;
import android.os.RemoteException;

public final class FlingDeviceControllerStubImpl extends

IFlingDeviceController.Stub {
    final FlingService mFlingService;
    private final FlingDialController mFlingDeviceController;

    public FlingDeviceControllerStubImpl(FlingService service,
            FlingDialController controller) {
        super();
        mFlingService = service;
        mFlingDeviceController = controller;
    }

    @Override
    public void disconnect() throws RemoteException {
        // TODO Auto-generated method stub

        if (!mFlingDeviceController.isDisposed()) {
            mFlingDeviceController.releaseReference();
        }
    }

    @Override
    public void launchApplication(String applicationId,
            boolean relaunchIfRunning) throws RemoteException {
        // TODO Auto-generated method stub

        mFlingDeviceController.launchApplication(applicationId,
                relaunchIfRunning);
    }

    @Override
    public void joinApplication(String applicationId)
            throws RemoteException {
        // TODO Auto-generated method stub

        mFlingDeviceController.joinApplication(applicationId);
    }

    @Override
    public void leaveApplication() throws RemoteException {
        // TODO Auto-generated method stub

        mFlingDeviceController.leaveApplication();
    }

    @Override
    public void stopApplication() throws RemoteException {
        // TODO Auto-generated method stub

        mFlingDeviceController.stopApplication();
    }

    @Override
    public void requestStatus() throws RemoteException {
        // TODO Auto-generated method stub

        mFlingDeviceController.requestStatus();
    }

    @Override
    public void setVolume(double volume, boolean isMute)
            throws RemoteException {
        // TODO Auto-generated method stub

        mFlingDeviceController.setVolume(volume, isMute);
    }

    @Override
    public void sendMessage(String namespace, String message, long requestId)
            throws RemoteException {
        // TODO Auto-generated method stub

        if (namespace == null || namespace.length() > 128) {
            return;
        }

        mFlingDeviceController.sendMessageInternal(namespace, message,
                requestId);
    }

    @Override
    public void setMessageReceivedCallbacks(String namespace)
            throws RemoteException {
        // TODO Auto-generated method stub

        mFlingDeviceController.setMessageReceivedCallbacks(namespace);
    }

    @Override
    public void removeMessageReceivedCallbacks(String namespace)
            throws RemoteException {
        // TODO Auto-generated method stub

        mFlingDeviceController.removeMessageReceivedCallbacks(namespace);
    }
}
