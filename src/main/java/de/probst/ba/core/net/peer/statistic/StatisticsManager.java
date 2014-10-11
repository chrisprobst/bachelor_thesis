package de.probst.ba.core.net.peer.statistic;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.handler.handlers.RecordPeerHandler;
import de.probst.ba.core.util.concurrent.CancelableRunnable;
import de.probst.ba.core.util.concurrent.Task;
import de.probst.ba.core.util.io.IOUtil;
import de.probst.ba.core.util.statistic.Statistic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 10.10.14.
 */
public final class StatisticsManager {

    private final Logger logger = LoggerFactory.getLogger(StatisticsManager.class);

    // The scheduler
    private final ScheduledExecutorService scheduledExecutorService;

    // Statistics vars
    private Instant startTime;
    private Path recordsDirectory;
    private final List<Statistic<Peer>> statistics = new LinkedList<>();
    private CancelableRunnable statisticsTask;
    private RecordPeerHandler recordPeerHandler;
    private boolean setup = false;
    private boolean started = false;
    private boolean stopped = false;

    // Queues from which the statistic is built
    private final Queue<Peer> superSeederUploadBandwidthStatisticPeers = new ConcurrentLinkedQueue<>();
    private final Queue<Peer> uploadBandwidthStatisticPeers = new ConcurrentLinkedQueue<>();
    private final Queue<Peer> downloadBandwidthStatisticPeers = new ConcurrentLinkedQueue<>();
    private final Queue<Peer> chunkCompletionStatisticPeers = new ConcurrentLinkedQueue<>();
    private final Queue<DataInfo> completionDataInfo = new ConcurrentLinkedQueue<>();

    // Mapper functions
    private final Function<Peer, String> socketAddressMapper = peer -> peer.getPeerId().getSocketAddress().toString();
    private final Function<Peer, Number> totalUploadedMapper =
            peer -> peer.getBandwidthStatisticState().getTotalUploaded();
    private final Function<Peer, Number> totalDownloadedMapper =
            peer -> peer.getBandwidthStatisticState().getTotalDownloaded();
    private final Function<Peer, Number> uploadRateMapper =
            peer -> peer.getBandwidthStatisticState().getCurrentUploadRate();
    private final Function<Peer, Number> downloadRateMapper =
            peer -> peer.getBandwidthStatisticState().getCurrentDownloadRate();
    private final Function<Peer, Number> dataInfoCompletionMapper =
            peer -> completionDataInfo.stream().map(DataInfo::getHash).map(
                    peer.getDataBase()::get).mapToDouble(x -> x != null ? x.getPercentage() : 0.0).sum();

    public StatisticsManager(ScheduledExecutorService scheduledExecutorService) {
        Objects.requireNonNull(scheduledExecutorService);
        this.scheduledExecutorService = scheduledExecutorService;
    }

    public synchronized void setup(boolean recordStatistics, boolean recordEvents, Path recordsDirectory) {
        if (recordStatistics || recordEvents) {
            Objects.requireNonNull(recordsDirectory);
        }

        if (setup) {
            throw new IllegalStateException("setup");
        }
        setup = true;
        this.recordsDirectory = recordsDirectory;

        if (recordStatistics) {

            // Add chunk completion statistic
            statistics.add(new Statistic<>("ChunkCompletion",
                                           chunkCompletionStatisticPeers,
                                           socketAddressMapper,
                                           dataInfoCompletionMapper));

            // Add super seeder statistics
            statistics.add(new Statistic<>("CurrentSuperSeederUploadBandwidth",
                                           superSeederUploadBandwidthStatisticPeers,
                                           socketAddressMapper,
                                           uploadRateMapper));
            statistics.add(new Statistic<>("TotalSuperSeederUploadedBandwidth",
                                           superSeederUploadBandwidthStatisticPeers,
                                           socketAddressMapper,
                                           totalUploadedMapper));

            // Add seeder statistics
            statistics.add(new Statistic<>("CurrentUploadBandwidth",
                                           uploadBandwidthStatisticPeers,
                                           socketAddressMapper,
                                           uploadRateMapper));
            statistics.add(new Statistic<>("TotalUploadedBandwidth",
                                           uploadBandwidthStatisticPeers,
                                           socketAddressMapper,
                                           totalUploadedMapper));

            // Add leecher statistics
            statistics.add(new Statistic<>("CurrentDownloadBandwidth",
                                           downloadBandwidthStatisticPeers,
                                           socketAddressMapper,
                                           downloadRateMapper));
            statistics.add(new Statistic<>("TotalDownloadedBandwidth",
                                           downloadBandwidthStatisticPeers,
                                           socketAddressMapper,
                                           totalDownloadedMapper));
        }

        if (recordEvents) {
            recordPeerHandler = new RecordPeerHandler();
        }


        // Log the settings
        logger.info(">>> [ Statistics setup]");
        logger.info(">>> Record directory:  " + recordsDirectory);
        logger.info(">>> Record events:     " + recordEvents);
        logger.info(">>> Record statistics: " + recordStatistics);
    }

