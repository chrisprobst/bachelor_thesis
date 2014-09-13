package de.probst.ba.core.net.peer.peers.netty.handlers.traffic;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.Objects;

public final class WriteRequest {
    private final ChannelHandlerContext channelHandlerContext;
    private final Object message;
    private final ChannelPromise channelPromise;

    public WriteRequest(ChannelHandlerContext channelHandlerContext,
                        Object message,
                        ChannelPromise channelPromise) {
        Objects.requireNonNull(channelHandlerContext);
        Objects.requireNonNull(message);
        Objects.requireNonNull(channelPromise);
        this.channelHandlerContext = channelHandlerContext;
        this.message = message;
        this.channelPromise = channelPromise;
    }

    public void writeAndFlush() {
        channelHandlerContext.writeAndFlush(message, channelPromise);
    }

    public long getMessageSize() {
        return TrafficUtil.estimateMessageSize(message);
    }
}