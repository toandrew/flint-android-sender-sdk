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

package tv.matchstick.server.fling.mdns;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tv.matchstick.client.internal.ValueChecker;
import tv.matchstick.server.fling.mdns.FlingDeviceInfoContainer.FlingDeviceInfo;
import tv.matchstick.server.utils.LOG;
import android.text.TextUtils;

abstract class MdnsClient {
	private static final LOG log = new LOG("MdnsClient");

	private final String mHostName;

	private MulticastSocket mMulticastSocket;

	private final byte[] mBuf = new byte[65536];

	private Thread mReceiveThread;
	private Thread mSendThread;

	private volatile boolean mStopScan;

	private final NetworkInterface mNetworkInterface;
	private final Set i = new LinkedHashSet();
	private static final Charset mCharset;
	private static final InetAddress mInetAddress;

	static {
		Charset charSet = null;
		try {
			charSet = Charset.forName("UTF-8");
		} catch (IllegalCharsetNameException e) {
			e.printStackTrace();
		} catch (UnsupportedCharsetException ex) {
			ex.printStackTrace();
		}

		mCharset = charSet;

		InetAddress address = null;
		try {
			address = InetAddress.getByName("224.0.0.251");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		mInetAddress = address;
	}

	public MdnsClient(String hostName, NetworkInterface network) {
		this.mHostName = hostName;
		this.mNetworkInterface = network;
	}

	private void handleReceive(MdnsClient mdnsClient) throws IOException {
		byte[] arrayOfByte1 = new byte[4];
		byte[] arrayOfByte2 = new byte[16];
		DatagramPacket received = new DatagramPacket(mdnsClient.mBuf,
				mdnsClient.mBuf.length);
		MdnsClientDpHelper localavj = null;
		int k = 0;
		int m = 0;
		int n = 0;
		FlingDeviceInfoContainer localavm = null;
		while (!mdnsClient.mStopScan) {
			try {
				mdnsClient.mMulticastSocket.receive(received);

				log.d("received a packet of length %d", received.getLength());
				localavj = new MdnsClientDpHelper(received);
				localavj.b();
				localavj.b();
				int j = localavj.b();
				k = localavj.b();
				m = localavj.b();
				n = localavj.b();
				if (j != 1)
					throw new IOException("invalid response");
			} catch (IOException e) {
				if (!mdnsClient.mStopScan) {
					log.d(e.toString(), "while receiving packet");
					continue;
				}
			}
			String str1 = TextUtils.join(".", localavj.c());
			localavj.b();
			localavj.b();
			localavm = new FlingDeviceInfoContainer(str1);

			for (int i1 = 0;; i1++) {
				int i2;
				if (i1 < n + (k + m)) {
					String[] arrayOfString = localavj.c();
					i2 = localavj.b();
					localavj.b();
					localavj.a(4);
					byte[] arrayOfByte3 = localavj.mData;
					int i3 = localavj.b;
					localavj.b = (i3 + 1);
					long l1 = (long) (0xFF & arrayOfByte3[i3]) << 24;
					byte[] arrayOfByte4 = localavj.mData;
					int i4 = localavj.b;
					localavj.b = (i4 + 1);
					long l2 = l1 | (long) (0xFF & arrayOfByte4[i4]) << 16;
					byte[] arrayOfByte5 = localavj.mData;
					int i5 = localavj.b;
					localavj.b = (i5 + 1);
					long l3 = l2 | (long) (0xFF & arrayOfByte5[i5]) << 8;
					byte[] arrayOfByte6 = localavj.mData;
					int i6 = localavj.b;
					localavj.b = (i6 + 1);
					long l4 = l3 | (long) (0xFF & arrayOfByte6[i6]);
					int i7 = localavj.b();
					if ((i1 < k) && (l4 > 0L) && (l4 < 604800L)) {
						int i11 = localavj.b;
						byte[] arrayOfByte7 = Arrays.copyOfRange(
								received.getData(), i11, i11 + i7);
						Set localSet = mdnsClient.i;
						MdnsClientPrivData localavc = new MdnsClientPrivData(
								arrayOfByte7, (int) l4);
						localSet.add(new SoftReference(localavc));
					}
					FlingDeviceInfo localavl1 = localavm.mFlingDeviceInfo;
					if ((localavl1.mTTL < 0L) || (l4 < localavl1.mTTL)) {
						localavl1.mTTL = l4;
					}
					switch (i2) {
					case 1:
						localavj.a(arrayOfByte1);
						try {
							Inet4Address localInet4Address = (Inet4Address) InetAddress
									.getByAddress(arrayOfByte1);
							FlingDeviceInfo localavl4 = localavm.mFlingDeviceInfo;
							if (localavl4.mIpV4AddrList == null)
								localavl4.mIpV4AddrList = new ArrayList();
							localavl4.mIpV4AddrList.add(localInet4Address);
						} catch (UnknownHostException localUnknownHostException2) {
						} catch (IOException e) {
							e.printStackTrace();
						}

						break;
					case 12:
						localavj.c();
						break;
					case 16:
						if (i7 >= 0)
							localavj.c = (i7 + localavj.b);
						while (localavj.a() > 0) {
							String str2 = localavj.d();
							FlingDeviceInfo localavl2 = localavm.mFlingDeviceInfo;
							if (localavl2.mTextStringList == null)
								localavl2.mTextStringList = new ArrayList();
							localavl2.mTextStringList.add(str2);
						}
						localavj.c = -1;
						break;
					case 28:
						localavj.a(arrayOfByte2);
						try {
							Inet6Address localInet6Address = (Inet6Address) InetAddress
									.getByAddress(arrayOfByte2);
							FlingDeviceInfo localavl3 = localavm.mFlingDeviceInfo;
							if (localavl3.mIpV6AddrList == null)
								localavl3.mIpV6AddrList = new ArrayList();
							localavl3.mIpV6AddrList.add(localInet6Address);
						} catch (UnknownHostException localUnknownHostException1) {
						} catch (IOException e) {
							e.printStackTrace();
						}
						break;
					case 33:
						int i8 = localavj.b();
						localavm.mFlingDeviceInfo.mPriority = i8;
						int i9 = localavj.b();
						localavm.mFlingDeviceInfo.mWeight = i9;
						int i10 = localavj.b();
						localavm.mFlingDeviceInfo.mPort = i10;
						String str3 = TextUtils.join(".", localavj.c());
						localavm.mFlingDeviceInfo.mHost = str3;

						// todo
						if (arrayOfString.length < 4) {
							throw new IOException("invalid name in SRV record");
						} else if (arrayOfString.length > 4) {
							String[] fixArrayOfString = new String[4];
							fixArrayOfString[3] = arrayOfString[arrayOfString.length - 1];
							fixArrayOfString[2] = arrayOfString[arrayOfString.length - 2];
							fixArrayOfString[1] = arrayOfString[arrayOfString.length - 3];
							int nameCount = arrayOfString.length - 3;
							String[] name = new String[nameCount];
							for (int len = 0; len < nameCount; len++) {
								name[len] = arrayOfString[len];
							}
							fixArrayOfString[0] = TextUtils.join(".", name);
							arrayOfString = fixArrayOfString;
						}

						String str4 = arrayOfString[0];
						localavm.mFlingDeviceInfo.e = str4;
						String str5 = arrayOfString[1];
						localavm.mFlingDeviceInfo.mName = str5;
						String str6 = arrayOfString[2];
						if (str6.equals("_tcp")) {
							localavm.setProto(1);
						} else if (str6.equals("_udp")) {
							localavm.setProto(2);
						}

						break;
					default:
						localavj.a(i7);
						localavj.b = (i7 + localavj.b);
						break;
					}
				} else {
					mdnsClient.onScanResults(localavm.mFlingDeviceInfo);
					break;
				}
			}
		}
	}

	private static void stopThread(Thread thread) {
		while (true)
			try {
				thread.interrupt();
				thread.join();
				return;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

	private void handleSend(MdnsClient mdnsClient) {
		int j = 1;
		int k = 1000;
		MdnsClientOutputStreamHelper helper;
		int n;
		int i1;
		int i4;

		while (!mdnsClient.mStopScan) {
			try {
				String hostName = mdnsClient.mHostName;
				helper = new MdnsClientOutputStreamHelper();
				Iterator localIterator1 = mdnsClient.i.iterator();
				n = 1024;
				while (localIterator1.hasNext()) {
					if (n <= 1024) {
						MdnsClientPrivData privData = (MdnsClientPrivData) ((SoftReference) localIterator1
								.next()).get();
						if (privData != null) {
							if ((System.currentTimeMillis() - privData.mCurrentTime) / 1000L <= privData.c / 2.0D) {
								i1 = 0;
							} else {
								i1 = 1;
							}
							if (i1 != 0) {
								localIterator1.remove();
							}
						} else {
							localIterator1.remove();

						}
					} else {
						localIterator1.remove();
					}

					n--;
				} // while
				helper.b(0);
				helper.b(0);
				helper.b(1);
				helper.b(mdnsClient.i.size());
				helper.b(0);
				helper.b(0);
				String[] arrayOfString = hostName.split("\\.");
				int i2 = arrayOfString.length;
				for (int i3 = 0; i3 < i2; i3++) {
					byte[] arrayOfByte1 = arrayOfString[i3].getBytes(mCharset);
					helper.a(arrayOfByte1.length);
					helper.a(arrayOfByte1);
				}
				helper.a(0);
				helper.b(12);
				if (j != 0) {
					i4 = 32768;
				} else {
					i4 = 0;
				}
				helper.b(i4 | 0x1);
				Iterator localIterator2 = mdnsClient.i.iterator();
				while (localIterator2.hasNext()) {
					MdnsClientPrivData localavc2 = (MdnsClientPrivData) ((SoftReference) localIterator2
							.next()).get();
					if (localavc2 != null)
						helper.a(localavc2.a);
				}

				DatagramPacket datagramPacket = helper.createDatagramPacket();
				byte[] arrayOfByte2 = datagramPacket.getData();
				int length = datagramPacket.getLength();
				StringBuilder packets = new StringBuilder(
						2 * arrayOfByte2.length);
				int i6 = 0;
				int i7 = 0;
				while (i7 < length + 0) {
					Object[] arrayOfObject1 = new Object[1];
					arrayOfObject1[0] = Integer
							.valueOf(0xFF & arrayOfByte2[i7]);
					packets.append(String.format("%02X ", arrayOfObject1));
					i6++;
					if (i6 % 8 == 0) {
						packets.append('\n');
					}

					i7++;
				}

				log.d("packet:\n%s", packets.toString());
				mdnsClient.mMulticastSocket.send(datagramPacket);

			} catch (IOException e) {
				e.printStackTrace();
			}

			j = 0;

			try {
				Thread.currentThread();
				Thread.sleep(k);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// 32000 is default
			if (k >= 8000) {
				k = 8000;
			} else {
				k *= 2;
			}
			// int m;
			// if (k >= 32000) {
			// m = k;
			// } else {
			// m = k * 2;
			// k = m;
			// }
		}
	}

	public final synchronized void startScan() {
		try {
			MulticastSocket socket = this.mMulticastSocket;
			if (socket != null) {
				return;
			}

			this.mStopScan = false;
			this.mMulticastSocket = new MulticastSocket();
			this.mMulticastSocket.setTimeToLive(1);
			if (this.mNetworkInterface != null)
				this.mMulticastSocket
						.setNetworkInterface(this.mNetworkInterface);
			this.mReceiveThread = new Thread(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					try {
						handleReceive(MdnsClient.this);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			});
			this.mReceiveThread.start();
			this.mSendThread = new Thread(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					handleSend(MdnsClient.this);
				}

			});
			this.mSendThread.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected abstract void onScanResults(FlingDeviceInfo paramavl);

	public final synchronized void stopScan() {
		try {
			MulticastSocket socket = this.mMulticastSocket;
			if (socket == null) {
				return;
			}

			this.mStopScan = true;
			this.mMulticastSocket.close();
			if (this.mReceiveThread != null) {
				stopThread(this.mReceiveThread);
				this.mReceiveThread = null;
			}
			if (this.mSendThread != null) {
				stopThread(this.mSendThread);
				this.mSendThread = null;
			}
			this.mMulticastSocket = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	final class MdnsClientDpHelper {
		final byte[] mData;
		int b;
		int c;
		private final int mDataLen;
		private final Map e;

		public MdnsClientDpHelper(DatagramPacket paramDatagramPacket) {
			this.mData = paramDatagramPacket.getData();
			this.mDataLen = paramDatagramPacket.getLength();
			this.b = 0;
			this.c = -1;
			this.e = new HashMap();
		}

		private int e() {
			a(1);
			byte[] arrayOfByte = this.mData;
			int i = this.b;
			this.b = (i + 1);
			return 0xFF & arrayOfByte[i];
		}

		public final int a() {
			int i = this.c;
			if (c < 0) {
				i = this.mDataLen;
			}
			return i - this.b;
		}

		final void a(int paramInt) {
			try {
				if (a() < paramInt)
					throw new EOFException();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public final void a(byte[] paramArrayOfByte) {
			a(paramArrayOfByte.length);
			System.arraycopy(this.mData, this.b, paramArrayOfByte, 0,
					paramArrayOfByte.length);
			this.b += paramArrayOfByte.length;
		}

		public final int b() {
			a(2);
			byte[] arrayOfByte1 = this.mData;
			int i = this.b;
			this.b = (i + 1);
			int j = (0xFF & arrayOfByte1[i]) << 8;
			byte[] arrayOfByte2 = this.mData;
			int k = this.b;
			this.b = (k + 1);
			return j | 0xFF & arrayOfByte2[k];
		}

		public final String[] c() {
			HashMap localHashMap = new HashMap();
			ArrayList localArrayList1 = new ArrayList();
			int i;
			int j;
			while (a() > 0) {
				a(1);
				i = this.mData[this.b];
				if (i == 0) {
					this.b = (1 + this.b);
					break;
				} else {
					if ((i & 0xC0) == 192) {
						j = 1;
					} else {
						j = 0;
					}
					int k;
					List localList;
					Object localObject;
					k = this.b;
					if (j == 0) {
						// break label233;
						String str = d();
						ArrayList localArrayList2 = new ArrayList();
						localArrayList2.add(str);
						localObject = localArrayList2;
					} else {
						int m = (0x3F & e()) << 8 | 0xFF & e();
						localList = (List) this.e.get(Integer.valueOf(m));
						try {
							if (localList == null) {
								throw new IOException("invalid label pointer");
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
						localObject = localList;
					}

					localArrayList1.addAll((Collection) localObject);
					Iterator localIterator = localHashMap.keySet().iterator();
					while (localIterator.hasNext())
						((List) localHashMap
								.get((Integer) localIterator.next()))
								.addAll((Collection) localObject);

					localHashMap.put(Integer.valueOf(k), localObject);
					if (j == 0) {
						continue;
					} else {
						break;
					}
				}
			}
			this.e.putAll(localHashMap);
			return (String[]) localArrayList1
					.toArray(new String[localArrayList1.size()]);
		}

		public final String d() {
			int i = e();
			a(i);
			String str = new String(this.mData, this.b, i, mCharset);
			this.b = (i + this.b);
			return str;
		}
	}

	final class MdnsClientOutputStreamHelper {
		private ByteArrayOutputStream a = new ByteArrayOutputStream(16384);
		private DataOutputStream b = new DataOutputStream(this.a);

		public final DatagramPacket createDatagramPacket() {
			try {
				this.b.flush();
				byte[] arrayOfByte = this.a.toByteArray();
				return new DatagramPacket(arrayOfByte, arrayOfByte.length,
						mInetAddress, 5353);
			} catch (IOException e) {
				// break label7;
				e.printStackTrace();
			}

			return null;
		}

		public final void a(int paramInt) {
			try {
				this.b.writeByte(paramInt & 0xFF);
				return;
			} catch (IOException localIOException) {
			}
		}

		public final void a(byte[] paramArrayOfByte) {
			try {
				this.b.write(paramArrayOfByte, 0, paramArrayOfByte.length);
				return;
			} catch (IOException localIOException) {
			}
		}

		public final void b(int paramInt) {
			try {
				this.b.writeShort(0xFFFFF & paramInt);
				return;
			} catch (IOException localIOException) {
			}
		}
	}

	private static final class MdnsClientPrivData {
		final byte[] a;
		final long mCurrentTime;
		final int c;

		public MdnsClientPrivData(byte[] paramArrayOfByte, int paramInt) {
			boolean bool = false;
			if (paramInt <= 0 || paramInt >= 604800) {
				bool = false;
			} else {
				bool = true;
			}

			ValueChecker.checkTrue(bool);
			this.a = paramArrayOfByte;
			this.mCurrentTime = System.currentTimeMillis();
			this.c = paramInt;
		}
	}
}
