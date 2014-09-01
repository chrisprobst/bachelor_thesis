package de.probst.ba.core.net.peer.peers.netty;

import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.net.peer.LeecherHandler;
import de.probst.ba.core.net.peer.PeerId;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.Optional;

/**
 * Created by chrisprobst on 01.09.14.
 */
public final class TcpNettyLeecher extends AbstractNettyClientLeecher {
    
    @Override
    protected Class<? extends Channel> getLeecherChannelClass() {
        return NioSocketChannel.class;
    }

    @Override
    protected EventLoopGroup createLeecherEventLoopGroup() {
        return new NioEventLoopGroup();
    }

    public TcpNettyLeecher(long downloadRate,
                           PeerId peerId,
                           DataBase dataBase,
                           LeecherDistributionAlgorithm distributionAlgorithm,
                           LeecherHandler peerHandler,
                           Optional<EventLoopGroup> leecherEventLoopGroup) {
        super(downloadRate, peerId, dataBase, distributionAlgorithm, peerHandler, leecherEventLoopGroup);
    }
}
