
package tv.matchstick.server.fling.mdns;

import tv.matchstick.client.internal.ValueChecker;

public final class MdnsClientPrivData
{
    final byte[] a;
    final long mCurrentTime;
    final int c;

    public MdnsClientPrivData(byte[] paramArrayOfByte, int paramInt)
    {
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
