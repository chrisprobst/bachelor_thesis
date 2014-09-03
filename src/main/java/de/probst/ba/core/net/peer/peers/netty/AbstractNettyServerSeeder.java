package de.probst.ba.core.net.peer.peers.netty;

import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.handler.SeederHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;

import java.util.Optional;

/**
 * Created by chrisprobst on 01.09.14.
 */
public abstract class AbstractNettyServerSeeder extends AbstractNettySeeder {

    private ServerBootstrap seederBootstrap;

    @Override
    protected void initSeederBootstrap() {
        (seederBootstrap = new ServerBootstrap())
                .group(getSeederEventLoopGroup())
                .channel(getSeederChannelClass())
                .childHandler(getSeederChannelInitializer());
    }

    @Override
    protected ChannelFuture createSeederInitFuture() {
        return seederBootstrap.bind(getPeerId().getAddress());
    }

    protected abstract Class<? extends ServerChannel> getSeederChannelClass();

    protected AbstractNettyServerSeeder(long maxUploadRate,
                                        PeerId peerId,
                                        DataBase dataBase,
                                        SeederDistributionAlgorithm seederDistributionAlgorithm,
                                        Optional<SeederHandler> seederHandler,
                                        EventLoopGroup seederEventLoopGroup) {
        super(maxUploadRate, peerId, dataBase, seederDistributionAlgorithm, seederHandler, seederEventLoopGroup);
    }
}
