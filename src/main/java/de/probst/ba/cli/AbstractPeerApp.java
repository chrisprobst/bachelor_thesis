package de.probst.ba.cli;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;
import com.beust.jcommander.converters.FileConverter;
import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.distribution.algorithms.Algorithms;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.handler.handlers.RecordPeerHandler;
import de.probst.ba.core.net.peer.peers.Peers;
import de.probst.ba.core.net.peer.peers.netty.NettyConfig;
import de.probst.ba.core.net.peer.peers.netty.handlers.traffic.TrafficUtil;
import de.probst.ba.core.net.peer.state.BandwidthStatisticState;
import de.probst.ba.core.statistic.AbstractStatistic;
import de.probst.ba.core.statistic.BandwidthStatistic;
import de.probst.ba.core.statistic.ChunkCompletionStatistic;
import de.probst.ba.core.util.concurrent.CancelableRunnable;
import de.probst.ba.core.util.concurrent.Task;
import de.probst.ba.core.util.io.IOUtil;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Created by chrisprobst on 07.09.14.
 */
public abstract class AbstractPeerApp {

    public static final int STATISTIC_INTERVAL = 250;

    @Parameter(names = {"-pt", "--peer-type"},
               description = "Peer type [Local, TCP]",
               converter = PeerTypeConverter.class)
    protected Peers.PeerType peerType = Peers.PeerType.TCP;

    @Parameter(names = {"-da", "--distribution-algorithm"},
               description = "Distribution algorithm type [SuperSeederChunkedSwarm, ChunkedSwarm, Logarithmic]",
               converter = AlgorithmTypeConverter.class)
    protected Algorithms.AlgorithmType algorithmType = Algorithms.AlgorithmType.ChunkedSwarm;

    @Parameter(names = {"-bc", "--binary-codec"},
               description = "Use a binary codec for the meta data instead of using refs " +
                             "(This option is implicitly true if non-local transport is used)")
    protected Boolean binaryCodec = false;

    @Parameter(names = {"--help"},
               description = "Show usage")
    protected Boolean showUsage = false;

    @Parameter(names = {"-re", "--record-events"},
               description = "Record the events and serialize them")
    protected Boolean recordEvents = false;

    @Parameter(names = {"-rs", "--record-statistics"},
               description = "Record statistics and save them in cvs form")
    protected Boolean recordStats = false;

    @Parameter(names = {"-rd", "--records-directory"},
               description = "The directory to save the records",
               converter = FileConverter.class,
               required = false)
    protected File recordsDirectory = new File(".");

    @Parameter(names = {"-pa", "--parts"},
               description = "(Experimental) The number of parts (" + PartsValidator.MSG + ")",
               validateValueWith = PartsValidator.class)
    protected Integer parts = 1;

    @Parameter(names = {"-ms", "--meta-size"},
               description =
                       "The simulated size of the meta data expressed in percentage relative to the chunk size (" +
                       PercentageValidator.MSG + ") (Ignored if binary codec is activated)",
               validateValueWith = PercentageValidator.class)
    protected Double metaDataSize = 0.0;

    @Parameter(names = {"-c", "--chunk-count"},
               description = "The number of chunks (" + ChunkCountValidator.MSG + ")",
               validateValueWith = ChunkCountValidator.class)
    protected Integer chunkCount = 100;

    @Parameter(names = {"-t", "--total-size"},
               description = "The total size in bytes (" + TotalSizeValidator.MSG + ")",
               validateValueWith = TotalSizeValidator.class)
    protected Long totalSize = 10_000_000L;

    @Parameter(names = {"-u", "--upload-rate"},
               description = "The upload rate in bytes per second, " +
                             "must be less-equal than the download rate (" + TransferRateValidator.MSG + ")",
               validateValueWith = TransferRateValidator.class)
    protected Integer uploadRate = 1_000_000;

    @Parameter(names = {"-d", "--download-rate"},
               description = "The download rate in bytes per second, " +
                             "must greater-equal than the upload rate (" + TransferRateValidator.MSG + ")",
               validateValueWith = TransferRateValidator.class)
    protected Integer downloadRate = 0;

