package de.probst.ba.core;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.FileConverter;
import de.probst.ba.core.diag.CombinedDiagnostic;
import de.probst.ba.core.diag.Diagnostic;
import de.probst.ba.core.diag.DiagnosticAdapter;
import de.probst.ba.core.diag.LoggingDiagnostic;
import de.probst.ba.core.diag.PeerChunkCVSDiagnostic;
import de.probst.ba.core.diag.RecordDiagnostic;
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

        public static final int MIN = 50;
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

    public static class TotalSizeValidator implements IValueValidator<Integer> {

        public static final int MIN = 1000;
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
            names = {"-r", "--record"},
            description = "Record the simulation",
            arity = 1)
    private Boolean record = true;

    @Parameter(
            names = {"-dir", "--directory"},
            description = "The directory to save the records",
            converter = FileConverter.class,
            required = false)
    private File recordsDirectory = new File(".");

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
        DataInfo dataInfo = new DataInfo(
                0,
                totalSize,
                Optional.empty(),
                Optional.empty(),
                "Benchmark hash",
                chunkCount,
                String::valueOf)
                .full();

        // List of peers
        Queue<Peer> peers = new LinkedList<>();

        // The event loop group shared by all peers
        EventLoopGroup eventLoopGroup = new DefaultEventLoopGroup();

        // Setup diagnostic
        CountDownLatch countDownLatch = new CountDownLatch(leechers);
        Diagnostic shutdown = new DiagnosticAdapter() {
            @Override
            public void dataCompleted(Peer peer, DataInfo dataInfo, TransferManager lastTransferManager) {
                countDownLatch.countDown();
            }
        };

        // Setup diagnostic
        Diagnostic combined;
        RecordDiagnostic recordDiagnostic = null;
        PeerChunkCVSDiagnostic peerChunkCVSDiagnostic = null;
        PeerChunkCVSDiagnostic totalChunkCVSDiagnostic = null;

        // If we have to record the data
        if (record) {
            recordDiagnostic = new RecordDiagnostic();
            peerChunkCVSDiagnostic = new PeerChunkCVSDiagnostic();
            totalChunkCVSDiagnostic = new PeerChunkCVSDiagnostic();
            combined = new CombinedDiagnostic(
                    recordDiagnostic,
                    peerChunkCVSDiagnostic,
                    totalChunkCVSDiagnostic,
                    new LoggingDiagnostic(),
                    shutdown);
        } else {
            combined = new CombinedDiagnostic(
                    new LoggingDiagnostic(),
                    shutdown);
        }

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
                    brainFactory.get(),
                    combined,
                    Optional.of(eventLoopGroup)));
        }

        // Wait for init
        Peers.waitForInit(peers);

        // Connect every peer to every other peer
        Peers.connectGrid(peers);

        if (record) {
            // Run diagnostic now
            recordDiagnostic.start();

            // Setup cvs diagnostic
            peerChunkCVSDiagnostic.setTotal(false);
            peerChunkCVSDiagnostic.setDataInfoHash(dataInfo.getHash());
            peerChunkCVSDiagnostic.setPeers(peers);
            peerChunkCVSDiagnostic.writeStatus();

            // Setup cvs diagnostic
            totalChunkCVSDiagnostic.setTotal(true);
            totalChunkCVSDiagnostic.setDataInfoHash(dataInfo.getHash());
            totalChunkCVSDiagnostic.setPeers(peers);
            totalChunkCVSDiagnostic.writeStatus();
        }

        // Stop the time
        Instant first = Instant.now();

        // Await the count down latch to finish
        countDownLatch.await();

        if (record) {
            // Stop diagnostic
            recordDiagnostic.end();

            // Get records and print
            IOUtil.serialize(new File(recordsDirectory, "records.dat"), recordDiagnostic.sortAndGetRecords());

            // Save peer chunks
            Files.write(new File(recordsDirectory, "peerChunks.csv").toPath(),
                    peerChunkCVSDiagnostic.getCVSString().getBytes());

            // Save total chunks
            Files.write(new File(recordsDirectory, "totalChunks.csv").toPath(),
                    totalChunkCVSDiagnostic.getCVSString().getBytes());

            logger.info("[== SERIALIZED RECORDS ==]");
        }

        // Calculate the duration
        Duration duration = Duration.between(first, Instant.now());

        // Print result
        logger.info("[== COMPLETED ==]");
        logger.info("[== THIS SIMULATION NEEDED: " + (duration.toMillis() / 1000.0) + " seconds ==]");

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

