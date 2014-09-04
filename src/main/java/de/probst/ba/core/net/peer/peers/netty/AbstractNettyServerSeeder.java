package de.probst.ba.core.net.peer.peers.netty;

import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.handler.SeederPeerHandler;
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

    protected AbstractNettyServerSeeder(long maxUploadRate,
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
    protected void initSeederBootstrap() {
        (seederBootstrap = new ServerBootstrap()).group(getSeederEventLoopGroup())
                                                 .channel(getSeederChannelClass())
                                                 .childHandler(getSeederChannelInitializer());
    }

    @Override
    protected ChannelFuture createSeederInitFuture() {
        return seederBootstrap.bind(getPeerId().getAddress());
    }

    protected abstract Class<? extends ServerChannel> getSeederChannelClass();
}
