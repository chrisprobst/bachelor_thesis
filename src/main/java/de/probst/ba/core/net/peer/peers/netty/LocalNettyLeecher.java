package de.probst.ba.core.net.peer.peers.netty;

import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.handler.LeecherPeerHandler;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalChannel;

import java.util.Optional;

/**
 * Created by chrisprobst on 12.08.14.
 */
public final class LocalNettyLeecher extends AbstractNettyClientLeecher {

    public LocalNettyLeecher(long maxUploadRate,
                             long maxDownloadRate,
                             PeerId peerId,
                             DataBase dataBase,
                             LeecherDistributionAlgorithm leecherDistributionAlgorithm,
                             Optional<LeecherPeerHandler> leecherHandler,
                             EventLoopGroup leecherEventLoopGroup) {
        super(maxUploadRate,
              maxDownloadRate,
              peerId,
              dataBase,
              leecherDistributionAlgorithm,
              leecherHandler,
              leecherEventLoopGroup);
    }

    @Override
    protected Class<? extends Channel> getLeecherChannelClass() {
        return LocalChannel.class;
    }
}
