package de.probst.ba.core.net.peer.peers.netty;

import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.net.peer.LeecherHandler;
import de.probst.ba.core.net.peer.PeerId;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;

import java.net.SocketAddress;
import java.util.Optional;

/**
 * Created by chrisprobst on 01.09.14.
 */
public abstract class AbstractNettyClientLeecher extends AbstractNettyLeecher {

    private Bootstrap leecherBootstrap;

    @Override
    protected void initLeecherBootstrap() {
        (leecherBootstrap = new Bootstrap())
                .group(getLeecherEventLoopGroup())
                .channel(getLeecherChannelClass())
                .handler(getLeecherChannelInitializer());
    }

    protected abstract Class<? extends Channel> getLeecherChannelClass();

    protected AbstractNettyClientLeecher(long downloadRate,
                                         PeerId peerId,
                                         DataBase dataBase,
                                         LeecherDistributionAlgorithm distributionAlgorithm,
                                         LeecherHandler peerHandler,
                                         Optional<EventLoopGroup> leecherEventLoopGroup) {
        super(downloadRate, peerId, dataBase, distributionAlgorithm, peerHandler, leecherEventLoopGroup);
    }

    @Override
    public void connect(SocketAddress socketAddress) {
        leecherBootstrap.connect(socketAddress);
    }
}