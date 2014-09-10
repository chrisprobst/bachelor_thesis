package de.probst.ba.core.app;

import com.beust.jcommander.Parameter;
import de.probst.ba.core.media.database.databases.DataBases;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.handler.LeecherHandlerList;
import de.probst.ba.core.net.peer.handler.handlers.DataInfoCompletionHandler;
import de.probst.ba.core.net.peer.peers.Peers;
import de.probst.ba.core.util.collections.Tuple2;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.nio.NioEventLoopGroup;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by chrisprobst on 12.08.14.
 */
public class Benchmark extends AbstractPeerApp {

    @Parameter(names = {"-s", "--seeders"},
               description = "Number of seeders (" + PeerCountValidator.MSG + ")",
               validateValueWith = PeerCountValidator.class)
    private Integer seeders = 1;
    @Parameter(names = {"-l", "--leechers"},
               description = "Number of leechers (" + PeerCountValidator.MSG + ")",
               validateValueWith = PeerCountValidator.class)
    private Integer leechers = 7;

    private final Queue<Peer> peerQueue = new ConcurrentLinkedQueue<>();
    private final Queue<Peer> seederQueue = new ConcurrentLinkedQueue<>();
    private final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
    private DataInfoCompletionHandler dataInfoCompletionHandler;

    private SocketAddress getSeederSocketAddress(int i) {
        if (peerType == Peers.PeerType.TCP) {
            return new InetSocketAddress("127.0.0.1", 10000 + i);
        } else {
            return new LocalAddress("S-" + i);
        }
    }

    private SocketAddress getLeecherSocketAddress(int i) {
        if (peerType == Peers.PeerType.TCP) {
            return new InetSocketAddress("127.0.0.1", 20000 + i);
        } else {
            return new LocalAddress("L-" + i);
        }
    }

    @Override
    protected void setupPeerHandlers() {
        dataInfoCompletionHandler = new DataInfoCompletionHandler(leechers * parts);
    }

    @Override
    protected void setupPeers() throws Exception {
        // Setup all seeders
        for (int i = 0; i < seeders; i++) {
            Seeder seeder = Peers.seeder(peerType,
                                         uploadRate,
                                         downloadRate,
                                         getSeederSocketAddress(i),
                                         DataBases.fakeDataBase(),
                                         getSeederDistributionAlgorithm(),
                                         Optional.ofNullable(recordPeerHandler),
                                         Optional.of(eventLoopGroup)).getInitFuture().get();

            uploadBandwidthStatisticPeers.add(seeder);
            dataBaseUpdatePeers.add(seeder);
            seederQueue.add(seeder);
            peerQueue.add(seeder);
        }

        // Setup all leechers
        for (int i = 0; i < leechers; i++) {
            LeecherHandlerList leecherHandlerList = new LeecherHandlerList();
            leecherHandlerList.add(dataInfoCompletionHandler);
            if (recordPeerHandler != null) {
                leecherHandlerList.add(recordPeerHandler);
            }
            Tuple2<Seeder, Leecher> tuple = Peers.initSeederAndLeecher(peerType,
                                                                       uploadRate,
                                                                       downloadRate,
                                                                       getLeecherSocketAddress(i),
                                                                       DataBases.fakeDataBase(),
                                                                       getSeederDistributionAlgorithm(),
                                                                       getLeecherDistributionAlgorithm(),
                                                                       Optional.ofNullable(recordPeerHandler),
                                                                       Optional.of(leecherHandlerList),
                                                                       true,
                                                                       Optional.of(eventLoopGroup)).get();
            Seeder seeder = tuple.first();
            Leecher leecher = tuple.second();

            downloadBandwidthStatisticPeers.add(leecher);
            uploadBandwidthStatisticPeers.add(seeder);
            peerQueue.add(seeder);
            peerQueue.add(leecher);

            seederQueue.stream()
                       .map(Peer::getPeerId)
                       .map(PeerId::getSocketAddress)
                       .map(Optional::get)
                       .map(leecher::connect)
                       .forEach(CompletableFuture::join);

        }
    }

    private void logTransferInfo() {
        // Setup status flags
        double timePerTransfer = totalSize / uploadRate;

        // Server/Client like
        double dumbTime = timePerTransfer / seeders * leechers;

        // ~ who knows ?!
        double chunkSwarmTime = timePerTransfer / seeders * 1.3;

        // log(n) - log(s) = log(n / s) = log((s + l) / s) = log(1 + l/s)
        double logarithmicTime =
                timePerTransfer * Math.ceil(Math.log(1 + leechers / (double) seeders) / Math.log(2));

        // A small info for all waiters
        logger.info("[== One transfer needs approx.: " + timePerTransfer + " seconds ==]");
        logger.info("[== A dumb algorithm needs approx.: " + dumbTime + " seconds ==]");
        logger.info("[== A Logarithmic algorithm needs approx.: " + logarithmicTime + " seconds ==]");
        logger.info("[== A ChunkedSwarm algorithm needs approx.: " + chunkSwarmTime + " seconds ==]");
    }

    @Override
    protected void start() throws Exception {
        setup();
        logTransferInfo();

        Thread.sleep(2000);

        Scanner scanner = new Scanner(System.in);
        logger.info("[== Press [ENTER] to start benchmark ==]");
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        } else {
            return;
        }

        setupStart(eventLoopGroup);
        dataInfoCompletionHandler.getCountDownLatch().await();
        setupStop();

        Peers.closeAndWait(peerQueue);
    }

    public static void main(String[] args) throws Exception {
        new Benchmark().parse(args);
    }
}