    protected boolean checkParameters(JCommander jCommander) {
        if (showUsage) {
            return false;
        }

        if (downloadRate != 0 && uploadRate > downloadRate) {
            System.out.println("The upload rate is greater than the download rate");
            return false;
        }

        if (!recordsDirectory.exists()) {
            System.out.println("The directory path does not exist");
            return false;
        }

        if (!recordsDirectory.isDirectory()) {
            System.out.println("The directory path must point to a directory");
            return false;
        }

        if (!binaryCodec && peerType != Peers.PeerType.Local) {
            System.out.println("Binary codec implicitly activated (Non-local transport is used)");
            binaryCodec = true;
        }

        if (binaryCodec && metaDataSize > 0.0) {
            System.out.println("Ignoring meta data size (Binary codec is activated)");
            metaDataSize = 0.0;
        }

        return true;
    }

    protected final Queue<Peer> dataBaseUpdatePeers = new ConcurrentLinkedQueue<>();
    protected Logger logger;

    // Events
    protected RecordPeerHandler recordPeerHandler;

    // Statistics
    protected final Queue<Peer> uploadBandwidthStatisticPeers = new ConcurrentLinkedQueue<>();
    protected final Queue<Peer> downloadBandwidthStatisticPeers = new ConcurrentLinkedQueue<>();
    protected final Queue<Peer> chunkCompletionStatisticPeers = new ConcurrentLinkedQueue<>();
    protected final List<AbstractStatistic> statistics = new LinkedList<>();

    protected CancelableRunnable statisticTask;
    protected DataInfo[] dataInfo;
    protected Instant startTime;

    protected SeederDistributionAlgorithm getSeederOnlyDistributionAlgorithm() {
        return Algorithms.getSeederOnlyDistributionAlgorithm(algorithmType);
    }

    protected SeederDistributionAlgorithm getSeederDistributionAlgorithm() {
        return Algorithms.getSeederDistributionAlgorithm(algorithmType);
    }

    protected LeecherDistributionAlgorithm getLeecherDistributionAlgorithm() {
        return Algorithms.getLeecherDistributionAlgorithm(algorithmType);
    }

    protected void setupVerbosity() throws Exception {
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
        logger = LoggerFactory.getLogger(getClass());
    }

    protected void setupConfig() {
        TrafficUtil.setDefaultMessageSize((long) (totalSize / chunkCount * (metaDataSize) / 100));
        NettyConfig.setUseCodec(binaryCodec);

        logger.info(">>> [ Config ]");
        logger.info(">>> Peer type:                 " + peerType);
        logger.info(">>> Algorithm type:            " + algorithmType);
        logger.info(">>> Simulated meta data size:  " + TrafficUtil.getDefaultMessageSize() + " bytes");
        logger.info(">>> Using codec:               " + NettyConfig.isUseCodec());
        logger.info(">>> Total size:                " + totalSize);
        logger.info(">>> Chunk count:               " + chunkCount);
        logger.info(">>> Upload rate:               " + uploadRate);
        logger.info(">>> Download rate:             " + downloadRate);
        logger.info(">>> Parts:                     " + parts);
    }

    protected void setupRecords() {
        // Setup events
        if (recordEvents) {
            recordPeerHandler = new RecordPeerHandler();
        }

        logger.info(">>> Record directory:          " + recordsDirectory.getAbsolutePath());
        logger.info(">>> Record events:             " + recordEvents);
        logger.info(">>> Record stats:              " + recordStats);
    }

    protected void setupDataInfo() {
        dataInfo = IntStream.range(0, parts)
                            .mapToObj(i -> new DataInfo(i,
                                                        totalSize,
                                                        Optional.empty(),
                                                        Optional.empty(),
                                                        getClass().getSimpleName() + " pseudo hash for part " + i,
                                                        chunkCount,
                                                        String::valueOf).full())
                            .toArray(DataInfo[]::new);
    }

    protected void setupPeerHandlers() {

    }

    protected void setupPeers() throws Exception {
    }

    protected void setup() throws Exception {
        setupVerbosity();
        setupConfig();
        setupRecords();
        setupDataInfo();
        setupPeerHandlers();
        setupPeers();
    }

    protected void setupStartTime() {
        startTime = Instant.now();
        logger.info(">>> [ Starting " + getClass().getSimpleName() + " ]");
    }