    public synchronized void start(long statisticInterval, Instant now) {
        Objects.requireNonNull(now);

        if (!setup) {
            throw new IllegalStateException("!setup");
        }
        if (started) {
            throw new IllegalStateException("started");
        }
        started = true;
        startTime = now;

        // Setup task
        if (!statistics.isEmpty()) {
            List<Runnable> statisticRunnables = statistics.stream()
                                                          .map(statistic -> (Runnable) statistic::writeNextEntry)
                                                          .collect(Collectors.toList());

            // Write start statistic
            statisticRunnables.forEach(Runnable::run);

            // Setup task and run
            (statisticsTask = new Task(task -> {
                statisticRunnables.forEach(Runnable::run);
                task.run();
            }, runnable -> scheduledExecutorService.schedule(runnable,
                                                             statisticInterval,
                                                             TimeUnit.MILLISECONDS))).run();

            logger.info(">>> [ Recording statistics now ]");
        }

        if (recordPeerHandler != null) {
            recordPeerHandler.start();
            logger.info(">>> [ Recording events now ]");
        }

        logger.info(">>> [ Statistics started ]");
    }

    public synchronized void stop(Instant now) throws IOException {
        Objects.requireNonNull(now);

        if (!started) {
            throw new IllegalStateException("!started");
        }
        if (stopped) {
            throw new IllegalStateException("stopped");
        }
        stopped = true;
        Duration totalDuration = Duration.between(startTime, now);

        if (recordPeerHandler != null) {
            recordPeerHandler.end();
        }

        if (!statistics.isEmpty()) {
            // CSV
            logger.info(">>> [ Writing stats now ]");
            Instant timeStamp = Instant.now();

            // Stop task
            statisticsTask.cancel();

            // Write statistics
            for (Statistic<Peer> statistic : statistics) {
                Path statisticPath = recordsDirectory.resolve(statistic.getName() + ".csv");

                logger.info(">>> [ Writing " + statisticPath + " ]");
                Files.write(statisticPath, statistic.toString().getBytes());
            }

            Duration duration = Duration.between(timeStamp, Instant.now());
            logger.info(">>> [ Done in: " + (duration.toMillis() / 1000.0) + " seconds ]");
        }

        // Save events
        if (recordPeerHandler != null) {

            // Get records and serialize
            logger.info(">>> [ Writing events now ]");
            Instant timeStamp = Instant.now();

            File file = recordsDirectory.resolve("Events.dat").toFile();
            logger.info(">>> [ Writing " + file + " ]");
            IOUtil.serialize(file, recordPeerHandler.sortAndGetRecords());

            Duration duration = Duration.between(timeStamp, Instant.now());
            logger.info(">>> [ Done in: " + (duration.toMillis() / 1000.0) + " seconds ]");
        }

        logger.info(">>> [ Completed statistics in " + (totalDuration.toMillis() / 1000.0) + " seconds ]");
    }

    public RecordPeerHandler getRecordPeerHandler() {
        return recordPeerHandler;
    }

    public Queue<DataInfo> getCompletionDataInfo() {
        return completionDataInfo;
    }

    public Queue<Peer> getChunkCompletionStatisticPeers() {
        return chunkCompletionStatisticPeers;
    }

    public Queue<Peer> getDownloadBandwidthStatisticPeers() {
        return downloadBandwidthStatisticPeers;
    }

    public Queue<Peer> getUploadBandwidthStatisticPeers() {
        return uploadBandwidthStatisticPeers;
    }

    public Queue<Peer> getSuperSeederUploadBandwidthStatisticPeers() {
        return superSeederUploadBandwidthStatisticPeers;
    }
}
