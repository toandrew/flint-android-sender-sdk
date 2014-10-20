package tv.matchstick.client.internal;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import tv.matchstick.client.internal.MessageSender;

public abstract class FlingChannel {

    private static final AtomicInteger xA = new AtomicInteger(0);
    protected final LogUtil mLogUtil;
    private final String mNamespace;
    private MessageSender mMessageSender;

    protected FlingChannel(String namespace, String logTag) {
        mNamespace = namespace;
        mLogUtil = new LogUtil(logTag);
        mLogUtil.U(String.format("instance-%d",
                new Object[] { Integer.valueOf(xA.incrementAndGet()) }));
    }

    public final String getNamespace() {
        return mNamespace;
    }

    public final void setMessageSender(MessageSender paramdw) {
        mMessageSender = paramdw;
        if (mMessageSender != null) {
            return;
        }
        clean();
    }

    protected final void sendTextMessage(String message, long requestId,
            String targetId) throws IOException {
        mLogUtil.logv("Sending text message: %s to: %s", new Object[] {
                message, targetId });
        mMessageSender
                .sendTextMessage(mNamespace, message, requestId, targetId);
    }

    protected final void sendBinaryMessage(byte message[], String transId) {
        mLogUtil.logv("Sending binary message to: %s", new Object[] { transId });
        mMessageSender.sendBinaryMessage(mNamespace, message, 0L, transId);
    }

    public void onMessageReceived(String message) {
    }

    public void onMessageReceived(byte abyte0[]) {
    }

    public void trackUnSuccess(long requestId, int statusCode) {
    }

    protected final long getRequestId() {
        return mMessageSender.getRequestId();
    }

    public void clean() {
    }
}
