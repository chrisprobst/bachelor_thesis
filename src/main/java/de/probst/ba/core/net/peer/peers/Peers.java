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
import de.probst.ba.core.util.collections.Tuple;
import de.probst.ba.core.util.collections.Tuple2;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Created by chrisprobst on 17.08.14.
 */
public final class Peers {

    public enum PeerType {
        Local, TCP
    }

    private Peers() {
    }

    public static CompletableFuture<Tuple2<Seeder, Leecher>> initSeederAndLeecher(
            PeerType peerType,
            long maxUploadRate,
            long maxDownloadRate,
            SocketAddress listenSocketAddress,
            DataBase dataBase,
            SeederDistributionAlgorithm seederDistributionAlgorithm,
            LeecherDistributionAlgorithm leecherDistributionAlgorithm,
            Optional<SeederPeerHandler> seederHandler,
            Optional<LeecherPeerHandler> leecherHandler,
            boolean leecherAutoConnect,
            Optional<EventLoopGroup> eventLoopGroup) {

        return Peers.seeder(peerType,
                            maxUploadRate,
                            maxDownloadRate,
                            listenSocketAddress,
                            dataBase,
                            seederDistributionAlgorithm,
                            seederHandler,
                            eventLoopGroup)
                    .getInitFuture().thenCompose(seeder -> Peers.leecher(peerType,
                                                                         maxUploadRate,
                                                                         maxDownloadRate,
                                                                         Optional.of(seeder.getPeerId()),
                                                                         seeder.getDataBase(),
                                                                         leecherDistributionAlgorithm,
                                                                         leecherHandler,
                                                                         leecherAutoConnect,
                                                                         eventLoopGroup,
                                                                         seeder.getPeerId().getSocketAddress())
                                                                .getInitFuture()
                                                                .thenApply(leecher -> Tuple.of(seeder, leecher)));
    }

    public static Seeder seeder(PeerType peerType,
                                long maxUploadRate,
                                long maxDownloadRate,
                                SocketAddress socketAddress,
                                DataBase dataBase,
                                SeederDistributionAlgorithm seederDistributionAlgorithm,
                                Optional<SeederPeerHandler> seederHandler,
                                Optional<EventLoopGroup> seederEventLoopGroup) {
        return new NettyServerSeeder(maxUploadRate,
                                     maxDownloadRate,
                                     socketAddress,
                                     dataBase,
                                     seederDistributionAlgorithm,
                                     seederHandler,
                                     seederEventLoopGroup.orElseGet(NioEventLoopGroup::new),
                                     peerType == PeerType.TCP ?
                                     NioServerSocketChannel.class :
                                     LocalServerChannel.class);
    }

    public static Leecher leecher(PeerType peerType,
                                  long maxUploadRate,
                                  long maxDownloadRate,
                                  Optional<PeerId> peerId,
                                  DataBase dataBase,
                                  LeecherDistributionAlgorithm leecherDistributionAlgorithm,
                                  Optional<LeecherPeerHandler> leecherHandler,
                                  boolean autoConnect,
                                  Optional<EventLoopGroup> leecherEventLoopGroup,
                                  Optional<SocketAddress> announceSocketAddress) {
        return new NettyLeecher(maxUploadRate,
                                maxDownloadRate,
                                peerId,
                                dataBase,
                                leecherDistributionAlgorithm,
                                leecherHandler,
                                autoConnect,
                                leecherEventLoopGroup.orElseGet(NioEventLoopGroup::new),
                                peerType == PeerType.TCP ? NioSocketChannel.class : LocalChannel.class,
                                announceSocketAddress);
    }

    public static void waitForInit(Collection<Peer> peers) throws ExecutionException, InterruptedException {
        Objects.requireNonNull(peers);
        for (Peer peer : peers) {
            peer.getInitFuture().get();
        }
    }

    public static void closeAndWait(Collection<Peer> peers) throws IOException,
                                                                   ExecutionException,
                                                                   InterruptedException {
        close(peers);
        waitForClose(peers);
    }

    public static void close(Collection<Peer> peers) throws IOException {
        Objects.requireNonNull(peers);
        for (Peer peer : peers) {
            peer.close();
        }
    }

    public static void waitForClose(Collection<Peer> peers) throws ExecutionException, InterruptedException {
        Objects.requireNonNull(peers);
        for (Peer peer : peers) {
            peer.getCloseFuture().get();
        }
    }
}
