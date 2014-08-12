package de.probst.ba.core.net.handlers;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.EventExecutor;

/**
 * Created by chrisprobst on 12.08.14.
 */
@ChannelHandler.Sharable
public final class ChannelGroupHandler extends ChannelHandlerAdapter {

    // All channels
    private final ChannelGroup channelGroup;

    private void addChannel(ChannelHandlerContext ctx) {
        channelGroup.add(ctx.channel());
    }

    private void removeChannel(ChannelHandlerContext ctx) {
        channelGroup.remove(ctx.channel());
    }

    public ChannelGroupHandler(EventExecutor eventExecutor) {
        channelGroup = new DefaultChannelGroup(eventExecutor);
    }

    public ChannelGroup getChannelGroup() {
        return channelGroup;
    }

    ////
    //// Used for channel group management
    ////

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isActive() && ctx.channel().isRegistered()) {
            addChannel(ctx);
        }
        super.handlerAdded(ctx);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isActive()) {
            addChannel(ctx);
        }
        super.channelRegistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        addChannel(ctx);
        super.channelActive(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        removeChannel(ctx);
        super.handlerRemoved(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        removeChannel(ctx);
        super.channelInactive(ctx);
    }
}
