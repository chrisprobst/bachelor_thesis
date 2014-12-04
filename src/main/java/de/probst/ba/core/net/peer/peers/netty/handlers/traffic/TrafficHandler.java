package de.probst.ba.core.net.peer.peers.netty.handlers.traffic;

import de.probst.ba.core.util.concurrent.trafficshaper.MessageSink;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by chrisprobst on 16.08.14.
 */
public final class TrafficHandler extends ChannelHandlerAdapter {

    private final MessageSink<Object> uploadMessageSink;
    private final MessageSink<Object> downloadMessageSink;

    public TrafficHandler(MessageSink<Object> uploadMessageSink, MessageSink<Object> downloadMessageSink) {
        Objects.requireNonNull(uploadMessageSink);
        Objects.requireNonNull(downloadMessageSink);
        this.uploadMessageSink = uploadMessageSink;
        this.downloadMessageSink = downloadMessageSink;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        uploadMessageSink.sinkMessage(obj -> ctx.writeAndFlush(obj, promise), msg, msg instanceof Serializable)
                         .getTrafficShaper()
                         .run();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        downloadMessageSink.sinkMessage(ctx::fireChannelRead, msg, msg instanceof Serializable)
                           .getTrafficShaper()
                           .run();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        uploadMessageSink.close();
        downloadMessageSink.close();
        super.channelInactive(ctx);
    }
}