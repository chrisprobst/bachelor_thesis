package de.probst.ba.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParametersDelegate;
import de.probst.ba.cli.args.ArgsApp;
import de.probst.ba.cli.args.BandwidthArgs;
import de.probst.ba.cli.args.DataInfoGeneratorArgs;
import de.probst.ba.cli.args.HelpArgs;
import de.probst.ba.cli.args.NetworkArgs;
import de.probst.ba.cli.args.PeerCountArgs;
import de.probst.ba.cli.args.StatisticArgs;
import de.probst.ba.cli.args.SuperSeederBandwidthArgs;
import de.probst.ba.core.distribution.algorithms.Algorithms;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.database.databases.DataBases;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.handler.LeecherHandlerList;
import de.probst.ba.core.net.peer.handler.handlers.DataInfoCompletionHandler;
import de.probst.ba.core.net.peer.peers.Peers;
import de.probst.ba.core.net.peer.peers.netty.NettyConfig;
import de.probst.ba.core.net.peer.statistic.StatisticsManager;
import de.probst.ba.core.util.collections.Tuple2;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

/**
 * Created by chrisprobst on 12.08.14.
 */
public class Benchmark extends ArgsApp {

    // The benchmark logger
    private final Logger logger = LoggerFactory.getLogger(Benchmark.class);

    // The event loop group of the benchmark
    private final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    // The statistics manager of the benchmark
    private final StatisticsManager statisticsManager = new StatisticsManager(eventLoopGroup);

    private CountDownLatch completionCountDownLatch;

    // The argument delegates
    @ParametersDelegate
    private final HelpArgs helpArgs = new HelpArgs();
    @ParametersDelegate
    private final PeerCountArgs peerCountArgs = new PeerCountArgs();
    @ParametersDelegate
    private final NetworkArgs networkArgs = new NetworkArgs();
    @ParametersDelegate
    private final BandwidthArgs bandwidthArgs = new BandwidthArgs();
    @ParametersDelegate
    private final SuperSeederBandwidthArgs superSeederBandwidthArgs = new SuperSeederBandwidthArgs();
    @ParametersDelegate
    private final DataInfoGeneratorArgs dataInfoGeneratorArgs = new DataInfoGeneratorArgs();
    @ParametersDelegate
    private final StatisticArgs statisticArgs = new StatisticArgs();

    private void logTransferInfo() {
        logger.info(">>> [ Transfer info ]");
        if (bandwidthArgs.maxUploadRate.equals(superSeederBandwidthArgs.maxSuperSeederUploadRate) &&
            bandwidthArgs.maxUploadRate > 0) {
            // Setup status flags
            double timePerTransfer = dataInfoGeneratorArgs.size / (double) bandwidthArgs.maxUploadRate;

            // Server/Client like
            double dumbTime = timePerTransfer / peerCountArgs.superSeeders * peerCountArgs.seederLeecherCouples;

            // ~ who knows ?!
            double chunkSwarmTime = timePerTransfer + (peerCountArgs.seederLeecherCouples - 1) /
                                                      (double) dataInfoGeneratorArgs.chunkCount * timePerTransfer;

            // log(n) - log(s) = log(n / s) = log((s + l) / s) = log(1 + l/s)
            double logarithmicTime =
                    timePerTransfer * Math.ceil(
                            Math.log(1 + peerCountArgs.seederLeecherCouples / (double) peerCountArgs.superSeeders) /
                            Math.log(2));

            // A small info for all waiters
            logger.info(">>> One transfer needs approx.:                            " + timePerTransfer + " seconds");
            logger.info(">>> A Sequential algorithm needs approx.:                  " + dumbTime + " seconds");
            logger.info(">>> A Logarithmic algorithm needs approx.:                 " + logarithmicTime + " seconds");
            logger.info(">>> A (SuperSeeder)ChunkedSwarm algorithm needs approx.:   " + chunkSwarmTime + " seconds");
        } else {
            logger.info(">>> Cannot estimate time because upload rates differ or are infinite");
        }
    }

