package de.probst.ba.core.net.peer.local;

import de.probst.ba.core.net.peer.AbstractPeer;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;

/**
 * Created by chrisprobst on 12.08.14.
 */
public class LocalPeer extends AbstractPeer {

    private ServerBootstrap serverBootstrap;

    private Bootstrap bootstrap;

    protected void initServerBootstrap() {
        (serverBootstrap = new ServerBootstrap())
                .group(getEventLoopGroup())
                .channel(LocalServerChannel.class)
                .handler(getLogHandler())
                .childHandler(getServerChannelInitializer());
    }

    protected void initBootstrap() {
        (bootstrap = new Bootstrap())
                .group(getEventLoopGroup())
                .channel(LocalChannel.class)
                .handler(getChannelInitializer());
    }

    @Override
    protected ChannelFuture createInitFuture() {
        return serverBootstrap.bind(getAddress());
    }

    @Override
    protected EventLoopGroup createEventGroup() {
        return new DefaultEventLoopGroup();
    }

    public LocalPeer(String localAddress) {
        super(new LocalAddress(localAddress));
    }

    public ChannelFuture connect(String localAddress) {
        return bootstrap.connect(new LocalAddress(localAddress));
    }
}
