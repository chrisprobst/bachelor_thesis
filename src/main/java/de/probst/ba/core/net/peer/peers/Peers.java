package de.probst.ba.core.net.peer.peers;

import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.LeecherHandler;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.SeederHandler;
import de.probst.ba.core.net.peer.peers.netty.LocalNettyLeecher;
import de.probst.ba.core.net.peer.peers.netty.LocalNettySeeder;
import de.probst.ba.core.net.peer.peers.netty.TcpNettyLeecher;
import de.probst.ba.core.net.peer.peers.netty.TcpNettySeeder;
import io.netty.channel.EventLoopGroup;

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
                                long uploadRate,
                                PeerId peerId,
                                DataBase dataBase,
                                SeederDistributionAlgorithm distributionAlgorithm,
                                SeederHandler peerHandler,
                                Optional<EventLoopGroup> seederEventLoopGroup) {
        return peerType == PeerType.TCP ?
                new TcpNettySeeder(uploadRate, peerId, dataBase,
                        distributionAlgorithm, peerHandler, seederEventLoopGroup) :
                new LocalNettySeeder(uploadRate, peerId, dataBase,
                        distributionAlgorithm, peerHandler, seederEventLoopGroup);
    }

    public static Leecher leecher(PeerType peerType,
                                  long downloadRate,
                                  PeerId peerId,
                                  DataBase dataBase,
                                  LeecherDistributionAlgorithm distributionAlgorithm,
                                  LeecherHandler peerHandler,
                                  Optional<EventLoopGroup> leecherEventLoopGroup) {
        return peerType == PeerType.TCP ?
                new TcpNettyLeecher(downloadRate, peerId, dataBase,
                        distributionAlgorithm, peerHandler, leecherEventLoopGroup) :
                new LocalNettyLeecher(downloadRate, peerId, dataBase,
                        distributionAlgorithm, peerHandler, leecherEventLoopGroup);
    }

    public static Seeder tcpSeeder(long uploadRate,
                                   PeerId peerId,
                                   DataBase dataBase,
                                   SeederDistributionAlgorithm distributionAlgorithm,
                                   SeederHandler peerHandler,
                                   Optional<EventLoopGroup> seederEventLoopGroup) {
        return new TcpNettySeeder(uploadRate, peerId, dataBase,
                distributionAlgorithm, peerHandler, seederEventLoopGroup);
    }

    public static Leecher tcpLeecher(long downloadRate,
                                     PeerId peerId,
                                     DataBase dataBase,
                                     LeecherDistributionAlgorithm distributionAlgorithm,
                                     LeecherHandler peerHandler,
                                     Optional<EventLoopGroup> leecherEventLoopGroup) {
        return new TcpNettyLeecher(downloadRate, peerId, dataBase,
                distributionAlgorithm, peerHandler, leecherEventLoopGroup);
    }


    public static Seeder localSeeder(long uploadRate,
                                     PeerId peerId,
                                     DataBase dataBase,
                                     SeederDistributionAlgorithm distributionAlgorithm,
                                     SeederHandler peerHandler,
                                     Optional<EventLoopGroup> seederEventLoopGroup) {
        return new LocalNettySeeder(uploadRate, peerId, dataBase,
                distributionAlgorithm, peerHandler, seederEventLoopGroup);
    }

    public static Leecher localLeecher(long downloadRate,
                                       PeerId peerId,
                                       DataBase dataBase,
                                       LeecherDistributionAlgorithm distributionAlgorithm,
                                       LeecherHandler peerHandler,
                                       Optional<EventLoopGroup> leecherEventLoopGroup) {
        return new LocalNettyLeecher(downloadRate, peerId, dataBase,
                distributionAlgorithm, peerHandler, leecherEventLoopGroup);
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
