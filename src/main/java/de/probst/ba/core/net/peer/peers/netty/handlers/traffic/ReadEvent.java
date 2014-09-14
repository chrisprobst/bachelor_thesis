package de.probst.ba.core.net.peer.peers.netty.handlers.traffic;

import io.netty.channel.ChannelHandlerContext;

/**
 * Created by chrisprobst on 14.09.14.
 */
public final class ReadEvent extends AbstractMessageEvent {

    public ReadEvent(ChannelHandlerContext channelHandlerContext, Object message) {
        super(channelHandlerContext, message);
    }

    @Override
    public void dispatch() {
        channelHandlerContext.fireChannelRead(message);
    }
}
