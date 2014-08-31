package de.probst.ba.core;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.FileConverter;
import de.probst.ba.core.diag.ChunkCompletionCVSDiagnostic;
import de.probst.ba.core.diag.CombinedDiagnostic;
import de.probst.ba.core.diag.Diagnostic;
import de.probst.ba.core.diag.DiagnosticAdapter;
import de.probst.ba.core.diag.LoggingDiagnostic;
import de.probst.ba.core.diag.RecordDiagnostic;
import de.probst.ba.core.diag.UploadCVSDiagnostic;
import de.probst.ba.core.logic.Brain;
import de.probst.ba.core.logic.brains.Brains;
import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.media.databases.DataBases;
import de.probst.ba.core.net.TransferManager;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.peers.Peers;
import de.probst.ba.core.util.IOUtil;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Created by chrisprobst on 12.08.14.
 */
public class Benchmark {

    public static class PeerCountValidator implements IValueValidator<Integer> {

        public static final int MIN = 1;
        public static final int MAX = 100;
        public static final String MSG = "Must be between " + MIN + " and " + MAX;

        @Override
        public void validate(String name, Integer value) throws ParameterException {
            if (value < MIN || value > MAX) {
                throw new ParameterException("Parameter " + name + ": "
                        + MSG + " (found: " + value + ")");
            }
        }
    }

    public static class TransferRateValidator implements IValueValidator<Integer> {

        public static final int MIN = 1;
        public static final int MAX = 1000 * 1000;
        public static final String MSG = "Must be between " + MIN + " and " + MAX;

        @Override
        public void validate(String name, Integer value) throws ParameterException {
            if (value < MIN || value > MAX) {
                throw new ParameterException("Parameter " + name + ": "
                        + MSG + " (found: " + value + ")");
            }
        }
    }

    public static class DelayValidator implements IValueValidator<Long> {

        public static final long MIN = 10;
        public static final long MAX = 100 * 1000;
        public static final String MSG = "Must be between " + MIN + " and " + MAX;

        @Override
        public void validate(String name, Long value) throws ParameterException {
            if (value < MIN || value > MAX) {
                throw new ParameterException("Parameter " + name + ": "
                        + MSG + " (found: " + value + ")");
            }
        }
    }

    public static class TotalSizeValidator implements IValueValidator<Integer> {

        public static final int MIN = 1;
        public static final int MAX = 10 * 1000 * 1000;
        public static final String MSG = "Must be between " + MIN + " and " + MAX;

        @Override
        public void validate(String name, Integer value) throws ParameterException {
            if (value < MIN || value > MAX) {
                throw new ParameterException("Parameter " + name + ": "
                        + MSG + " (found: " + value + ")");
            }
        }
    }

    public static class ChunkCountValidator implements IValueValidator<Integer> {

        public static final int MIN = 1;
        public static final int MAX = 10 * 1000;
        public static final String MSG = "Must be between " + MIN + " and " + MAX;

        @Override
        public void validate(String name, Integer value) throws ParameterException {
            if (value < MIN || value > MAX) {
                throw new ParameterException("Parameter " + name + ": "
                        + MSG + " (found: " + value + ")");
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
                throw new ParameterException("Parameter " + name + ": "
                        + MSG + " (found: " + value + ")");
            }
        }
    }

    public static class BrainValidator implements IValueValidator<String> {

        public static final String INTELLIGENT = "intelligent";
        public static final String LOGARITHMIC = "logarithmic";
        public static final String MSG = "Must be '" + INTELLIGENT + "' or '" + LOGARITHMIC + "'";

        @Override
        public void validate(String name, String value) throws ParameterException {
            if (!value.equals(INTELLIGENT) && !value.equals(LOGARITHMIC)) {
                throw new ParameterException("Parameter " + name + ": "
                        + MSG + " (found: " + value + ")");
            }
        }
    }

    @Parameter(
            names = {"-ad", "--announce-delay"},
            description = "The announce delay in millis (" + DelayValidator.MSG + ")",
            validateValueWith = DelayValidator.class)
    private Long announceDelay = Config.getAnnounceDelay();

