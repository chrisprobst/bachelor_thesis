package de.probst.ba.core;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.probst.ba.core.diag.CombinedDiagnostic;
import de.probst.ba.core.diag.Diagnostic;
import de.probst.ba.core.diag.DiagnosticAdapter;
import de.probst.ba.core.diag.LoggingDiagnostic;
import de.probst.ba.core.diag.RecordDiagnostic;
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

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

/**
 * Created by chrisprobst on 12.08.14.
 */
public class Benchmark {

    @Parameter(names = {"-v", "-verbose"}, description = "Verbose mode")
    private Boolean verbose = false;

    @Parameter(names = "-s", description = "Number of seeders", required = true)
    private Integer seeders = 1;

    @Parameter(names = "-l", description = "Number of leechers", required = true)
    private Integer leechers = 5;

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {

        // Parse commands
        Benchmark benchmark = new Benchmark();
        JCommander jCommander = new JCommander(benchmark);
        jCommander.parse(args);

        // SETUP LOGGING
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "INFO");
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());


        int timePerTransfer = 20;

        // Demo data
        DataInfo dataInfo = new DataInfo(
                0,
                40 * 500,
                Optional.empty(),
                Optional.empty(),
                "Hello world",
                40,
                String::valueOf)
                .full();

        // List of peers
        List<Peer> peers = new LinkedList<>();

        EventLoopGroup eventLoopGroup = new DefaultEventLoopGroup();

        CountDownLatch countDownLatch = new CountDownLatch(benchmark.leechers);
        Diagnostic shutdown = new DiagnosticAdapter() {
            @Override
            public void dataCompleted(Peer peer, DataInfo dataInfo, TransferManager lastTransferManager) {
                countDownLatch.countDown();
            }
        };
        RecordDiagnostic recordDiagnostic = new RecordDiagnostic();
        Diagnostic combined = new CombinedDiagnostic(
                recordDiagnostic,
                new LoggingDiagnostic(),
                shutdown);


        for (int i = 0; i < benchmark.seeders; i++) {
            peers.add(Peers.localPeer(1000, 1000,
                    new LocalAddress("S-" + i),
                    DataBases.fakeDataBase(dataInfo),
                    Brains.intelligentBrain(),
                    combined,
                    Optional.of(eventLoopGroup)));
        }

        for (int i = 0; i < benchmark.leechers; i++) {
            peers.add(Peers.localPeer(1000, 1000,
                    new LocalAddress("L-" + i),
                    DataBases.fakeDataBase(),
                    Brains.intelligentBrain(),
                    combined,
                    Optional.of(eventLoopGroup)));
        }


        // Wait for init
        Peers.waitForInit(peers);

        // Connect every peer to every other peer
        Peers.connectGrid(peers);
        recordDiagnostic.start();

        Instant first = Instant.now();

        countDownLatch.await();
        recordDiagnostic.end();

        Duration duration = Duration.between(first, Instant.now());

        System.out.println("==>> READY READY READY! It took: " + duration + ", INT expected: " + timePerTransfer * 2 + ", LOG expected: " + (Math.ceil(Math.log(benchmark.leechers + benchmark.seeders) / Math.log(2))) * timePerTransfer);

        // Get records and print
        IOUtil.serialize(new File("/Users/chrisprobst/Desktop/records.dat"),
                recordDiagnostic.sortAndGetRecords());
        System.out.println("Serialized records");

        // Wait for close
        Peers.closeAndWait(peers);


    }

}
