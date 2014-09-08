package de.probst.ba.core.app;

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
import de.probst.ba.core.net.peer.state.BandwidthStatisticState;
import de.probst.ba.core.statistic.BandwidthStatistic;
import de.probst.ba.core.util.io.IOUtil;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.IntStream;

/**
 * Created by chrisprobst on 07.09.14.
 */
public abstract class AbstractPeerApp {

    public static final int STATISTIC_INTERVAL = 100;

    @Parameter(names = {"-pt", "--peer-type"},
               description = "Peer type [Local, TCP]",
               converter = PeerTypeConverter.class)
    protected Peers.PeerType peerType = Peers.PeerType.TCP;

    @Parameter(names = {"-da", "--distribution-algorithm"},
               description = "Distribution algorithm type [ChunkedSwarm, Logarithmic]",
               converter = AlgorithmTypeConverter.class)
    protected Algorithms.AlgorithmType algorithmType = Algorithms.AlgorithmType.ChunkedSwarm;

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

        return true;
    }

    protected final Queue<Peer> uploadBandwidthStatisticPeers = new ConcurrentLinkedQueue<>();
    protected final Queue<Peer> downloadBandwidthStatisticPeers = new ConcurrentLinkedQueue<>();
    protected final Queue<Peer> dataBaseUpdatePeers = new ConcurrentLinkedQueue<>();
    protected final Queue<Peer> initClosePeerQueue = new ConcurrentLinkedQueue<>();
    protected Logger logger;
    protected RecordPeerHandler recordPeerHandler;
    protected BandwidthStatistic uploadBandwidthStatistic;
    protected BandwidthStatistic downloadBandwidthStatistic;
    protected DataInfo[] dataInfo;
    protected Instant startTime;

    protected SeederDistributionAlgorithm getSeederDistributionAlgorithm() {
        return Algorithms.getSeederDistributionAlgorithm(algorithmType);
    }

    protected LeecherDistributionAlgorithm getLeecherDistributionAlgorithm() {
        return Algorithms.getLeecherDistributionAlgorithm(algorithmType);
    }

    protected void initPeers() throws ExecutionException, InterruptedException {
        Peers.waitForInit(initClosePeerQueue);
    }

    protected void closePeers() throws InterruptedException, ExecutionException, IOException {
        Peers.closeAndWait(initClosePeerQueue);
        Thread.sleep(1000);
    }

    protected void setupVerbosity() throws Exception {
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
        logger = LoggerFactory.getLogger(getClass());
    }

    protected void setupRecords() {
        // Setup events
        if (recordEvents) {
            recordPeerHandler = new RecordPeerHandler();
        }
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

    protected void setupSeeders() {
    }

    protected void setupLeechers() {
    }

    protected void setup() throws Exception {
        setupVerbosity();
        setupRecords();
        setupDataInfo();
        setupPeerHandlers();
        setupSeeders();
        setupLeechers();
    }

    protected void setupStartTime() {
        startTime = Instant.now();
        logger.info("[== Starting " + getClass().getSimpleName() + " ==]");
    }

    protected void setupStartRecords(ScheduledExecutorService scheduledExecutorService) {

        // Start stats
        if (recordStats) {
            BandwidthStatistic.BandwidthStatisticMode bandwidthStatisticMode =
                    BandwidthStatistic.BandwidthStatisticMode.TotalMedian;

            Collection<Peer> copy = new ArrayList<>(uploadBandwidthStatisticPeers);
            if (!copy.isEmpty()) {
                Path uploadStatisticPath = new File(recordsDirectory,
                                                    algorithmType + getClass().getSimpleName() + "Upload" +
                                                    bandwidthStatisticMode + ".csv").toPath();
                uploadBandwidthStatistic = new BandwidthStatistic(uploadStatisticPath,
                                                                  scheduledExecutorService,
                                                                  STATISTIC_INTERVAL,
                                                                  copy,
                                                                  BandwidthStatisticState::getCurrentUploadRate,
                                                                  bandwidthStatisticMode);
                uploadBandwidthStatistic.schedule();
            }

            copy = new ArrayList<>(downloadBandwidthStatisticPeers);
            if (!copy.isEmpty()) {
                Path downloadStatisticPath = new File(recordsDirectory,
                                                      algorithmType + getClass().getSimpleName() + "Download" +
                                                      bandwidthStatisticMode + ".csv").toPath();
                downloadBandwidthStatistic = new BandwidthStatistic(downloadStatisticPath,
                                                                    scheduledExecutorService,
                                                                    STATISTIC_INTERVAL,
                                                                    copy,
                                                                    BandwidthStatisticState::getCurrentDownloadRate,
                                                                    bandwidthStatisticMode);
                downloadBandwidthStatistic.schedule();
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


    protected void setupStopTime() {
        Duration duration = Duration.between(startTime, Instant.now());
        logger.info("[== Completed " + getClass().getSimpleName() + " in " + (duration.toMillis() / 1000.0) +
                    " seconds ==]");
    }

    protected void setupStopRecords() throws IOException {
        // Stop events
        if (recordEvents) {
            recordPeerHandler.end();
        }

        // Stop stats
        if (recordStats) {
            // CSV
            logger.info("[== Writing stats now ==]");
            Instant timeStamp = Instant.now();

            if (uploadBandwidthStatistic != null) {
                logger.info("[== Writing " + uploadBandwidthStatistic.getCsvPath() + " ==]");
                uploadBandwidthStatistic.close();
            }

            if (downloadBandwidthStatistic != null) {
                logger.info("[== Writing " + downloadBandwidthStatistic.getCsvPath() + " ==]");
                downloadBandwidthStatistic.close();
            }

            Duration duration = Duration.between(timeStamp, Instant.now());
            logger.info("[== Done in: " + (duration.toMillis() / 1000.0) + " seconds ==]");
        }

        // Save events
        if (recordEvents) {

            // Get records and serialize
            logger.info("[== Writing events now ==]");
            Instant timeStamp = Instant.now();

            File file = new File(recordsDirectory, algorithmType + getClass().getSimpleName() + "Events.dat");
            logger.info("[== Writing " + file + " ==]");
            IOUtil.serialize(file, recordPeerHandler.sortAndGetRecords());

            Duration duration = Duration.between(timeStamp, Instant.now());
            logger.info("[== Done in: " + (duration.toMillis() / 1000.0) + " seconds ==]");
        }
    }

    protected void setupStop() throws IOException, ExecutionException, InterruptedException {
        setupStopTime();
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
}