    protected void setupStartRecords(ScheduledExecutorService scheduledExecutorService) {

        // Start stats
        if (recordStats) {

            List<Runnable> statisticRunnables = new LinkedList<>();

            Collection<Peer> copy = new ArrayList<>(chunkCompletionStatisticPeers);
            if (dataInfo != null && dataInfo.length > 0 && !copy.isEmpty()) {
                ChunkCompletionStatistic chunkCompletionStatistic = new ChunkCompletionStatistic("ChunkCompletion",
                                                                                                 copy,
                                                                                                 dataInfo[0].getHash(),
                                                                                                 false);
                ChunkCompletionStatistic totalChunkCompletionStatistic =
                        new ChunkCompletionStatistic("TotalChunkCompletion",
                                                     copy,
                                                     dataInfo[0].getHash(),
                                                     true);


                statistics.add(chunkCompletionStatistic);
                statistics.add(totalChunkCompletionStatistic);
            }

            copy = new ArrayList<>(uploadBandwidthStatisticPeers);
            if (!copy.isEmpty()) {
                BandwidthStatistic currentTotalMedian =
                        new BandwidthStatistic("CurrentUploadTotalMedian",
                                               copy,
                                               BandwidthStatisticState::getCurrentUploadRate,
                                               BandwidthStatistic.BandwidthStatisticMode.TotalMedian);

                BandwidthStatistic currentTotalAccumulated =
                        new BandwidthStatistic("CurrentUploadTotalAccumulated",
                                               copy,
                                               BandwidthStatisticState::getCurrentUploadRate,
                                               BandwidthStatistic.BandwidthStatisticMode.TotalAccumulated);

                BandwidthStatistic currentPeer =
                        new BandwidthStatistic("CurrentUploadPeer",
                                               copy,
                                               BandwidthStatisticState::getCurrentUploadRate,
                                               BandwidthStatistic.BandwidthStatisticMode.Peer);

                BandwidthStatistic averageTotalMedian =
                        new BandwidthStatistic("AverageUploadTotalMedian",
                                               copy,
                                               BandwidthStatisticState::getAverageUploadRate,
                                               BandwidthStatistic.BandwidthStatisticMode.TotalMedian);

                BandwidthStatistic averageTotalAccumulated =
                        new BandwidthStatistic("AverageUploadTotalAccumulated",
                                               copy,
                                               BandwidthStatisticState::getAverageUploadRate,
                                               BandwidthStatistic.BandwidthStatisticMode.TotalAccumulated);

                BandwidthStatistic averagePeer =
                        new BandwidthStatistic("AverageUploadPeer",
                                               copy,
                                               BandwidthStatisticState::getAverageUploadRate,
                                               BandwidthStatistic.BandwidthStatisticMode.Peer);

                statistics.add(currentTotalMedian);
                statistics.add(currentTotalAccumulated);
                statistics.add(currentPeer);
                statistics.add(averageTotalMedian);
                statistics.add(averageTotalAccumulated);
                statistics.add(averagePeer);
            }

            copy = new ArrayList<>(downloadBandwidthStatisticPeers);
            if (!copy.isEmpty()) {
                BandwidthStatistic currentTotalMedian =
                        new BandwidthStatistic("CurrentDownloadTotalMedian",
                                               copy,
                                               BandwidthStatisticState::getCurrentDownloadRate,
                                               BandwidthStatistic.BandwidthStatisticMode.TotalMedian);

                BandwidthStatistic currentTotalAccumulated =
                        new BandwidthStatistic("CurrentDownloadTotalAccumulated",
                                               copy,
                                               BandwidthStatisticState::getCurrentDownloadRate,
                                               BandwidthStatistic.BandwidthStatisticMode.TotalAccumulated);

                BandwidthStatistic currentPeer =
                        new BandwidthStatistic("CurrentDownloadPeer",
                                               copy,
                                               BandwidthStatisticState::getCurrentDownloadRate,
                                               BandwidthStatistic.BandwidthStatisticMode.Peer);

                BandwidthStatistic averageTotalMedian =
                        new BandwidthStatistic("AverageDownloadTotalMedian",
                                               copy,
                                               BandwidthStatisticState::getAverageDownloadRate,
                                               BandwidthStatistic.BandwidthStatisticMode.TotalMedian);

                BandwidthStatistic averageTotalAccumulated =
                        new BandwidthStatistic("AverageDownloadTotalAccumulated",
                                               copy,
                                               BandwidthStatisticState::getAverageDownloadRate,
                                               BandwidthStatistic.BandwidthStatisticMode.TotalAccumulated);

                BandwidthStatistic averagePeer =
                        new BandwidthStatistic("AverageDownloadPeer",
                                               copy,
                                               BandwidthStatisticState::getAverageDownloadRate,
                                               BandwidthStatistic.BandwidthStatisticMode.Peer);

                statistics.add(currentTotalMedian);
                statistics.add(currentTotalAccumulated);
                statistics.add(currentPeer);
                statistics.add(averageTotalMedian);
                statistics.add(averageTotalAccumulated);
                statistics.add(averagePeer);
            }

            // Add all statistics to task
            statistics.forEach(statistic -> statisticRunnables.add(statistic::writeStatistic));

            // Setup task
            if (!statisticRunnables.isEmpty()) {
                statisticTask = new Task(task -> {
                    statisticRunnables.forEach(Runnable::run);
                    task.run();
                }, runnable -> scheduledExecutorService.schedule(runnable, STATISTIC_INTERVAL, TimeUnit.MILLISECONDS));
                statisticTask.run();
            }
        }

        // Start events
        if (recordEvents) {
            if (recordPeerHandler != null) {
                recordPeerHandler.start();
            }
        }
    }

