package de.probst.ba.core.net.peer.peers.netty;

import de.probst.ba.core.logic.Brain;
import de.probst.ba.core.media.DataBase;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ServerChannel;

import java.net.SocketAddress;

/**
 * Created by chrisprobst on 17.08.14.
 */
abstract class AbstractServerClientNettyPeer extends AbstractNettyPeer {

    private ServerBootstrap serverBootstrap;

    private Bootstrap bootstrap;


    protected void initServerBootstrap() {
        (serverBootstrap = new ServerBootstrap())
                .group(getEventLoopGroup())
                .channel(getServerChannelClass())
                .handler(getLogHandler())
                .childHandler(getServerChannelInitializer());
    }

    protected void initBootstrap() {
        (bootstrap = new Bootstrap())
                .group(getEventLoopGroup())
                .channel(getChannelClass())
                .handler(getChannelInitializer());
    }

    @Override
    protected ChannelFuture createInitFuture() {
        return serverBootstrap.bind(getLocalAddress());
    }

    protected abstract Class<? extends Channel> getChannelClass();

    protected abstract Class<? extends ServerChannel> getServerChannelClass();

    protected AbstractServerClientNettyPeer(long uploadRate,
                                            long downloadRate,
                                            SocketAddress localAddress,
                                            DataBase dataBase,
                                            Brain brain) {
        super(uploadRate, downloadRate, localAddress, dataBase, brain);
    }

    @Override
    public void connect(SocketAddress socketAddress) {
        bootstrap.connect(socketAddress);
    }
}
