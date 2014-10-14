package de.probst.ba.core.net.peer.peers.netty.handlers.traffic;

import de.probst.ba.core.net.peer.peers.netty.NettyConfig;
import io.netty.buffer.ByteBuf;

/**
 * Created by chrisprobst on 13.09.14.
 */
public final class TrafficUtil {

    private TrafficUtil() {

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
            return NettyConfig.getDefaultEstimatedMessageSize();
        }
    }
}
