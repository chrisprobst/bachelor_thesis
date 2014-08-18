package de.probst.ba.core;

import de.probst.ba.core.diag.RecordDiagnostic;
import de.probst.ba.core.logic.brains.Brains;
import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.media.databases.DataBases;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.peers.Peers;
import de.probst.ba.core.util.IOUtil;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;

import java.io.IOException;
import java.nio.file.Paths;
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

    public static int n = 4;
    public static CountDownLatch countDownLatch = new CountDownLatch(n);

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {

        // Demo data
        DataInfo dataInfo = new DataInfo(
                0,
                1000 * 5,
                Optional.empty(),
                Optional.empty(),
                "Hello world",
                10,
                String::valueOf)
                .full();
/*
        DataInfo dataInfo1 = new DataInfo(
                1,
                1000 * 20,
                Optional.empty(),
                Optional.empty(),
                "Hello world 2",
                20,
                String::valueOf)
                .full();

        DataInfo dataInfo2 = dataInfo
                .withoutChunk(5)
                .withoutChunk(6);*/

        // List of peers
        List<Peer> peers = new LinkedList<>();

        EventLoopGroup eventLoopGroup = new DefaultEventLoopGroup();

        RecordDiagnostic diagnostic = new RecordDiagnostic();

        // Create both clients
        peers.add(Peers.localPeer(1000, 1000,
                new LocalAddress("peer-0"),
                DataBases.fakeDataBase(dataInfo),
                Brains.logarithmicBrain(),
                diagnostic,
                Optional.of(eventLoopGroup)));

        for (int i = 1; i <= n - 1; i++) {
            peers.add(Peers.localPeer(1000, 1000,
                    new LocalAddress("peer-" + i),
                    DataBases.fakeDataBase(dataInfo.empty()),
                    Brains.logarithmicBrain(),
                    diagnostic,
                    Optional.of(eventLoopGroup)));
        }


        // Wait for init
        Peers.waitForInit(peers);

        // Connect every peer to every other peer
        Peers.connectGrid(peers);

        Instant first = Instant.now();

        countDownLatch.await();

        Duration duration = Duration.between(first, Instant.now());

        System.out.println("==>> READY READY READY! It took: " + duration + ", expected: " + (Math.ceil(Math.log(n) / Math.log(2))) * 5);

        // Get records and print
        IOUtil.serialize(Paths.get("/Users/chrisprobst/Desktop/records.dat"),
                diagnostic.getRecords());
        System.out.println("Serialized records");

        // Wait for close
        Peers.closeAndWait(peers);
    }
}