    protected void setupStart(ScheduledExecutorService scheduledExecutorService) {
        setupStartRecords(scheduledExecutorService);
        setupStartTime();
        updatePeerDataBases();
    }


    protected void setupStopTime(Instant now) {
        Duration duration = Duration.between(startTime, now);
        logger.info(">>> [ Completed " + getClass().getSimpleName() + " in " + (duration.toMillis() / 1000.0) +
                    " seconds ]");
    }

    protected void setupStopRecords() throws IOException {
        // Stop events
        if (recordEvents) {
            recordPeerHandler.end();
        }

        // Stop stats
        if (recordStats) {
            // CSV
            logger.info(">>> [ Writing stats now ]");
            Instant timeStamp = Instant.now();

            if (statisticTask != null) {
                statisticTask.cancel();
            }

            // Write statistics
            for (AbstractStatistic statistic : statistics) {
                Path statisticPath = new File(recordsDirectory,
                                              algorithmType + getClass().getSimpleName() +
                                              statistic.getName() + ".csv").toPath();

                logger.info(">>> [ Writing " + statisticPath + " ]");
                Files.write(statisticPath, statistic.toString().getBytes());
            }

            Duration duration = Duration.between(timeStamp, Instant.now());
            logger.info(">>> [ Done in: " + (duration.toMillis() / 1000.0) + " seconds ]");
        }

        // Save events
        if (recordEvents) {

            // Get records and serialize
            logger.info(">>> [ Writing events now ]");
            Instant timeStamp = Instant.now();

            File file = new File(recordsDirectory, algorithmType + getClass().getSimpleName() + "Events.dat");
            logger.info(">>> [ Writing " + file + " ]");
            IOUtil.serialize(file, recordPeerHandler.sortAndGetRecords());

            Duration duration = Duration.between(timeStamp, Instant.now());
            logger.info(">>> [ Done in: " + (duration.toMillis() / 1000.0) + " seconds ]");
        }
    }

    protected void setupStop(Instant now) throws IOException, ExecutionException, InterruptedException {
        setupStopTime(now);
        setupStopRecords();
    }

    protected void updatePeerDataBases() {
        dataBaseUpdatePeers.forEach(s -> Arrays.stream(dataInfo).forEach(s.getDataBase()::update));
    }

    protected abstract void start() throws Exception;

    public void parse(String[] args) throws Exception {
        JCommander jCommander = new JCommander(this);

        try {
            jCommander.parse(args);
            if (checkParameters(jCommander)) {
                start();
            } else {
                System.out.println();
                jCommander.usage();
            }
        } catch (ParameterException e) {
            System.out.println(e.getMessage());
            System.out.println();
            jCommander.usage();
            return;
        }
    }

    public static class PeerTypeConverter extends BaseConverter<Peers.PeerType> {

