package de.probst.ba.core;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.FileConverter;
import de.probst.ba.core.diagnostic.UploadBandwidthCSV;
import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.distribution.algorithms.Algorithms;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.database.databases.DataBases;
import de.probst.ba.core.media.transfer.TransferManager;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.handler.LeecherAdapter;
import de.probst.ba.core.net.peer.handler.LeecherHandler;
import de.probst.ba.core.net.peer.peers.Peers;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Created by chrisprobst on 12.08.14.
 */
public class Benchmark {

    public static class PeerCountValidator implements IValueValidator<Integer> {

        public static final int MIN = 1;
        public static final int MAX = 16;
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

        public static final int MIN = 0;
        public static final int MAX = 1000 * 1000 * 10;
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
        public static final int MAX = 1000 * 1000 * 1000;
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

    public static class PeerTypeValidator implements IValueValidator<String> {

        public static final String LOCAL = "Local";
        public static final String TCP = "TCP";
        public static final String MSG = "Must be '" + LOCAL + "' or '" + TCP + "'";

        @Override
        public void validate(String name, String value) throws ParameterException {
            if (!value.equals(LOCAL) && !value.equals(TCP)) {
                throw new ParameterException("Parameter " + name + ": "
                        + MSG + " (found: " + value + ")");
            }
        }
    }

    public static class DistributionAlgorithmTypeValidator implements IValueValidator<String> {

        public static final String CHUNKEDSWARM = "chunked-swarm";
        public static final String LOGARITHMIC = "logarithmic";
        public static final String MSG = "Must be '" + CHUNKEDSWARM + "' or '" + LOGARITHMIC + "'";

        @Override
        public void validate(String name, String value) throws ParameterException {
            if (!value.equals(CHUNKEDSWARM) && !value.equals(LOGARITHMIC)) {
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
            names = {"-pt", "--peer-type"},
            description = "Peer type (" + PeerTypeValidator.MSG + ")",
            validateValueWith = PeerTypeValidator.class)
    private String peerTypeString = PeerTypeValidator.LOCAL;

    @Parameter(
            names = {"-da", "--distribution-algorithm"},
            description = "Distribution algorithm type (" + DistributionAlgorithmTypeValidator.MSG + ")",
            validateValueWith = DistributionAlgorithmTypeValidator.class)
    private String distributionAlgorithmType = DistributionAlgorithmTypeValidator.LOGARITHMIC;

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
            description = "(Experimental) The number of parts (" + PartsValidator.MSG + ")",
            validateValueWith = PartsValidator.class)
    private Integer parts = 1;

    @Parameter(
            names = {"-c", "--chunk-count"},
            description = "The number of chunks (" + ChunkCountValidator.MSG + ")",
            validateValueWith = ChunkCountValidator.class)
    private Integer chunkCount = 100;

    @Parameter(
            names = {"-t", "--total-size"},
            description = "The total size in bytes (" + TotalSizeValidator.MSG + ")",
            validateValueWith = TotalSizeValidator.class)
    private Integer totalSize = 1000 * 1000 * 10;

    @Parameter(
            names = {"-u", "--upload-rate"},
            description = "The upload rate in bytes per second, " +
                    "must be less-equal than the download rate (" + TransferRateValidator.MSG + ")",
            validateValueWith = TransferRateValidator.class)
    private Integer uploadRate = 1000 * 1000 * 1;

    @Parameter(
            names = {"-d", "--download-rate"},
            description = "The download rate in bytes per second, " +
                    "must greater-equal than the upload rate (" + TransferRateValidator.MSG + ")",
            validateValueWith = TransferRateValidator.class)
    private Integer downloadRate = 0;

    @Parameter(
            names = {"-s", "--seeders"},
            description = "Number of seeders (" + PeerCountValidator.MSG + ")",
            validateValueWith = PeerCountValidator.class)
    private Integer seeders = 1;

    @Parameter(names = {"-l", "--leechers"},
            description = "Number of leechers (" + PeerCountValidator.MSG + ")",
            validateValueWith = PeerCountValidator.class)
    private Integer leechers = 7;

