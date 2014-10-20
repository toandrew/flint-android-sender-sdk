package tv.matchstick.server.fling.socket;

import android.os.SystemClock;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import tv.matchstick.server.utils.LOG;

public final class FlingSocketMultiplexer {
    private static LOG mLogs = new LOG("FlingSocketMultiplexer");
    private static FlingSocketMultiplexer mInstance;
    private final LinkedList<FlingSocket> mRegistedFlingSocketList = new LinkedList<FlingSocket>();
    private final LinkedList<FlingSocket> mAllFlingSocketList = new LinkedList<FlingSocket>();
    private Selector mSelector;
    private volatile boolean mQuitLoop;
    private volatile boolean g;
    private volatile Thread mFlingSocketMultiplexerThread;
    private final AtomicBoolean mReadyToConnect = new AtomicBoolean(false);
    private volatile Throwable j;

    private CountDownLatch mCountDownLatch;

    private FlingSocketMultiplexer() {
    }

    public static FlingSocketMultiplexer getInstance() {
        if (mInstance == null) {
            mInstance = new FlingSocketMultiplexer();
        }

        return mInstance;
    }

    synchronized void processData() {
        ArrayList<FlingSocket> errorFlingSocketList = new ArrayList<FlingSocket>();

        int i1 = 0;
        while (!mQuitLoop) {
            long elapsedRealtime_l1 = SystemClock.elapsedRealtime();
            if (mReadyToConnect.getAndSet(false)) {
                synchronized (mAllFlingSocketList) {
                    Iterator<FlingSocket> iterator3 = mAllFlingSocketList
                            .iterator();
                    while (iterator3.hasNext()) {
                        FlingSocket flingSocket = (FlingSocket) iterator3
                                .next();
                        try {
                            flingSocket.startConnecd().register(mSelector, 0)
                                    .attach(flingSocket);

                            mRegistedFlingSocketList.add(flingSocket);
                        } catch (Exception e) {
                            mLogs.d(e, "Error while connecting socket.",
                                    new Object[0]);
                            errorFlingSocketList.add(flingSocket);
                        }
                    }
                    mAllFlingSocketList.clear();
                }
            }

            if (!errorFlingSocketList.isEmpty()) {
                for (Iterator<FlingSocket> iterator2 = errorFlingSocketList
                        .iterator(); iterator2.hasNext(); ((FlingSocket) iterator2
                        .next()).onConnectError())
                    ;
                errorFlingSocketList.clear();
            }

            boolean flag = false;
            synchronized (mRegistedFlingSocketList) {
                Iterator<FlingSocket> iterator = mRegistedFlingSocketList
                        .iterator();

                while (iterator.hasNext()) {
                    FlingSocket flingSocket = (FlingSocket) iterator.next();
                    SocketChannel socketchannel = flingSocket
                            .getSocketChannel();
                    if (socketchannel == null
                            || socketchannel.keyFor(mSelector) == null
                            || !flingSocket.checkInterestOps(
                                    socketchannel.keyFor(mSelector),
                                    elapsedRealtime_l1)) {
                        iterator.remove();
                    } else {
                        boolean flag2;
                        if (flingSocket.isConnecting()
                                || flingSocket.isDisconnecting()) // need
                                                                  // check.
                                                                  // todo
                            flag2 = true;
                        else
                            flag2 = flag;
                        flag = flag2;
                    }
                }
            }

            long l2;
            if (flag)
                l2 = 1000L;
            else
                l2 = 0L;

            try {
                i1 = mSelector.select(l2);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (i1 != 0) {
                if (mQuitLoop) {
                    return;
                }

                Iterator<SelectionKey> iterator1 = mSelector.selectedKeys()
                        .iterator();
                while (iterator1.hasNext()) {
                    try {
                        SelectionKey selectionkey = (SelectionKey) iterator1
                                .next();
                        FlingSocket flingSocket = (FlingSocket) selectionkey
                                .attachment();
                        if (selectionkey.isConnectable()
                                && !flingSocket.onConnectable())
                            mRegistedFlingSocketList.remove(flingSocket);
                        if (selectionkey.isReadable() && !flingSocket.onRead())
                            mRegistedFlingSocketList.remove(flingSocket);
                        if (selectionkey.isWritable() && !flingSocket.onWrite())
                            mRegistedFlingSocketList.remove(flingSocket);
                    } catch (CancelledKeyException e) {
                    }
                    iterator1.remove();
                }
            }
        }
    }

    private void checkStatus() {
        if (g) {
            StringBuffer stringbuffer = new StringBuffer();
            stringbuffer.append("selector thread aborted due to ");
            if (j != null) {
                stringbuffer.append(j.getClass().getName());
                StackTraceElement astacktraceelement[] = j.getStackTrace();
                stringbuffer.append(" at ")
                        .append(astacktraceelement[0].getFileName())
                        .append(':')
                        .append(astacktraceelement[0].getLineNumber());
            } else {
                stringbuffer.append("unknown condition");
            }
            throw new IllegalStateException(stringbuffer.toString());
        }
        if (mFlingSocketMultiplexerThread == null)
            throw new IllegalStateException("not started; call start()");
        else
            return;
    }

    final synchronized void init() throws IOException {
        if (mFlingSocketMultiplexerThread != null) {
            return;
        }

        mLogs.d("starting multiplexer", new Object[0]);
        mCountDownLatch = new CountDownLatch(1);
        g = false;
        mQuitLoop = false;
        boolean flag = false;
        try {
            mSelector = Selector.open();
            mFlingSocketMultiplexerThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        mCountDownLatch.countDown();
                        processData();
                        return;
                    } catch (Throwable localThrowable) {
                        mLogs.e(localThrowable,
                                "Unexpected throwable in selector loop",
                                new Object[0]);
                        j = localThrowable;
                        g = true;
                        return;
                    } finally {
                        mLogs.d("**** selector loop thread exiting",
                                new Object[0]);
                        mFlingSocketMultiplexerThread = null;
                    }
                }

            });

            mFlingSocketMultiplexerThread.setName("FlingSocketMultiplexer");
            mFlingSocketMultiplexerThread.start();

            flag = mCountDownLatch.await(1L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (flag) {
            return;
        }

        throw new IOException(
                "timed out or interrupted waiting for muxer thread to start");
    }

    public final synchronized void doConnect(FlingSocket flingSocket) {
        try {
            checkStatus();
            synchronized (mAllFlingSocketList) {
                mLogs.d("added socket", new Object[0]);
                mAllFlingSocketList.add(flingSocket);
            }
            mReadyToConnect.set(true);
            mSelector.wakeup();
        } finally {

        }

        return;
    }

    public final synchronized void wakeup() {
        try {
            checkStatus();
            mSelector.wakeup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
