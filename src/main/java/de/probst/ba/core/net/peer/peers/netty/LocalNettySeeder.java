package de.probst.ba.core.net.peer.peers.netty;

import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.SeederHandler;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.local.LocalServerChannel;

import java.util.Optional;

/**
 * Created by chrisprobst on 12.08.14.
 */
public final class LocalNettySeeder extends AbstractNettyServerSeeder {

    @Override
    protected Class<? extends ServerChannel> getSeederChannelClass() {
        return LocalServerChannel.class;
    }

    @Override
    protected EventLoopGroup createSeederEventLoopGroup() {
        return new DefaultEventLoopGroup();
    }

    public LocalNettySeeder(long uploadRate,
                            PeerId peerId,
                            DataBase dataBase,
                            SeederDistributionAlgorithm distributionAlgorithm,
                            SeederHandler peerHandler,
                            Optional<EventLoopGroup> seederEventLoopGroup) {
        super(uploadRate, peerId, dataBase, distributionAlgorithm, peerHandler, seederEventLoopGroup);
    }
}
