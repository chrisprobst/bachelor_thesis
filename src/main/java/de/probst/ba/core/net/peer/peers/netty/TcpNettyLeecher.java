package de.probst.ba.core.net.peer.peers.netty;

import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.handler.LeecherHandler;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
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

    public TcpNettyLeecher(long maxUploadRate,
                           long maxDownloadRate,
                           PeerId peerId,
                           DataBase dataBase,
                           LeecherDistributionAlgorithm leecherDistributionAlgorithm,
                           Optional<LeecherHandler> leecherHandler,
                           EventLoopGroup leecherEventLoopGroup) {
        super(maxUploadRate, maxDownloadRate, peerId, dataBase, leecherDistributionAlgorithm, leecherHandler, leecherEventLoopGroup);
    }
}
