package tv.matchstick.client.internal;

import java.io.IOException;

public interface MessageSender {
    public abstract void sendBinaryMessage(String namespace, byte message[],
            long requestId, String transId);

    public abstract void sendTextMessage(String namespace, String message, long requestId,
            String paramString3) throws IOException;

    public abstract long getRequestId();

}
