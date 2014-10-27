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

package tv.matchstick.server.fling.socket;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLException;

import tv.matchstick.server.fling.socket.data.FlingMessage;
import tv.matchstick.server.utils.LOG;
import android.content.Context;
import android.os.SystemClock;

public final class FlingSocket {
	private static final LOG log = new LOG("FlingSocket");

	private static final int SIMPLE_SOCKET_PORT = 8011;
	private final FlingSocketListener mSocketListener;
	private SocketChannel mSocketChannel;
	private SocketBuf mReadSocketBuf;
	private SocketBuf mWriteSocketBuf;
	private final FlingSocketMultiplexer mFlingSocketMultiplexer;
	private final int MAX_MESSAGE_SIZE = 0x1fffc;
	private int mSocketStatus;
	private InetSocketAddress mInetSocketAddress;
	private long mBeginConnectTime;
	private long mTimeoutTime;
	private long mDisconnectTime;
	private long m;
	private boolean n;

	public FlingSocket(Context context, FlingSocketListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener cannot be null");
		} else {
			mSocketListener = listener;
			mSocketStatus = 0;
			mFlingSocketMultiplexer = FlingSocketMultiplexer
					.getInstance(context);
		}
	}

	private void doTeardown(int reason) {
		log.d("doTeardown with reason=%d", reason);

		if (mSocketChannel != null) {
			try {
				mSocketChannel.close();
			} catch (IOException e) {
			}
			mSocketChannel = null;
		}
		mReadSocketBuf = null;
		mWriteSocketBuf = null;
		boolean flag = false;
		if (mSocketStatus == 1) // connecting
			flag = true;
		else
			flag = false;
		mSocketStatus = 0;
		mDisconnectTime = 0L;
		mBeginConnectTime = 0L;
		n = true;
		if (flag) {
			mSocketListener.onConnectionFailed(reason);
		} else {
			mSocketListener.onDisconnected(reason);
		}
	}

	private synchronized void connectInternal(Inet4Address hostAddr, int port) {
		try {
			mFlingSocketMultiplexer.init();

			log.d("Connecting to %s:%d", hostAddr, port);
			int socketPort = SIMPLE_SOCKET_PORT;
			mInetSocketAddress = new InetSocketAddress(hostAddr, socketPort);
			mTimeoutTime = 10000L;// 5000L;
			m = 2000L;
			mFlingSocketMultiplexer.doConnect(this);
			mSocketStatus = 1; // connecting
			n = false;

			return;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void handleRead() throws IOException {
		boolean flag = false;
		Long long1;

		while (true) {
			if (mReadSocketBuf.e) {
				flag = false;
			} else {
				mReadSocketBuf.d = mReadSocketBuf.b;
				String str = null;
				byte abyte[] = new byte[mReadSocketBuf.c];
				System.arraycopy(mReadSocketBuf.a, 0, abyte, 0,
						mReadSocketBuf.c);
				str = new String(abyte, "utf-8");
				android.util.Log.d("FlingSocket", "read string : " + str);
				String[] subStr = str.split(":");
				long1 = Long.valueOf(subStr[0]);
				for (int i = 0; i < subStr[0].length() + 1; i++) {
					mReadSocketBuf.f();
				}

				if (long1 == null) {
					flag = true;
				} else {
					if (long1.longValue() > (long) MAX_MESSAGE_SIZE) {
						throw new IOException("invalid message size received");
					}

					if ((long) mReadSocketBuf.d() >= long1.longValue()) {

						byte abyte0[] = new byte[(int) long1.longValue()];

						int i1 = abyte0.length;
						if (mReadSocketBuf.d() >= i1) {
							int j1;
							int k1;
							int l1;
							if (mReadSocketBuf.e)
								j1 = 0;
							else if (mReadSocketBuf.b < mReadSocketBuf.c)
								j1 = mReadSocketBuf.c - mReadSocketBuf.b;
							else
								j1 = mReadSocketBuf.a.length - mReadSocketBuf.b;
							k1 = Math.min(i1, j1);
							System.arraycopy(mReadSocketBuf.a,
									mReadSocketBuf.b, abyte0, 0, k1);
							mReadSocketBuf.a(k1);
							l1 = i1 - k1;
							if (l1 > 0) {
								int i2 = k1 + 0;
								System.arraycopy(mReadSocketBuf.a,
										mReadSocketBuf.b, abyte0, i2, l1);
								mReadSocketBuf.a(l1);
							}
						}

						String message = new String(abyte0, "utf-8");
						android.util.Log.d("FlingSocket", "received message : "
								+ message);
						FlingMessage msg = new FlingMessage();
						msg.parseJson(message);
						mSocketListener.onMessageReceived(ByteBuffer
								.wrap(message.getBytes("UTF-8")));

						continue;
					} else {
						flag = true;
					}
				}
			}

			if (flag) {
				if (mReadSocketBuf.d != -1) {
					if (mReadSocketBuf.b != mReadSocketBuf.d) {
						mReadSocketBuf.b = mReadSocketBuf.d;
						mReadSocketBuf.e = false;
					}
					mReadSocketBuf.d = -1;
				}
				return;
			} else {
				mReadSocketBuf.d = -1;
				if (!mReadSocketBuf.e) {
					return;
				}

				mReadSocketBuf.c = 0;
				mReadSocketBuf.b = 0;
				return;
			}
		}
	}

	final synchronized SocketChannel startConnecd() throws IOException {
		log.d("startConnect");
		mBeginConnectTime = SystemClock.elapsedRealtime();
		mSocketChannel = SocketChannel.open();
		mSocketChannel.configureBlocking(false);
		mReadSocketBuf = new SocketBuf();
		mWriteSocketBuf = new SocketBuf();

		if (mSocketChannel.connect(mInetSocketAddress)) {
			mSocketStatus = 2;
			mSocketListener.onConnected();
			return mSocketChannel;
		}

		return mSocketChannel;
	}

	public final void connect(Inet4Address hostAddress, int port) {
		connectInternal(hostAddress, port);
	}

	public final synchronized void send(ByteBuffer bytebuffer)
			throws IOException {
		if (mSocketStatus != 2)
			throw new IllegalStateException("not connected; state="
					+ this.mSocketStatus);

		if (bytebuffer == null)
			throw new IllegalArgumentException("message cannot be null");

		// sample
		StringBuffer sb = new StringBuffer();
		FlingMessage flingMessage = new FlingMessage(bytebuffer.array());
		String json = flingMessage.buildJson().toString();
		int length = json.getBytes("utf-8").length;
		sb.append(length);
		sb.append(":");
		sb.append(json);
		android.util.Log.d("FlingSocket", "send message : " + sb.toString());
		byte[] message = sb.toString().getBytes("utf-8");
		bytebuffer = ByteBuffer.wrap(message);

		byte[] sendBuf = bytebuffer.array();
		int pos = bytebuffer.position();
		int remain = bytebuffer.remaining();
		int writeCount = 0;
		if (mWriteSocketBuf.c() >= remain) {
			if (!mWriteSocketBuf.e) {
				if (mWriteSocketBuf.c < mWriteSocketBuf.b) {
					writeCount = mWriteSocketBuf.b - mWriteSocketBuf.c;
				} else {
					writeCount = mWriteSocketBuf.a.length - mWriteSocketBuf.c;
				}
			} else {
				writeCount = mWriteSocketBuf.a.length;
			}
		} else {
			this.mFlingSocketMultiplexer.wakeup();
			return;
		}

		int actualWriteCount = Math.min(remain, writeCount);
		System.arraycopy(sendBuf, pos, mWriteSocketBuf.a, mWriteSocketBuf.c,
				actualWriteCount);
		mWriteSocketBuf.b(actualWriteCount);
		int currentPos = pos + actualWriteCount;
		int remainedCount = remain - actualWriteCount;
		if (remainedCount > 0) {
			System.arraycopy(sendBuf, currentPos, mWriteSocketBuf.a,
					mWriteSocketBuf.c, remainedCount);
			mWriteSocketBuf.b(remainedCount);
		}
		this.mFlingSocketMultiplexer.wakeup();
	}

	final synchronized boolean checkInterestOps(SelectionKey selectionkey,
			long elapsedRealtime) {
		boolean ok = false;
		if (n) {
			log.w("Socket is no longer connected");
			n = false;
			return false;
		}
		int mode = 0;
		switch (mSocketStatus) {
		case 1: // conncting?
			if (elapsedRealtime - this.mBeginConnectTime >= this.mTimeoutTime) {
				doTeardown(3);
			} else {
				if (!mSocketChannel.isConnected()) {
					mode = SelectionKey.OP_CONNECT;// 8;
				}
				selectionkey.interestOps(mode);
				ok = true;
			}
			break;
		case 2: // connected
			boolean flag1 = mReadSocketBuf.e();
			mode = 0;
			if (!flag1) {
				mode = SelectionKey.OP_READ; // 1;
			}
			if (!mWriteSocketBuf.e) {
				mode |= SelectionKey.OP_WRITE; // 4;
			}
			selectionkey.interestOps(mode);
			ok = true;
			break;
		case 3:// disconnecting?
			if (elapsedRealtime - mDisconnectTime < m) {

				if (mWriteSocketBuf.e) {
					doTeardown(0);
					ok = false;
					break;
				}

				mode = SelectionKey.OP_WRITE;// 4;

				selectionkey.interestOps(mode);
				ok = true;
			} else {
				doTeardown(0);
				ok = false;
			}

			break;
		}

		return ok;
	}

	public final synchronized void disconnect() {
		mSocketStatus = 3;
		mDisconnectTime = SystemClock.elapsedRealtime();
		mFlingSocketMultiplexer.wakeup();
	}

	public final synchronized boolean isConnected() {
		return (mSocketStatus == 2);
	}

	public final synchronized boolean isConnecting() {
		return (mSocketStatus == 1);
	}

	public final synchronized boolean isDisconnecting() {
		boolean flag = false;
		if (mSocketStatus == 3) {
			flag = true;
		}

		return flag;
	}

	public final synchronized int getState() {
		return mSocketStatus;
	}

	public final synchronized byte[] getPeerCertificate() {
		return null;
	}

	final synchronized boolean onConnectable() {
		boolean flag = true;

		log.d("onConnectable");
		try {
			mSocketChannel.finishConnect();

			mSocketStatus = 2;
			mSocketListener.onConnected();
		} catch (SSLException e) {
			log.d(e.toString(), "exception in onConnectable");
			doTeardown(4);
			flag = false;
		} catch (IOException ex) {
			log.d(ex.toString(), "exception in onConnectable");
			doTeardown(2);
			flag = false;
		}

		return flag;
	}

	final synchronized void onConnectError() {
		log.d("onConnectError");
		try {
			doTeardown(2);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	final synchronized boolean onRead() {
		boolean flag = true;
		try {
			if (!mReadSocketBuf.e()) {
				int i1 = (int) mSocketChannel.read(mReadSocketBuf.b());
				if (i1 <= 0) {
					throw new ClosedChannelException();
				}
				mReadSocketBuf.b(i1);
			}

			handleRead();
		} catch (ClosedChannelException e) {
			log.w(e, "ClosedChannelException when state was %d", mSocketStatus);
			doTeardown(1);
			flag = false;
		} catch (SSLException ee) {
			log.w(ee, "SSLException encountered. Tearing down the socket.");
			doTeardown(4);
			flag = false;
		} catch (IOException ex) {
			log.w(ex, "IOException encountered. Tearing down the socket.");
			doTeardown(2);
			flag = false;
		}
		return flag;
	}

	final synchronized boolean onWrite() {
		boolean flag = false;
		try {
			if (!mWriteSocketBuf.e) {
				int i1 = (int) mSocketChannel.write(mWriteSocketBuf.a());
				if (i1 <= 0) {
					// cond_2
					throw new ClosedChannelException();
				}
				mWriteSocketBuf.a(i1);
			}

			if (!mWriteSocketBuf.e || mSocketStatus != 3) {
				return true;
			}

			doTeardown(0);
		} catch (ClosedChannelException e) {
			log.w(e, "ClosedChannelException when state was %d", mSocketStatus);
			doTeardown(1);
		} catch (SSLException ex) {
			log.w(ex, "SSLException encountered. Tearing down the socket.");
			doTeardown(4);
			flag = false;
		} catch (IOException eio) {
			log.w(eio, "IOException encountered. Tearing down the socket.");
			doTeardown(2);
			flag = false;
		}

		return flag;
	}

	final synchronized SocketChannel getSocketChannel() {
		return mSocketChannel;
	}

	final class SocketBuf {
		byte a[];
		int b;
		int c;
		int d;
		boolean e;
		ByteBuffer f[];
		ByteBuffer g[];

		public SocketBuf() {
			a = new byte[0x20000];
			f = new ByteBuffer[1];
			g = new ByteBuffer[2];
			d = -1;
			e = true;
		}

		final void a(byte byte0) {
			a[c] = byte0;
			b(1);
		}

		final void a(int i) {
			label0: {
				b = i + b;
				if (b >= a.length)
					b = b - a.length;
				if (b == c) {
					if (d != -1)
						break label0;
					c = 0;
					b = 0;
					d = -1;
					e = true;
				}
				return;
			}
			e = true;
		}

		public final ByteBuffer[] a() {
			if (b <= c) {
				ByteBuffer abytebuffer1[] = f;
				abytebuffer1[0] = ByteBuffer.wrap(a, b, c - b);
				return abytebuffer1;
			} else {
				ByteBuffer abytebuffer[] = g;
				abytebuffer[0] = ByteBuffer.wrap(a, b, a.length - b);
				abytebuffer[1] = ByteBuffer.wrap(a, 0, c);
				return abytebuffer;
			}
		}

		final void b(int i) {
			c = i + c;
			if (c >= a.length)
				c = c - a.length;
			e = false;
			d = -1;
		}

		public final ByteBuffer[] b() {
			if (c < b) {
				ByteBuffer abytebuffer1[] = f;
				abytebuffer1[0] = ByteBuffer.wrap(a, c, b - c);
				return abytebuffer1;
			} else {
				ByteBuffer abytebuffer[] = g;
				abytebuffer[0] = ByteBuffer.wrap(a, c, a.length - c);
				abytebuffer[1] = ByteBuffer.wrap(a, 0, b);
				return abytebuffer;
			}
		}

		public final int c() {
			if (e)
				return a.length;
			if (c < b)
				return b - c;
			else
				return (a.length - c) + b;
		}

		public final int d() {
			if (e)
				return 0;
			if (b < c)
				return c - b;
			else
				return (a.length - b) + c;
		}

		public final boolean e() {
			return !e && b == c;
		}

		final byte f() {
			byte byte0 = a[b];
			a(1);
			return byte0;
		}
	}

}
