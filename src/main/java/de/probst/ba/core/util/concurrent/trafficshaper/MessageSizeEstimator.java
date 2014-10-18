package de.probst.ba.core.util.concurrent.trafficshaper;

import io.netty.buffer.ByteBuf;

/**
 * Created by chrisprobst on 13.09.14.
 */
public final class MessageSizeEstimator {

    private static volatile long defaultEstimatedMessageSize;

    public static long getDefaultEstimatedMessageSize() {
        return defaultEstimatedMessageSize;
    }

    public static void setDefaultEstimatedMessageSize(long defaultEstimatedMessageSize) {
        if (defaultEstimatedMessageSize < 0) {
            throw new IllegalArgumentException("defaultEstimatedMessageSize < 0");
        }
        MessageSizeEstimator.defaultEstimatedMessageSize = defaultEstimatedMessageSize;
    }

    public static long estimateMessageSize(Object msg) {
        if (msg == null) {
            return 0;
        } else if (msg instanceof ByteBuf) {
            return ((ByteBuf) msg).readableBytes();
        } else if (msg instanceof byte[]) {
            return ((byte[]) msg).length;
        } else if (msg.getClass().isAnnotationPresent(EstimatedMessageSize.class)) {
            return msg.getClass().getAnnotation(EstimatedMessageSize.class).value();
        } else {
            return getDefaultEstimatedMessageSize();
        }
    }

    private MessageSizeEstimator() {
    }
}
