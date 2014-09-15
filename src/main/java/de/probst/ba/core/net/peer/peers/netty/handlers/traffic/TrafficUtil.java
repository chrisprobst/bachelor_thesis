package de.probst.ba.core.net.peer.peers.netty.handlers.traffic;

import io.netty.buffer.ByteBuf;

/**
 * Created by chrisprobst on 13.09.14.
 */
public final class TrafficUtil {

    private static volatile long defaultMessageSize;

    private TrafficUtil() {

    }

    public static long estimateMessageSize(Object msg) {
        if (msg instanceof ByteBuf) {
            return ((ByteBuf) msg).readableBytes();
        } else if (msg instanceof byte[]) {
            return ((byte[]) msg).length;
        } else {
            return getDefaultMessageSize();
        }
    }

    public static long getDefaultMessageSize() {
        return defaultMessageSize;
    }

    public static void setDefaultMessageSize(long defaultMessageSize) {
        TrafficUtil.defaultMessageSize = defaultMessageSize;
    }
}
