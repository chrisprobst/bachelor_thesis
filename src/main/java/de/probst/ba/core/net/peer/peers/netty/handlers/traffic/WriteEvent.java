package de.probst.ba.core.net.peer.peers.netty.handlers.traffic;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.Objects;

public final class WriteEvent extends AbstractMessageEvent {

    private final ChannelPromise channelPromise;

    public WriteEvent(ChannelHandlerContext channelHandlerContext,
                      Object message,
                      ChannelPromise channelPromise) {
        super(channelHandlerContext, message);
        Objects.requireNonNull(channelPromise);
        this.channelPromise = channelPromise;
    }

    @Override
    public void dispatch() {
        channelHandlerContext.writeAndFlush(message, channelPromise);
    }
}