    @Parameter(
            names = {"-bd", "--brain-delay"},
            description = "The brain delay in millis (" + DelayValidator.MSG + ")",
            validateValueWith = DelayValidator.class)
    private Long brainDelay = Config.getBrainDelay();

    @Parameter(
            names = {"-b", "--brain"},
            description = "Brain type (" + BrainValidator.MSG + ")",
            validateValueWith = BrainValidator.class)
    private String brainType = BrainValidator.LOGARITHMIC;

    @Parameter(
            names = {"-h", "--help"},
            description = "Show usage")
    private Boolean showUsage = false;

    @Parameter(
            names = {"-v", "--verbose"},
            description = "Verbose mode")
    private Boolean verbose = false;

    @Parameter(
            names = {"-re", "--record-events"},
            description = "Record the events and serialize them")
    private Boolean recordEvents = false;

    @Parameter(
            names = {"-rs", "--record-statistics"},
            description = "Record statistics and save them in cvs form")
    private Boolean recordStats = false;

    @Parameter(
            names = {"-dir", "--directory"},
            description = "The directory to save the records",
            converter = FileConverter.class,
            required = false)
    private File recordsDirectory = new File(".");

    @Parameter(
            names = {"-p", "--parts"},
            description = "The number of parts (" + PartsValidator.MSG + ")",
            validateValueWith = PartsValidator.class)
    private Integer parts = 1;

    @Parameter(
            names = {"-c", "--chunk-count"},
            description = "The number of chunks (" + ChunkCountValidator.MSG + ")",
            validateValueWith = ChunkCountValidator.class)
    private Integer chunkCount = 40;

    @Parameter(
            names = {"-t", "--total-size"},
            description = "The total size in bytes (" + TotalSizeValidator.MSG + ")",
            validateValueWith = TotalSizeValidator.class)
    private Integer totalSize = 40 * 500;

    @Parameter(
            names = {"-u", "--upload-rate"},
            description = "The upload rate in bytes per second, " +
                    "must be less-equal than the download rate (" + TransferRateValidator.MSG + ")",
            validateValueWith = TransferRateValidator.class)
    private Integer uploadRate = 1000;

    @Parameter(
            names = {"-d", "--download-rate"},
            description = "The download rate in bytes per second, " +
                    "must greater-equal than the upload rate (" + TransferRateValidator.MSG + ")",
            validateValueWith = TransferRateValidator.class)
    private Integer downloadRate = 1000;

    @Parameter(
            names = {"-s", "--seeders"},
            description = "Number of seeders (" + PeerCountValidator.MSG + ")",
            validateValueWith = PeerCountValidator.class)
    private Integer seeders = 1;

    @Parameter(names = {"-l", "--leechers"},
            description = "Number of leechers (" + PeerCountValidator.MSG + ")",
            validateValueWith = PeerCountValidator.class)
    private Integer leechers = 5;

    private boolean checkParameters(JCommander jCommander) {
        if (showUsage) {
            jCommander.usage();
            return false;
        }

        if (uploadRate > downloadRate) {
            System.out.println("The upload rate is greater than the download rate");
            System.out.println();
            jCommander.usage();
            return false;
        }

        if (!recordsDirectory.exists()) {
            System.out.println("The directory path does not exist");
            System.out.println();
            jCommander.usage();
            return false;
        }

        if (!recordsDirectory.isDirectory()) {
            System.out.println("The directory path must point to a directory");
            System.out.println();
            jCommander.usage();
            return false;
        }


        return true;
    }

