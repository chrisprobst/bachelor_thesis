package de.probst.ba.cli;

import com.beust.jcommander.Parameter;
import de.probst.ba.core.distribution.algorithms.Algorithms;
import de.probst.ba.core.media.database.databases.DataBases;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.handler.LeecherHandlerList;
import de.probst.ba.core.net.peer.handler.handlers.DataInfoCompletionHandler;
import de.probst.ba.core.net.peer.peers.Peers;
import de.probst.ba.core.net.peer.peers.netty.NettyConfig;
import de.probst.ba.core.util.collections.Tuple2;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.nio.NioEventLoopGroup;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
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
    private final Queue<Leecher> leecherQueue = new ConcurrentLinkedQueue<>();
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
        logger.info(">>> Seeders:                   " + seeders);
        logger.info(">>> Leechers:                  " + leechers);

        Path dbPath = Paths.get("/Users/chrisprobst/Desktop/databases");

        // Setup all seeders
        for (int i = 0; i < seeders; i++) {
            Seeder seeder = Peers.seeder(peerType,
                                         superUploadRate,
                                         downloadRate,
                                         getSeederSocketAddress(i),
                                         DataBases.fileDataBase(dbPath.resolve("db_seeder_" + i)),
                                         getSuperSeederDistributionAlgorithm(),
                                         Optional.ofNullable(recordPeerHandler),
                                         Optional.of(eventLoopGroup)).getInitFuture().get();

            superSeederUploadBandwidthStatisticPeers.add(seeder);
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

            if (algorithmType != Algorithms.AlgorithmType.Sequential) {
                Tuple2<Seeder, Leecher> tuple = Peers.initSeederAndLeecher(peerType,
                                                                           uploadRate,
                                                                           downloadRate,
                                                                           getLeecherSocketAddress(i),
                                                                           DataBases.fileDataBase(dbPath.resolve(
                                                                                   "db_leecher_" + i)),
                                                                           getSeederDistributionAlgorithm(),
                                                                           getLeecherDistributionAlgorithm(),
                                                                           Optional.ofNullable(recordPeerHandler),
                                                                           Optional.of(leecherHandlerList),
                                                                           true,
                                                                           Optional.of(eventLoopGroup)).get();
                Seeder seeder = tuple.first();
                Leecher leecher = tuple.second();

                chunkCompletionStatisticPeers.add(leecher);
                downloadBandwidthStatisticPeers.add(leecher);
                uploadBandwidthStatisticPeers.add(seeder);
                peerQueue.add(seeder);
                peerQueue.add(leecher);
                leecherQueue.add(leecher);

                seederQueue.stream()
                           .map(Peer::getPeerId)
                           .map(PeerId::getSocketAddress)
                           .map(Optional::get)
                           .map(leecher::connect)
                           .forEach(CompletableFuture::join);
            } else {
                Leecher leecher = Peers.leecher(peerType,
                                                uploadRate,
                                                downloadRate,
                                                Optional.empty(),
                                                DataBases.fileDataBase(dbPath.resolve("db_leecher_" + i)),
                                                getLeecherDistributionAlgorithm(),
                                                Optional.of(leecherHandlerList),
                                                true,
                                                Optional.of(eventLoopGroup),
                                                Optional.empty());

                chunkCompletionStatisticPeers.add(leecher);
                downloadBandwidthStatisticPeers.add(leecher);
                peerQueue.add(leecher);
                leecherQueue.add(leecher);

                seederQueue.stream()
                           .map(Peer::getPeerId)
                           .map(PeerId::getSocketAddress)
                           .map(Optional::get)
                           .map(leecher::connect)
                           .forEach(CompletableFuture::join);
            }
        }
    }

    private void logTransferInfo() {
        logger.info(">>> [ Transfer info ]");
        if (uploadRate > 0) {
            // Setup status flags
            double timePerTransfer = totalSize / (double) uploadRate;

            // Server/Client like
            double dumbTime = timePerTransfer / seeders * leechers;

            // ~ who knows ?!
            double chunkSwarmTime = timePerTransfer + (leechers - 1) / (double) chunkCount * timePerTransfer;

            // log(n) - log(s) = log(n / s) = log((s + l) / s) = log(1 + l/s)
            double logarithmicTime =
                    timePerTransfer * Math.ceil(Math.log(1 + leechers / (double) seeders) / Math.log(2));

            // A small info for all waiters
            logger.info(">>> One transfer needs approx.:                            " + timePerTransfer + " seconds");
            logger.info(">>> A Sequential algorithm needs approx.:                  " + dumbTime + " seconds");
            logger.info(">>> A Logarithmic algorithm needs approx.:                 " + logarithmicTime + " seconds");
            logger.info(">>> A (SuperSeeder)ChunkedSwarm algorithm needs approx.:   " + chunkSwarmTime + " seconds");
        } else {
            logger.info(">>> Cannot estimate time because upload rate is infinity (0)");
        }
    }

    private void waitForConnections() throws InterruptedException {

        // The expected number of connections
        int limit = NettyConfig.getMaxConnectionsPerLeecher();
        limit = limit < 1 ? Integer.MAX_VALUE : limit;
        int expectedConnections = algorithmType == Algorithms.AlgorithmType.Sequential ?
                                  leechers * seeders :
                                  leechers * (Math.min(leechers - 1 + seeders, limit));
        logger.info(">>> [ Waiting for " + expectedConnections + " connections ]");
        while (true) {
            long activeConnections = leecherQueue.stream()
                                                 .map(Leecher::getConnections)
                                                 .map(Map::entrySet)
                                                 .flatMap(Set::stream)
                                                 .filter(Map.Entry::getValue)
                                                 .count();

            if (activeConnections != expectedConnections) {
                logger.info(">>> Found: " + activeConnections);
                Thread.sleep(500);
            } else {
                break;
            }
        }
        logger.info(">>> [ All connections established ]");
    }

    @Override
    protected void start() throws Exception {
        setup();
        logTransferInfo();
        waitForConnections();

        setupStart(eventLoopGroup);
        dataInfoCompletionHandler.getCountDownLatch().await();
        Instant now = Instant.now();
        Thread.sleep(3000);
        setupStop(now);

        Peers.closeAndWait(peerQueue);
    }

    public static void main(String[] args) throws Exception {
        new Benchmark().parse(args);
    }
}

