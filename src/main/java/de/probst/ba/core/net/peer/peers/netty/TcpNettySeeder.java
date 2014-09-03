package de.probst.ba.core.net.peer.peers.netty;

import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.handler.SeederHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.Optional;

/**
 * Created by chrisprobst on 01.09.14.
 */
public final class TcpNettySeeder extends AbstractNettyServerSeeder {

    @Override
    protected Class<? extends ServerChannel> getSeederChannelClass() {
        return NioServerSocketChannel.class;
    }

    public TcpNettySeeder(long maxUploadRate,
                          PeerId peerId,
                          DataBase dataBase,
                          SeederDistributionAlgorithm seederDistributionAlgorithm,
                          Optional<SeederHandler> seederHandler,
                          EventLoopGroup seederEventLoopGroup) {
        super(maxUploadRate, peerId, dataBase, seederDistributionAlgorithm, seederHandler, seederEventLoopGroup);
    }
}
