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
public final class NettyServerSeeder extends AbstractNettySeeder {

    @Override
    protected ChannelFuture initSeederBootstrap() {
        return new ServerBootstrap().group(getSeederEventLoopGroup())
                                    .channel(getSeederChannelClass())
                                    .childHandler(getSeederChannelInitializer())
                                    .bind(getPeerId().getAddress().get());
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Class<? extends ServerChannel> getSeederChannelClass() {
        return (Class<? extends ServerChannel>) super.getSeederChannelClass();
    }

    public NettyServerSeeder(long maxUploadRate,
                             long maxDownloadRate,
                             PeerId peerId,
                             DataBase dataBase,
                             SeederDistributionAlgorithm seederDistributionAlgorithm,
                             Optional<SeederPeerHandler> seederHandler,
                             EventLoopGroup seederEventLoopGroup,
                             Class<? extends ServerChannel> seederChannelClass) {
        super(maxUploadRate,
              maxDownloadRate,
              peerId,
              dataBase,
              seederDistributionAlgorithm,
              seederHandler,
              seederEventLoopGroup,
              seederChannelClass);
    }
}
