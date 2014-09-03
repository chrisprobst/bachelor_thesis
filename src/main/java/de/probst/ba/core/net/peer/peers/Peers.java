package de.probst.ba.core.net.peer.peers;

import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.handler.LeecherHandler;
import de.probst.ba.core.net.peer.handler.SeederHandler;
import de.probst.ba.core.net.peer.peers.netty.LocalNettyLeecher;
import de.probst.ba.core.net.peer.peers.netty.LocalNettySeeder;
import de.probst.ba.core.net.peer.peers.netty.TcpNettyLeecher;
import de.probst.ba.core.net.peer.peers.netty.TcpNettySeeder;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
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

    public static Seeder seeder(PeerType peerType,
                                long maxUploadRate,
                                PeerId peerId,
                                DataBase dataBase,
                                SeederDistributionAlgorithm seederDistributionAlgorithm,
                                Optional<SeederHandler> seederHandler,
                                Optional<EventLoopGroup> seederEventLoopGroup) {
        return peerType == PeerType.TCP ?
                new TcpNettySeeder(maxUploadRate, peerId, dataBase,
                        seederDistributionAlgorithm, seederHandler,
                        seederEventLoopGroup.orElseGet(NioEventLoopGroup::new)) :
                new LocalNettySeeder(maxUploadRate, peerId, dataBase,
                        seederDistributionAlgorithm, seederHandler,
                        seederEventLoopGroup.orElseGet(DefaultEventLoopGroup::new));
    }

    public static Leecher leecher(PeerType peerType,
                                  long maxDownloadRate,
                                  PeerId peerId,
                                  DataBase dataBase,
                                  LeecherDistributionAlgorithm leecherDistributionAlgorithm,
                                  Optional<LeecherHandler> leecherHandler,
                                  Optional<EventLoopGroup> leecherEventLoopGroup) {
        return peerType == PeerType.TCP ?
                new TcpNettyLeecher(maxDownloadRate, peerId, dataBase,
                        leecherDistributionAlgorithm, leecherHandler,
                        leecherEventLoopGroup.orElseGet(NioEventLoopGroup::new)) :
                new LocalNettyLeecher(maxDownloadRate, peerId, dataBase,
                        leecherDistributionAlgorithm, leecherHandler,
                        leecherEventLoopGroup.orElseGet(DefaultEventLoopGroup::new));
    }

    public static void waitForInit(Collection<Peer> peers) throws ExecutionException, InterruptedException {
        Objects.requireNonNull(peers);
        for (Peer peer : peers) {
            peer.getInitFuture().get();
        }
    }

    public static void closeAndWait(Collection<Peer> peers) throws ExecutionException, InterruptedException, IOException {
        Objects.requireNonNull(peers);
        for (Peer peer : peers) {
            peer.close();
        }

        for (Peer peer : peers) {
            peer.getCloseFuture().get();
        }
    }

    public static void waitForClose(Collection<Peer> peers) throws ExecutionException, InterruptedException {
        Objects.requireNonNull(peers);
        for (Peer peer : peers) {
            peer.getCloseFuture().get();
        }
    }

    public static void connectGrid(Collection<Peer> peers) {
        Objects.requireNonNull(peers);
        peers.stream()
                .filter(leecher -> leecher instanceof Leecher)
                .forEach(leecher -> peers.stream()
                        .filter(seeder -> seeder instanceof Seeder)
                        .filter(seeder -> seeder != leecher)
                        .filter(seeder -> !seeder.getPeerId().equals(leecher.getPeerId()))
                        .forEach(seeder -> ((Leecher) leecher).connect(seeder.getPeerId().getAddress())));
    }
}
