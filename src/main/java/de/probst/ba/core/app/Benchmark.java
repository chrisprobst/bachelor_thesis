package de.probst.ba.core.app;

import com.beust.jcommander.Parameter;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.database.databases.DataBases;
import de.probst.ba.core.media.transfer.TransferManager;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.handler.LeecherHandlerList;
import de.probst.ba.core.net.peer.handler.LeecherPeerAdapter;
import de.probst.ba.core.net.peer.handler.LeecherPeerHandler;
import de.probst.ba.core.net.peer.peers.Peers;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.nio.NioEventLoopGroup;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

/**
 * Created by chrisprobst on 12.08.14.
 */
public class Benchmark extends AbstractPeerApp {

    @Parameter(names = {"-s", "--seeders"},
               description = "Number of seeders (" + PeerCountValidator.MSG + ")",
               validateValueWith = PeerCountValidator.class)
    private Integer seeders = 1;
    @Parameter(names = {"-l", "--leechers"},
               description = "Number of leechers (" + PeerCountValidator.MSG + ")",
               validateValueWith = PeerCountValidator.class)
    private Integer leechers = 7;

    private final Queue<Peer> peerQueue = new ConcurrentLinkedQueue<>();
    private final Queue<Peer> seederQueue = new ConcurrentLinkedQueue<>();
    private final Queue<Peer> allSeederQueue = new ConcurrentLinkedQueue<>();
    private final Queue<Peer> leecherQueue = new ConcurrentLinkedQueue<>();
    private final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
    private CountDownLatch countDownLatch;
    private LeecherPeerHandler shutdown;
    private DataInfo[] dataInfo;

    private void setupDataInfo() {
        dataInfo = IntStream.range(0, parts)
                            .mapToObj(i -> new DataInfo(i,
                                                        totalSize,
                                                        Optional.empty(),
                                                        Optional.empty(),
                                                        "Benchmark hash, part: " + i,
                                                        chunkCount,
                                                        String::valueOf).full())
                            .toArray(DataInfo[]::new);
    }

    private void setupCountDownLatch() {
        countDownLatch = new CountDownLatch(leechers * parts);
        shutdown = new LeecherPeerAdapter() {
            @Override
            public void dataCompleted(Leecher leecher, DataInfo dataInfo, TransferManager lastTransferManager) {

                countDownLatch.countDown();
            }
        };
    }

    private SocketAddress getSeederSocketAddress(int i) {
        if (peerType == Peers.PeerType.TCP) {
            return new InetSocketAddress(10000 + i);
        } else {
            return new LocalAddress("S-" + i);
        }
    }

    private SocketAddress getLeecherSocketAddress(int i) {
        if (peerType == Peers.PeerType.TCP) {
            return new InetSocketAddress(20000 + i);
        } else {
            return new LocalAddress("L-" + i);
        }
    }

    private void setupSeeders() {
        // Setup all seeders
        for (int i = 0; i < seeders; i++) {
            Seeder seeder = Peers.seeder(peerType,
                                         uploadRate,
                                         downloadRate,
                                         new PeerId(getSeederSocketAddress(i)),
                                         DataBases.fakeDataBase(),
                                         getSeederDistributionAlgorithm(),
                                         Optional.ofNullable(recordPeerHandler),
                                         Optional.of(eventLoopGroup));

            seederQueue.add(seeder);
            allSeederQueue.add(seeder);
            peerQueue.add(seeder);
        }
    }

    private void updateSeederDataBases() {
        seederQueue.forEach(s -> Arrays.stream(dataInfo).forEach(s.getDataBase()::update));
    }

    private void setupLeechers() {

        // Setup all leechers
        for (int i = 0; i < leechers; i++) {
            // Duplex leechers share PeerId and DataBase
            PeerId peerId = new PeerId(getLeecherSocketAddress(i));
            DataBase dataBase = DataBases.fakeDataBase();

            // Add the seeder part
            Seeder seeder = Peers.seeder(peerType,
                                         uploadRate,
                                         downloadRate,
                                         peerId,
                                         dataBase,
                                         getSeederDistributionAlgorithm(),
                                         Optional.ofNullable(recordPeerHandler),
                                         Optional.of(eventLoopGroup));


            // Add the leecher part
            Leecher leecher = Peers.leecher(peerType,
                                            uploadRate,
                                            downloadRate,
                                            peerId,
                                            dataBase,
                                            getLeecherDistributionAlgorithm(),
                                            Optional.of(new LeecherHandlerList().add(Optional.of(shutdown))
                                                                                .add(Optional.ofNullable(
                                                                                        recordPeerHandler))),
                                            true,
                                            Optional.of(eventLoopGroup),
                                            Optional.of(peerId));

            leecherQueue.add(leecher);
            allSeederQueue.add(seeder);
            peerQueue.add(seeder);
            peerQueue.add(leecher);
        }
    }

    @Override
    protected void start() throws Exception {
        setupVerbosity();

        // Setup status flags
        double timePerTransfer = totalSize / uploadRate;

        // Server/Client like
        double dumbTime = timePerTransfer / seeders * leechers;

        // ~ who knows ?!
        double chunkSwarmTime = timePerTransfer / seeders * 1.3;

        // log(n) - log(s) = log(n / s) = log((s + l) / s) = log(1 + l/s)
        double logarithmicTime =
                timePerTransfer * Math.ceil(Math.log(1 + leechers / (double) seeders) / Math.log(2));

        // A small info for all waiters
        logger.info("[== One transfer needs approx.: " + timePerTransfer + " seconds ==]");
        logger.info("[== A dumb algorithm needs approx.: " + dumbTime + " seconds ==]");
        logger.info("[== A Logarithmic algorithm needs approx.: " + logarithmicTime + " seconds ==]");
        logger.info("[== A ChunkedSwarm algorithm needs approx.: " + chunkSwarmTime + " seconds ==]");

        setupCountDownLatch();
        setupDataInfo();
        setupRecords(eventLoopGroup, allSeederQueue, leecherQueue);
        setupSeeders();
        setupLeechers();

        // Wait for init
        Peers.waitForInit(peerQueue);

        // Connect every peer to every other peer
        seederQueue.stream().map(Peer::getPeerId).forEach(p -> Peers.connectTo(leecherQueue, p));

        // Start benchmark
        Thread.sleep(2000);
        Instant timeStamp = Instant.now();
        startRecords();
        logger.info("[== Starting benchmark ==]");
        updateSeederDataBases();

        // Await the count down latch to finish
        countDownLatch.await();

        // Calculate the duration
        Duration duration = Duration.between(timeStamp, Instant.now());

        // Print result
        logger.info("[== COMPLETED IN: " + (duration.toMillis() / 1000.0) + " seconds ==]");

        flushRecords();

        // Wait for close
        Peers.closeAndWait(peerQueue);

        Thread.sleep(1000);
    }

    public static void main(String[] args) throws Exception {
        new Benchmark().parse(args);
    }
}

