package de.probst.ba.core;

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
public class App {

    public static int n = 6;


    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
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

        CountDownLatch countDownLatch = new CountDownLatch(n - 1);
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

        // Create both clients
        peers.add(Peers.localPeer(1000, 1000,
                new LocalAddress("P-0"),
                DataBases.fakeDataBase(dataInfo),
                Brains.intelligentBrain(),
                combined,
                Optional.of(eventLoopGroup)));

        for (int i = 1; i <= n - 1; i++) {
            peers.add(Peers.localPeer(1000, 1000,
                    new LocalAddress("P-" + i),
                    DataBases.fakeDataBase(),
                    Brains.intelligentBrain(),
                    combined,
                    Optional.of(eventLoopGroup)));
        }


        // Wait for init
        Peers.waitForInit(peers);

        // Connect every peer to every other peer
        Peers.connectGrid(peers);

        Instant first = Instant.now();

        countDownLatch.await();

        Duration duration = Duration.between(first, Instant.now());

        System.out.println("==>> READY READY READY! It took: " + duration + ", INT expected: " + timePerTransfer * 2 + ", LOG expected: " + (Math.ceil(Math.log(n) / Math.log(2))) * timePerTransfer);

        // Get records and print
        IOUtil.serialize(new File("/Users/chrisprobst/Desktop/records.dat"),
                recordDiagnostic.getRecords());
        System.out.println("Serialized records");

        // Wait for close
        Peers.closeAndWait(peers);


    }

}
