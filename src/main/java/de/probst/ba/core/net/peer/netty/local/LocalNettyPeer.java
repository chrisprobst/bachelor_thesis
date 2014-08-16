package de.probst.ba.core.net.peer.netty.local;

import de.probst.ba.core.logic.Brain;
import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.net.peer.netty.AbstractNettyPeer;
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
public class LocalNettyPeer extends AbstractNettyPeer {

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

    public LocalNettyPeer(long uploadRate,
                          long downloadRate,
                          String localAddress,
                          DataBase dataBase,
                          Brain brain) {
        super(uploadRate, downloadRate, new LocalAddress(localAddress), dataBase, brain);
    }

    public ChannelFuture connect(String localAddress) {
        return bootstrap.connect(new LocalAddress(localAddress));
    }
}