    private void setupPeers() throws Exception {
        // Create the data info completion handler
        DataInfoCompletionHandler dataInfoCompletionHandler =
                new DataInfoCompletionHandler(peerCountArgs.seederLeecherCouples * dataInfoGeneratorArgs.partitions);

        // Init the count down latch
        completionCountDownLatch = dataInfoCompletionHandler.getCountDownLatch();

        // The benchmark always uses the fake data base
        Supplier<DataBase> dataBaseSupplier = DataBases::fakeDataBase;

        // Setup all seeders
        for (int i = 0; i < peerCountArgs.superSeeders; i++) {
            // Instantiate a new super seeder
            Seeder seeder = Peers.seeder(networkArgs.peerType,
                                         superSeederBandwidthArgs.maxSuperSeederUploadRate,
                                         superSeederBandwidthArgs.maxSuperSeederDownloadRate,
                                         networkArgs.getSuperSeederSocketAddress(i),
                                         dataBaseSupplier.get(),
                                         networkArgs.getSuperSeederDistributionAlgorithm(),
                                         Optional.ofNullable(statisticsManager.getRecordPeerHandler()),
                                         Optional.of(eventLoopGroup)).getInitFuture().get();

            // Link super seeder
            statisticsManager.getSuperSeederUploadBandwidthStatisticPeers().add(seeder);
            dataInfoGeneratorArgs.getDataBaseUpdatePeers().add(seeder);
            peerCountArgs.getSuperSeederQueue().add(seeder);
            peerCountArgs.getPeerQueue().add(seeder);
        }

        // Setup all leechers
        for (int i = 0; i < peerCountArgs.seederLeecherCouples; i++) {
            // Create the seeder-leecher-couple handler list
            LeecherHandlerList leecherHandlerList = new LeecherHandlerList();
            leecherHandlerList.add(dataInfoCompletionHandler);
            if (statisticsManager.getRecordPeerHandler() != null) {
                leecherHandlerList.add(statisticsManager.getRecordPeerHandler());
            }

            if (networkArgs.algorithmType != Algorithms.AlgorithmType.Sequential) {
                // Instantiate new seeder-leecher couple
                Tuple2<Seeder, Leecher> tuple = Peers.initSeederAndLeecher(
                        networkArgs.peerType,
                        bandwidthArgs.maxUploadRate,
                        bandwidthArgs.maxDownloadRate,
                        networkArgs.getSeederLeecherCoupleSocketAddress(i),
                        dataBaseSupplier.get(),
                        networkArgs.getSeederDistributionAlgorithm(),
                        networkArgs.getLeecherDistributionAlgorithm(),
                        Optional.ofNullable(statisticsManager.getRecordPeerHandler()),
                        Optional.of(leecherHandlerList),
                        true,
                        Optional.of(eventLoopGroup)).get();

                Seeder seeder = tuple.first();
                Leecher leecher = tuple.second();

                statisticsManager.getChunkCompletionStatisticPeers().add(leecher);
                statisticsManager.getDownloadBandwidthStatisticPeers().add(leecher);
                statisticsManager.getUploadBandwidthStatisticPeers().add(seeder);

                peerCountArgs.getPeerQueue().add(seeder);
                peerCountArgs.getPeerQueue().add(leecher);
                peerCountArgs.getLeecherQueue().add(leecher);

                // Connect to all super seeders
                peerCountArgs.getSuperSeederQueue()
                             .stream()
                             .map(Peer::getPeerId)
                             .map(PeerId::getSocketAddress)
                             .map(Optional::get)
                             .map(leecher::connect)
                             .forEach(CompletableFuture::join);
            } else {
                // Instantiate new leecher only
                Leecher leecher = Peers.leecher(networkArgs.peerType,
                                                bandwidthArgs.maxUploadRate,
                                                bandwidthArgs.maxDownloadRate,
                                                Optional.empty(),
                                                dataBaseSupplier.get(),
                                                networkArgs.getLeecherDistributionAlgorithm(),
                                                Optional.of(leecherHandlerList),
                                                true,
                                                Optional.of(eventLoopGroup),
                                                Optional.empty());

                statisticsManager.getChunkCompletionStatisticPeers().add(leecher);
                statisticsManager.getDownloadBandwidthStatisticPeers().add(leecher);

                peerCountArgs.getPeerQueue().add(leecher);
                peerCountArgs.getLeecherQueue().add(leecher);

                // Connect to all super seeders
                peerCountArgs.getSuperSeederQueue()
                             .stream()
                             .map(Peer::getPeerId)
                             .map(PeerId::getSocketAddress)
                             .map(Optional::get)
                             .map(leecher::connect)
                             .forEach(CompletableFuture::join);
            }
        }
    }


