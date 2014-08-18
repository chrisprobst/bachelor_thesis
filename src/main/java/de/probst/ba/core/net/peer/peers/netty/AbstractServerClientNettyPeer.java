package de.probst.ba.core.net.peer.peers.netty;

import de.probst.ba.core.diag.Diagnostic;
import de.probst.ba.core.logic.Brain;
import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.net.peer.PeerId;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;

import java.net.SocketAddress;
import java.util.Optional;

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
        return serverBootstrap.bind(getLocalPeerId().getAddress());
    }

    protected abstract Class<? extends Channel> getChannelClass();

    protected abstract Class<? extends ServerChannel> getServerChannelClass();

    protected AbstractServerClientNettyPeer(long uploadRate,
                                            long downloadRate,
                                            PeerId localPeerId,
                                            DataBase dataBase,
                                            Brain brain,
                                            Diagnostic diagnostic,
                                            Optional<EventLoopGroup> eventLoopGroup) {
        super(uploadRate, downloadRate, localPeerId, dataBase, brain, diagnostic, eventLoopGroup);
    }

    @Override
    public void connect(SocketAddress socketAddress) {
        bootstrap.connect(socketAddress);
    }
}