    private boolean checkParameters(JCommander jCommander) {
        if (showUsage) {
            jCommander.usage();
            return false;
        }

        if (downloadRate != 0 && uploadRate > downloadRate) {
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
        Queue<Peer> peers = new ConcurrentLinkedQueue<>();

        // The event loop group shared by all peers
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

        // Setup diagnostic
        CountDownLatch countDownLatch = new CountDownLatch(leechers * parts);
        LeecherHandler shutdown = new LeecherAdapter() {
            @Override
            public void dataCompleted(Leecher leecher, DataInfo dataInfo, TransferManager lastTransferManager) {
                countDownLatch.countDown();
            }
        };

        // Setup events
        if (recordEvents) {

        }

        UploadBandwidthCSV uploadBandwidthCSV = null;

        // Setup stats
        if (recordStats) {
            Path csvPath = new File(recordsDirectory, distributionAlgorithmType + "TotalUploads.csv").toPath();
            uploadBandwidthCSV = new UploadBandwidthCSV(eventLoopGroup, peers, csvPath, 100, true);
        }

        // Create the algorithm factories
        Supplier<SeederDistributionAlgorithm> seederDistributionAlgorithmSupplier = () ->
                distributionAlgorithmType.equals(DistributionAlgorithmTypeValidator.LOGARITHMIC) ?
                        Algorithms.limitedSeederDistributionAlgorithm(1) :
                        Algorithms.defaultSeederDistributionAlgorithm();
        Supplier<LeecherDistributionAlgorithm> leecherDistributionAlgorithmSupplier = () ->
                distributionAlgorithmType.equals(DistributionAlgorithmTypeValidator.LOGARITHMIC) ?
                        Algorithms.orderedLogarithmicLeecherDistributionAlgorithm() :
                        Algorithms.orderedChunkedSwarmLeecherDistributionAlgorithm();

        // Get the peer type
        Peers.PeerType peerType = Peers.PeerType.valueOf(peerTypeString);

        IntFunction<SocketAddress> seederAddress = i -> {
            if (peerType == Peers.PeerType.TCP) {
                return new InetSocketAddress(10000 + i);
            } else {
                return new LocalAddress("S-" + i);
            }
        };

        IntFunction<SocketAddress> leecherAddress = i -> {
            if (peerType == Peers.PeerType.TCP) {
                return new InetSocketAddress(20000 + i);
            } else {
                return new LocalAddress("L-" + i);
            }
        };

        // Setup all seeders
        for (int i = 0; i < seeders; i++) {
            peers.add(Peers.seeder(
                    peerType,
                    uploadRate,
                    new PeerId(seederAddress.apply(i)),
                    DataBases.fakeDataBase(dataInfo),
                    //DataBases.singleFileDataBase(Paths.get("/Users/chrisprobst/Desktop/data.file"), dataInfo[0]),
                    seederDistributionAlgorithmSupplier.get(),
                    Optional.empty(),
                    Optional.of(eventLoopGroup)));
        }

        // Setup all leechers
        for (int i = 0; i < leechers; i++) {
            // Duplex leechers share PeerId and DataBase
            PeerId peerId = new PeerId(leecherAddress.apply(i));
            DataBase dataBase = DataBases.fakeDataBase();

            // Add the seeder part
            peers.add(Peers.seeder(
                    peerType,
                    uploadRate,
                    peerId,
                    dataBase,
                    //DataBases.singleFileDataBase(Paths.get("/Users/chrisprobst/Desktop/data.file"), dataInfo[0]),
                    seederDistributionAlgorithmSupplier.get(),
                    Optional.empty(),
                    Optional.of(eventLoopGroup)));

            // Add the leecher part
            peers.add(Peers.leecher(
                    peerType,
                    downloadRate,
                    peerId,
                    dataBase,
                    //DataBases.singleFileDataBase(Paths.get("/Users/chrisprobst/Desktop/data.file"), dataInfo[0]),
                    leecherDistributionAlgorithmSupplier.get(),
                    Optional.of(shutdown),
                    Optional.of(eventLoopGroup)));
        }

        // Wait for init
        Peers.waitForInit(peers);

        // Connect every peer to every other peer
        Peers.connectGrid(peers);

        // Start events
        if (recordEvents) {

        }

        // Start stats
        if (recordStats) {
            uploadBandwidthCSV.schedule();
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

        }

        // Stop  stats
        if (recordStats) {
            if (uploadBandwidthCSV != null) {
                uploadBandwidthCSV.close();
            }
        }

        // Save stats
        if (recordStats) {
            /*
            // CSV
            logger.info("[== WRITING STATS ==]");
            timeStamp = Instant.now();

            // Save peer chunks
            Files.write(new File(recordsDirectory, distributionAlgorithmType + "PeerChunks.csv").toPath(),
                    peerChunkCompletionCVSDiagnostic.toString().getBytes());

            // Save total chunks
            Files.write(new File(recordsDirectory, distributionAlgorithmType + "TotalChunks.csv").toPath(),
                    totalChunkCompletionCVSDiagnostic.toString().getBytes());

            // Save peer upload
            Files.write(new File(recordsDirectory, distributionAlgorithmType + "PeerUploads.csv").toPath(),
                    peerUploadCVSDiagnostic.toString().getBytes());

            // Save total upload
            Files.write(new File(recordsDirectory, distributionAlgorithmType + "TotalUploads.csv").toPath(),
                    totalUploadCVSDiagnostic.toString().getBytes());

            duration = Duration.between(timeStamp, Instant.now());
            logger.info("[== DONE IN: " + (duration.toMillis() / 1000.0) + " seconds ==]");*/
        }

        // Save events
        if (recordEvents) {

            /*
            eventRecordDiagnostic.end();

            // Get records and serialize
            logger.info("[== WRITING EVENTS ==]");
            timeStamp = Instant.now();
            IOUtil.serialize(new File(recordsDirectory, distributionAlgorithmType + "Records.dat"), eventRecordDiagnostic.sortAndGetRecords());
            duration = Duration.between(timeStamp, Instant.now());
            logger.info("[== DONE IN: " + (duration.toMillis() / 1000.0) + " seconds ==]");*/
        }

        // Wait for close
        Peers.closeAndWait(peers);

        Thread.sleep(1000);
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

