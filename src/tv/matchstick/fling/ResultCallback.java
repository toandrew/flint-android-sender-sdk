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

package tv.matchstick.fling;

/**
 * An interface for receiving a Result from a PendingResult as an asynchronous
 * callback.
 */
public interface ResultCallback<R extends Result> {
    /**
     * Called when result is returned
     *
     * It is the responsibility of each callback to release any resources
     * associated with the result. Some result types may implement Releasable,
     * in which case release() should be used to free the associated resources.
     * 
     * @param result
     *            The result from the API call. May not be null.
     */
    public void onResult(R result);
}
