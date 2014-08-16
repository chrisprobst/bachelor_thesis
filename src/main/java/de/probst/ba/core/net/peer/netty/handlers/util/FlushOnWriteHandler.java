package de.probst.ba.core.net.peer.netty.handlers.util;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 * Created by chrisprobst on 16.08.14.
 */
public final class FlushOnWriteHandler extends ChannelHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ctx.writeAndFlush(msg, promise);
    }
}
