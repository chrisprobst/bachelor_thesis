package de.probst.ba.core.net.peer.peers;

import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.handler.LeecherPeerHandler;
import de.probst.ba.core.net.peer.handler.SeederPeerHandler;
import de.probst.ba.core.net.peer.peers.netty.NettyLeecher;
import de.probst.ba.core.net.peer.peers.netty.NettyServerSeeder;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Created by chrisprobst on 17.08.14.
 */
public final class Peers {

    private Peers() {
    }

    public static Seeder seeder(Type type,
                                long maxUploadRate,
                                long maxDownloadRate,
                                PeerId peerId,
                                DataBase dataBase,
                                SeederDistributionAlgorithm seederDistributionAlgorithm,
                                Optional<SeederPeerHandler> seederHandler,
                                Optional<EventLoopGroup> seederEventLoopGroup) {
        return new NettyServerSeeder(maxUploadRate,
                                     maxDownloadRate,
                                     peerId,
                                     dataBase,
                                     seederDistributionAlgorithm,
                                     seederHandler,
                                     seederEventLoopGroup.orElseGet(NioEventLoopGroup::new),
                                     type == Type.TCP ? NioServerSocketChannel.class : LocalServerChannel.class);
    }

    public static Leecher leecher(Type type,
                                  long maxUploadRate,
                                  long maxDownloadRate,
                                  PeerId peerId,
                                  DataBase dataBase,
                                  LeecherDistributionAlgorithm leecherDistributionAlgorithm,
                                  Optional<LeecherPeerHandler> leecherHandler,
                                  boolean autoConnect,
                                  Optional<EventLoopGroup> leecherEventLoopGroup,
                                  Optional<PeerId> announcePeerId) {
        return new NettyLeecher(maxUploadRate,
                                maxDownloadRate,
                                peerId,
                                dataBase,
                                leecherDistributionAlgorithm,
                                leecherHandler,
                                autoConnect,
                                leecherEventLoopGroup.orElseGet(NioEventLoopGroup::new),
                                type == Type.TCP ? SocketChannel.class : LocalChannel.class,
                                announcePeerId);
    }

    public static void waitForInit(Collection<Peer> peers) throws ExecutionException, InterruptedException {
        Objects.requireNonNull(peers);
        for (Peer peer : peers) {
            peer.getInitFuture().get();
        }
    }

    public static void closeAndWait(Collection<Peer> peers)
            throws ExecutionException, InterruptedException, IOException {
        Objects.requireNonNull(peers);
        for (Peer peer : peers) {
            peer.close();
        }

        waitForClose(peers);
    }

    public static void waitForClose(Collection<Peer> peers) throws ExecutionException, InterruptedException {
        Objects.requireNonNull(peers);
        for (Peer peer : peers) {
            peer.getCloseFuture().get();
        }
    }

    public static void connectTo(Collection<Peer> leechers, PeerId peerId) {
        Objects.requireNonNull(leechers);
        leechers.forEach(l -> ((Leecher) l).connect(peerId));
    }

    public enum Type {
        Local, TCP
    }
}
