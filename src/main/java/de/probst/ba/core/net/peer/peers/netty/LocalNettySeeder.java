package de.probst.ba.core.net.peer.peers.netty;

import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.handler.SeederPeerHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.local.LocalServerChannel;

import java.util.Optional;

/**
 * Created by chrisprobst on 12.08.14.
 */
public final class LocalNettySeeder extends AbstractNettyServerSeeder {

    public LocalNettySeeder(long maxUploadRate,
                            long maxDownloadRate,
                            PeerId peerId,
                            DataBase dataBase,
                            SeederDistributionAlgorithm seederDistributionAlgorithm,
                            Optional<SeederPeerHandler> seederHandler,
                            EventLoopGroup seederEventLoopGroup) {
        super(maxUploadRate,
              maxDownloadRate,
              peerId,
              dataBase,
              seederDistributionAlgorithm,
              seederHandler,
              seederEventLoopGroup);
    }

    @Override
    protected Class<? extends ServerChannel> getSeederChannelClass() {
        return LocalServerChannel.class;
    }
}
