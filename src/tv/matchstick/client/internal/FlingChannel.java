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

package tv.matchstick.client.internal;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fling Channel
 */
public abstract class FlingChannel {
	private static final AtomicInteger idGen = new AtomicInteger(0);

	protected final LogUtil log;

	private final String mNamespace;

	private MessageSender mMessageSender;

	protected FlingChannel(String namespace, String tag) {
		mNamespace = namespace;

		log = new LogUtil(tag);
		log.setPrefixMsg(String.format("instance-%d", idGen.incrementAndGet()));
	}

	/**
	 * Get namespace of this channel
	 * 
	 * @return this channel's namespace
	 */
	public final String getNamespace() {
		return mNamespace;
	}

	/**
	 * set message sender
	 * 
	 * @param sender
	 */
	public final void setMessageSender(MessageSender sender) {
		mMessageSender = sender;

		if (mMessageSender != null) {
			return;
		}

		clean();
	}

	/**
	 * Send text message
	 * 
	 * @param message
	 *            message to send
	 * @param requestId
	 *            request's Id
	 * @param targetId
	 *            target's Id
	 * @throws IOException
	 */
	protected final void sendTextMessage(String message, long requestId,
			String targetId) throws IOException {
		log.v("Sending text message: %s to: %s", new Object[] { message,
				targetId });

		mMessageSender
				.sendTextMessage(mNamespace, message, requestId, targetId);
	}

	/**
	 * Send binary message
	 * 
	 * @param message
	 * @param transId
	 */
	protected final void sendBinaryMessage(byte message[], String transId) {
		log.v("Sending binary message to: %s", new Object[] { transId });

		mMessageSender.sendBinaryMessage(mNamespace, message, 0L, transId);
	}

	/**
	 * Called when string message received
	 * 
	 * @param message
	 */
	public void onMessageReceived(String message) {
	}

	/**
	 * Called when bytes message received
	 * 
	 * @param abyte0
	 */
	public void onMessageReceived(byte abyte0[]) {
	}

	/**
	 * Track un-success request
	 * 
	 * @param requestId
	 * @param statusCode
	 */
	public void trackUnSuccess(long requestId, int statusCode) {
	}

	/**
	 * Get current request Id
	 * 
	 * @return
	 */
	protected final long getRequestId() {
		return mMessageSender.getRequestId();
	}

	/**
	 * Do some clean work
	 */
	public void clean() {
	}
}
