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

package tv.matchstick.server.fling.service.operation;

import java.io.IOException;

import tv.matchstick.server.fling.FlingDeviceController;

public abstract class FlingOperation {
    public static String TAG = "FlingOperation";

    protected final FlingDeviceController mFlingDeviceController;

    public FlingOperation(FlingDeviceController controller) {
        mFlingDeviceController = controller;
        mFlingDeviceController.generateId();
    }

    public abstract void doFling() throws IOException;

    public final void releaseReference() {
        mFlingDeviceController.releaseReference();
    }

}