    private void waitForConnections() throws InterruptedException {
        // The expected number of connections
        int limit = NettyConfig.getMaxConnectionsPerLeecher();
        limit = limit < 1 ? Integer.MAX_VALUE : limit;
        int expectedConnections = networkArgs.algorithmType == Algorithms.AlgorithmType.Sequential ?
                                  peerCountArgs.seederLeecherCouples * peerCountArgs.superSeeders :
                                  peerCountArgs.seederLeecherCouples *
                                  (Math.min(peerCountArgs.seederLeecherCouples - 1 + peerCountArgs.superSeeders,
                                            limit));

        logger.info(">>> [ Waiting for " + expectedConnections + " connections ]");
        while (true) {
            // Count existing connections
            long activeConnections = peerCountArgs.getLeecherQueue()
                                                  .stream()
                                                  .map(Leecher::getConnections)
                                                  .map(Map::entrySet)
                                                  .flatMap(Set::stream)
                                                  .filter(Map.Entry::getValue)
                                                  .count();

            // Wait if there are missing connections
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
        // Setup the statistics
        statisticsManager.setup(statisticArgs.recordStatistics,
                                statisticArgs.recordEvents,
                                statisticArgs.recordsDirectory.toPath());

        // Setup the netty implementation
        NettyConfig.setupConfig(Math.min(bandwidthArgs.getSmallestBandwidth(),
                                         superSeederBandwidthArgs.getSmallestBandwidth()),
                                dataInfoGeneratorArgs.chunkSize,
                                networkArgs.maxConnections,
                                networkArgs.metaDataSizePercentage,
                                networkArgs.binaryCodec);

        // Setup the peers
        setupPeers();

        // Info info
        logTransferInfo();

        // Wait for all connections
        waitForConnections();

        // Start statistics
        statisticsManager.start(AppConfig.getStatisticInterval(), Instant.now());

        // Publish the data set
        List<DataInfo> dataInfo = dataInfoGeneratorArgs.generateDataInfo();
        statisticsManager.getCompletionDataInfo().addAll(dataInfo);
        dataInfoGeneratorArgs.updatePeerDataBases(dataInfo);

        // Await termination
        completionCountDownLatch.await();

        // Stop and write statistics
        Instant now = Instant.now();
        Thread.sleep(3000);
        statisticsManager.stop(now);

        // Close all and wait
        Peers.closeAndWait(peerCountArgs.getPeerQueue());
    }

    @Override
    public boolean check(JCommander jCommander) {
        return helpArgs.check(jCommander) &&
               peerCountArgs.check(jCommander) &&
               networkArgs.check(jCommander) &&
               bandwidthArgs.check(jCommander) &&
               superSeederBandwidthArgs.check(jCommander) &&
               dataInfoGeneratorArgs.check(jCommander) &&
               statisticArgs.check(jCommander);
    }

    public static void main(String[] args) throws Exception {
        new Benchmark().parse(args);
    }
}

