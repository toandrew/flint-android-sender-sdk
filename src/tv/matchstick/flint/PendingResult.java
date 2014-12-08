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

package tv.matchstick.flint;

import java.util.concurrent.TimeUnit;

/**
 * This interface used for flint request wait and callback process.
 *
 * Represents a pending result from calling an API method in Flint service. The
 * final result object from a PendingResult is of type R, which can be retrieved
 * in one of two ways. via blocking calls to await(), or await(long, TimeUnit),
 * or via a callback by passing in an object implementing interface
 * ResultCallback to setResultCallback(ResultCallback). After the result has
 * been retrieved using await() or delivered to the result callback, it is an
 * error to attempt to retrieve the result again. It is the responsibility of
 * the caller or callback receiver to release any resources associated with the
 * returned result. Some result types may implement Releasable, in which case
 * release() should be used to free the associated resources.
 *
 * @param <R>
 */
public interface PendingResult<R extends Result> {
    /**
     * Block wait.
     *
     * Blocks until the task is completed.
     *
     * @return
     */
    public R await();

    /**
     * Blocks until the task is completed or has timed out waiting for the
     * result.
     *
     * @param time
     * @param timeUnit
     * @return
     */
    public R await(long time, TimeUnit timeUnit);

    /**
     * Set the callback here if you want the result to be delivered via a
     * callback when the result is ready or has timed out waiting for the
     * result.
     *
     * @param resultCb
     */
    public void setResultCallback(ResultCallback<R> resultCb);

    /**
     * Set the callback here if you want the result to be delivered via a
     * callback when the result is ready.
     *
     * The returned result object can have an additional failure mode of
     * TIMEOUT.
     * 
     * @param resultCb
     * @param time
     * @param timeUnit
     */
    public void setResultCallback(ResultCallback<R> resultCb, long time,
            TimeUnit timeUnit);
}