    private void start() throws ExecutionException, InterruptedException, IOException {

        // Setup config
        Config.setAnnounceDelay(announceDelay);
        Config.setBrainDelay(brainDelay);

        // Setup logging
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", verbose ? "info" : "warn");
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
        Logger logger = LoggerFactory.getLogger(Benchmark.class);

        // Setup status flags
        double timePerTransfer = totalSize / uploadRate;

        // Server/Client like
        double dumbBrainTime = timePerTransfer / seeders * leechers;

        // ~ who knows ?!
        double intelligentBrainTime = timePerTransfer / seeders * 1.3;

        // log(n) - log(s) = log(n / s) = log((s + l) / s) = log(1 + l/s)
        double logarithmicBrainTime = timePerTransfer * Math.ceil(Math.log(1 + leechers / (double) seeders) / Math.log(2));

        // A small info for all waiters
        logger.info("[== One transfer needs approx.: " + timePerTransfer + " seconds ==]");
        logger.info("[== A dumb brain needs approx.: " + dumbBrainTime + " seconds ==]");
        logger.info("[== A logarithmic brain needs approx.: " + logarithmicBrainTime + " seconds ==]");
        logger.info("[== An intelligent brain needs approx.: " + intelligentBrainTime + " seconds ==]");

        // Benchmark data
        DataInfo[] dataInfo = IntStream.range(0, parts)
                .mapToObj(i -> new DataInfo(
                        i,
                        totalSize,
                        Optional.empty(),
                        Optional.empty(),
                        "Benchmark hash, part: " + i,
                        chunkCount,
                        String::valueOf)
                        .full())
                .toArray(DataInfo[]::new);

        // List of peers
        Queue<Peer> peers = new LinkedList<>();

        // The event loop group shared by all peers
        EventLoopGroup eventLoopGroup = new DefaultEventLoopGroup();

        // Setup diagnostic
        CountDownLatch countDownLatch = new CountDownLatch(leechers * parts);
        Diagnostic shutdown = new DiagnosticAdapter() {
            @Override
            public void dataCompleted(Peer peer, DataInfo dataInfo, TransferManager lastTransferManager) {
                countDownLatch.countDown();
            }
        };

        // Event recording
        CombinedDiagnostic combinedEvents;
        RecordDiagnostic eventRecordDiagnostic = null;

        // Stats recording
        CombinedDiagnostic combinedStats;
        ChunkCompletionCVSDiagnostic peerChunkCompletionCVSDiagnostic = null;
        ChunkCompletionCVSDiagnostic totalChunkCompletionCVSDiagnostic = null;
        UploadCVSDiagnostic peerUploadCVSDiagnostic = null;
        UploadCVSDiagnostic totalUploadCVSDiagnostic = null;

        // Setup events
        if (recordEvents) {
            eventRecordDiagnostic = new RecordDiagnostic();
            combinedEvents = new CombinedDiagnostic(eventRecordDiagnostic);
        } else {
            combinedEvents = new CombinedDiagnostic();
        }

        // Setup stats
        if (recordStats) {

            // Setup stats recording
            peerChunkCompletionCVSDiagnostic = new ChunkCompletionCVSDiagnostic();
            totalChunkCompletionCVSDiagnostic = new ChunkCompletionCVSDiagnostic();
            peerUploadCVSDiagnostic = new UploadCVSDiagnostic();
            totalUploadCVSDiagnostic = new UploadCVSDiagnostic();
            combinedStats = new CombinedDiagnostic(
                    peerChunkCompletionCVSDiagnostic,
                    totalChunkCompletionCVSDiagnostic,
                    peerUploadCVSDiagnostic,
                    totalUploadCVSDiagnostic);
        } else {
            combinedStats = new CombinedDiagnostic();
        }

        // Create the combined diagnostic
        Diagnostic combined = new CombinedDiagnostic(
                combinedEvents,
                combinedStats,
                new LoggingDiagnostic(leechers * chunkCount * parts),
                shutdown);

        // Create the brain factory
        Supplier<Brain> brainFactory = () -> brainType.equals(BrainValidator.LOGARITHMIC) ?
                Brains.logarithmicBrain() : Brains.intelligentBrain();

        // Setup all seeders
        for (int i = 0; i < seeders; i++) {
            peers.add(Peers.localPeer(
                    uploadRate,
                    uploadRate,
                    new LocalAddress("S-" + i),
                    DataBases.fakeDataBase(dataInfo),
                    //DataBases.singleFileDataBase(Paths.get("/Users/chrisprobst/Desktop/data.file"), dataInfo[0]),
                    brainFactory.get(),
                    combined,
                    Optional.of(eventLoopGroup)));
        }

        // Setup all leechers
        for (int i = 0; i < leechers; i++) {
            peers.add(Peers.localPeer(
                    uploadRate,
                    uploadRate,
                    new LocalAddress("L-" + i),
                    DataBases.fakeDataBase(),
                    //DataBases.singleFileDataBase(Paths.get("/Users/chrisprobst/Desktop/data.file" + i), dataInfo[0].empty()),
                    brainFactory.get(),
                    combined,
                    Optional.of(eventLoopGroup)));
        }

        // Initialize stats
        if (recordStats) {
            // Setup peer chunk completion cvs diagnostic
            peerChunkCompletionCVSDiagnostic.setTotal(false);
            peerChunkCompletionCVSDiagnostic.setDataInfoHash(dataInfo[0].getHash());
            peerChunkCompletionCVSDiagnostic.setPeers(peers);

            // Setup total chunk completion cvs diagnostic
            totalChunkCompletionCVSDiagnostic.setTotal(true);
            totalChunkCompletionCVSDiagnostic.setDataInfoHash(dataInfo[0].getHash());
            totalChunkCompletionCVSDiagnostic.setPeers(peers);

            // Setup total upload cvs diagnostic
            peerUploadCVSDiagnostic.setTotal(false);
            peerUploadCVSDiagnostic.setPeers(peers);

            // Setup total upload cvs diagnostic
            totalUploadCVSDiagnostic.setTotal(true);
            totalUploadCVSDiagnostic.setPeers(peers);
        }

        // Wait for init
        Peers.waitForInit(peers);

        // Connect every peer to every other peer
        Peers.connectGrid(peers);

        // Start events
        if (recordEvents) {
            eventRecordDiagnostic.start();
        }

        // Start stats
        if (recordStats) {
            peerChunkCompletionCVSDiagnostic.writeStatus();
            totalChunkCompletionCVSDiagnostic.writeStatus();
            peerUploadCVSDiagnostic.writeStatus();
            totalUploadCVSDiagnostic.writeStatus();
        }

        // Stop the time
        Instant timeStamp = Instant.now();

        // Await the count down latch to finish
        countDownLatch.await();

        // Calculate the duration
        Duration duration = Duration.between(timeStamp, Instant.now());

        // Print result
        logger.info("[== COMPLETED IN: " + (duration.toMillis() / 1000.0) + " seconds ==]");

        // Stop events
        if (recordEvents) {
            eventRecordDiagnostic.end();
        }

        // Save stats
        if (recordStats) {
            // CSV
            logger.info("[== WRITING STATS ==]");
            timeStamp = Instant.now();

            // Save peer chunks
            Files.write(new File(recordsDirectory, brainType + "PeerChunks.csv").toPath(),
                    peerChunkCompletionCVSDiagnostic.getCVSString().getBytes());

            // Save total chunks
            Files.write(new File(recordsDirectory, brainType + "TotalChunks.csv").toPath(),
                    totalChunkCompletionCVSDiagnostic.getCVSString().getBytes());

            // Save peer upload
            Files.write(new File(recordsDirectory, brainType + "PeerUploads.csv").toPath(),
                    peerUploadCVSDiagnostic.getCVSString().getBytes());

            // Save total upload
            Files.write(new File(recordsDirectory, brainType + "TotalUploads.csv").toPath(),
                    totalUploadCVSDiagnostic.getCVSString().getBytes());

            duration = Duration.between(timeStamp, Instant.now());
            logger.info("[== DONE IN: " + (duration.toMillis() / 1000.0) + " seconds ==]");
        }

        // Save events
        if (recordEvents) {
            eventRecordDiagnostic.end();

            // Get records and serialize
            logger.info("[== WRITING EVENTS ==]");
            timeStamp = Instant.now();
            IOUtil.serialize(new File(recordsDirectory, brainType + "Records.dat"), eventRecordDiagnostic.sortAndGetRecords());
            duration = Duration.between(timeStamp, Instant.now());
            logger.info("[== DONE IN: " + (duration.toMillis() / 1000.0) + " seconds ==]");
        }

        // Wait for close
        Peers.closeAndWait(peers);
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        // Setup parser
        Benchmark benchmark = new Benchmark();
        JCommander jCommander = new JCommander(benchmark);

        try {
            jCommander.parse(args);
            if (benchmark.checkParameters(jCommander)) {
                benchmark.start();
            }
        } catch (ParameterException e) {
            System.out.println(e.getMessage());
            System.out.println();
            jCommander.usage();
            return;
        }
    }
}