        public PeerTypeConverter(String optionName) {
            super(optionName);
        }

        @Override
        public Peers.PeerType convert(String value) {
            try {
                return Peers.PeerType.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw new ParameterException(getErrorString(value, Arrays.toString(Peers.PeerType.values())));
            }
        }
    }

    public static class AlgorithmTypeConverter extends BaseConverter<Algorithms.AlgorithmType> {

        public AlgorithmTypeConverter(String optionName) {
            super(optionName);
        }

        @Override
        public Algorithms.AlgorithmType convert(String value) {
            try {
                return Algorithms.AlgorithmType.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw new ParameterException(getErrorString(value, Arrays.toString(Algorithms.AlgorithmType.values())));
            }
        }
    }

    public static class HostNameConverter extends BaseConverter<InetAddress> {

        public HostNameConverter(String optionName) {
            super(optionName);
        }

        @Override
        public InetAddress convert(String value) {
            try {
                return InetAddress.getByName(value);
            } catch (UnknownHostException e) {
                throw new ParameterException(getErrorString(value, "a host name, cause " + e.getMessage()));
            }
        }
    }

    public static class PortValidator implements IValueValidator<Integer> {

        public static final int MIN = 0;
        public static final int MAX = 65535;
        public static final String MSG = "Must be between " + MIN + " and " + MAX;

        @Override
        public void validate(String name, Integer value) throws ParameterException {
            if (value < MIN || value > MAX) {
                throw new ParameterException("Parameter " + name + ": " + MSG + " (found: " + value + ")");
            }
        }
    }

    public static class PeerCountValidator implements IValueValidator<Integer> {

        public static final int MIN = 1;
        public static final int MAX = 1000;
        public static final String MSG = "Must be between " + MIN + " and " + MAX;

        @Override
        public void validate(String name, Integer value) throws ParameterException {
            if (value < MIN || value > MAX) {
                throw new ParameterException("Parameter " + name + ": " + MSG + " (found: " + value + ")");
            }
        }
    }

    public static class TransferRateValidator implements IValueValidator<Integer> {

        public static final int MIN = 0;
        public static final int MAX = 125_000_000;
        public static final String MSG = "Must be between " + MIN + " and " + MAX;

        @Override
        public void validate(String name, Integer value) throws ParameterException {
            if (value < MIN || value > MAX) {
                throw new ParameterException("Parameter " + name + ": " + MSG + " (found: " + value + ")");
            }
        }
    }

    public static class TotalSizeValidator implements IValueValidator<Long> {

        public static final long MIN = 1;
        public static final long MAX = 100_000_000_000L;
        public static final String MSG = "Must be between " + MIN + " and " + MAX;

        @Override
        public void validate(String name, Long value) throws ParameterException {
            if (value < MIN || value > MAX) {
                throw new ParameterException("Parameter " + name + ": " + MSG + " (found: " + value + ")");
            }
        }
    }

    public static class ChunkCountValidator implements IValueValidator<Integer> {

        public static final int MIN = 1;
        public static final int MAX = 10_000;
        public static final String MSG = "Must be between " + MIN + " and " + MAX;

        @Override
        public void validate(String name, Integer value) throws ParameterException {
            if (value < MIN || value > MAX) {
                throw new ParameterException("Parameter " + name + ": " + MSG + " (found: " + value + ")");
            }
        }
    }

    public static class PartsValidator implements IValueValidator<Integer> {

        public static final int MIN = 1;
        public static final int MAX = 1000;
        public static final String MSG = "Must be between " + MIN + " and " + MAX;

        @Override
        public void validate(String name, Integer value) throws ParameterException {
            if (value < MIN || value > MAX) {
                throw new ParameterException("Parameter " + name + ": " + MSG + " (found: " + value + ")");
            }
        }
    }

    public static class PercentageValidator implements IValueValidator<Double> {

        public static final double MIN = 0;
        public static final double MAX = 100;
        public static final String MSG = "Must be between " + MIN + " and " + MAX;

        @Override
        public void validate(String name, Double value) throws ParameterException {
            if (value < MIN || value > MAX) {
                throw new ParameterException("Parameter " + name + ": " + MSG + " (found: " + value + ")");
            }
        }
    }
}
