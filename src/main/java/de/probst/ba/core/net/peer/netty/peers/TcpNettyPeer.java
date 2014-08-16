package de.probst.ba.core.net.peer.netty.peers;

import de.probst.ba.core.logic.Brain;
import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.net.peer.netty.AbstractNettyPeer;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.SocketAddress;

/**
 * Created by chrisprobst on 16.08.14.
 */
public class TcpNettyPeer extends AbstractNettyPeer {

    private ServerBootstrap serverBootstrap;

    private Bootstrap bootstrap;

    public TcpNettyPeer(long uploadRate,
                        long downloadRate,
                        SocketAddress address,
                        DataBase dataBase,
                        Brain brain) {
        super(uploadRate, downloadRate, address, dataBase, brain);
    }

    protected void initServerBootstrap() {
        (serverBootstrap = new ServerBootstrap())
                .group(getEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                .handler(getLogHandler())
                .childHandler(getServerChannelInitializer());
    }

    protected void initBootstrap() {
        (bootstrap = new Bootstrap())
                .group(getEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(getChannelInitializer());
    }

    @Override
    protected EventLoopGroup createEventGroup() {
        return new NioEventLoopGroup();
    }

    @Override
    protected ChannelFuture createInitFuture() {
        return serverBootstrap.bind(getAddress());
    }

    public ChannelFuture connect(String host, int port) {
        return bootstrap.connect(host, port);
    }
}
