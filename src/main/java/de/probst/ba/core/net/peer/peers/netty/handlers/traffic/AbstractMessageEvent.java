package de.probst.ba.core.net.peer.peers.netty.handlers.traffic;

import io.netty.channel.ChannelHandlerContext;

import java.util.Objects;

/**
 * Created by chrisprobst on 14.09.14.
 */
public abstract class AbstractMessageEvent {

    protected final ChannelHandlerContext channelHandlerContext;
    protected final Object message;
    protected volatile long size;

    public AbstractMessageEvent(ChannelHandlerContext channelHandlerContext, Object message) {
        Objects.requireNonNull(channelHandlerContext);
        Objects.requireNonNull(message);
        this.channelHandlerContext = channelHandlerContext;
        this.message = message;
        size = TrafficUtil.estimateMessageSize(message);
    }

    public abstract void dispatch();

    public synchronized void decreaseMessageSize(long amount) {
        size = Math.max(size - amount, 0);
    }

    public long getMessageSize() {
        return size;
    }
}
