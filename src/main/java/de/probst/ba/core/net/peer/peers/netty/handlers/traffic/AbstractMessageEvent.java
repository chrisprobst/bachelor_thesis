package de.probst.ba.core.net.peer.peers.netty.handlers.traffic;

import io.netty.channel.ChannelHandlerContext;

import java.util.Objects;

/**
 * Created by chrisprobst on 14.09.14.
 */
public abstract class AbstractMessageEvent {

    protected final ChannelHandlerContext channelHandlerContext;
    protected final Object message;

    public AbstractMessageEvent(ChannelHandlerContext channelHandlerContext, Object message) {
        Objects.requireNonNull(channelHandlerContext);
        Objects.requireNonNull(message);
        this.channelHandlerContext = channelHandlerContext;
        this.message = message;
    }

    public abstract void dispatch();

    public long getMessageSize() {
        return TrafficUtil.estimateMessageSize(message);
    }
}
