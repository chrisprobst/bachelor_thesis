package de.probst.ba.core.net.peer.handlers;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.EventExecutor;

/**
 * Sharable channel group handler.
 * <p>
 * Put this handle into one or more channel
 * pipelines and all of those channels will be
 * added to and removed from the internal channel
 * group.
 * <p>
 * Created by chrisprobst on 12.08.14.
 */
@ChannelHandler.Sharable
public final class ChannelGroupHandler extends ChannelHandlerAdapter {

    // All channels
    private final ChannelGroup channelGroup;

    public ChannelGroupHandler(EventExecutor eventExecutor) {
        channelGroup = new DefaultChannelGroup(eventExecutor);
    }

    /**
     * @return The channel group.
     */
    public ChannelGroup getChannelGroup() {
        return channelGroup;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channelGroup.add(ctx.channel());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        channelGroup.remove(ctx.channel());
        super.channelInactive(ctx);
    }
}
