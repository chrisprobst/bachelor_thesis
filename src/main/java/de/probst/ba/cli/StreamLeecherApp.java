package de.probst.ba.cli;

import com.beust.jcommander.Parameter;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.database.databases.DataBases;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.handler.LeecherHandlerList;
import de.probst.ba.core.net.peer.handler.LeecherPeerHandler;
import de.probst.ba.core.net.peer.handler.LeecherPeerHandlerAdapter;
import de.probst.ba.core.net.peer.handler.handlers.DataInfoCompletionHandler;
import de.probst.ba.core.net.peer.peers.Peers;
import de.probst.ba.core.net.peer.transfer.TransferManager;
import de.probst.ba.core.util.collections.Tuple2;
import de.probst.ba.gui.StreamerGUI;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by chrisprobst on 08.09.14.
 */
public class StreamLeecherApp extends AbstractSocketAddressApp {

    @Parameter(names = {"-ep", "--external-port"},
               description = "External port of the seeder (" + PortValidator.MSG + ")",
               validateValueWith = PortValidator.class,
               required = true)
    protected Integer externalPort;

    @Parameter(names = {"-eh", "--external-host-name"},
               description = "External host name of the seeder",
               converter = HostNameConverter.class,
               required = true)
    protected InetAddress externalHostName;

    private LeecherHandlerList leecherHandlerList;
    private DataInfoCompletionHandler dataInfoCompletionHandler;
    private LeecherPeerHandler streamHandler;
    private Seeder seeder;
    private Leecher leecher;

    @Override
    protected void setupPeerHandlers() {
        dataInfoCompletionHandler = new DataInfoCompletionHandler(parts);
        streamHandler = new LeecherPeerHandlerAdapter() {

            volatile int i = 0;

            @Override
            public void dataCompleted(Leecher leecher, DataInfo dataInfo, TransferManager lastTransferManager) {
                if (dataInfo.getId() == i) {
                    System.out.println(" ********  START STREAMING ********");
                    eventLoopGroup.schedule(new Runnable() {
                        @Override
                        public void run() {
                            i++;
                            DataInfo di = leecher.getDataInfoState()
                                                 .getDataInfo()
                                                 .values()
                                                 .stream()
                                                 .filter(d -> d.getId() == i)
                                                 .findFirst()
                                                 .orElse(null);

                            if (di.isCompleted()) {
                                System.out.println(" ********  FAST ENOUGH, CONTINUE STREAMING ********");
                                eventLoopGroup.schedule(this, 20000, TimeUnit.MILLISECONDS);
                            } else {
                                System.out.println(" ******** BUG BUG BUG BUG ********");
                            }
                        }
                    }, 20000, TimeUnit.MILLISECONDS);
                }
            }
        };
        leecherHandlerList = new LeecherHandlerList();
        leecherHandlerList.add(streamHandler);
        leecherHandlerList.add(dataInfoCompletionHandler);
        if (recordPeerHandler != null) {
            leecherHandlerList.add(recordPeerHandler);
        }
    }


    protected InetSocketAddress getExternalSocketAddress() {
        return new InetSocketAddress(externalHostName, externalPort);
    }

    @Override
    protected void setupPeers() throws Exception {
        Tuple2<Seeder, Leecher> tuple = Peers.initSeederAndLeecher(peerType,
                                                                   uploadRate,
                                                                   downloadRate,
                                                                   getSocketAddress(), DataBases.fakeDataBase(),
                                                                   getSeederDistributionAlgorithm(),
                                                                   getLeecherDistributionAlgorithm(),
                                                                   Optional.ofNullable(recordPeerHandler),
                                                                   Optional.of(leecherHandlerList),
                                                                   true,
                                                                   Optional.of(eventLoopGroup)).get();

        seeder = tuple.first();
        leecher = tuple.second();

        downloadBandwidthStatisticPeers.add(leecher);
        uploadBandwidthStatisticPeers.add(seeder);
    }

    @Override
    protected void start() throws Exception {
        setup();

        StreamerGUI gui = new StreamerGUI(leecher.getDataBase(), 20000);
        Scanner scanner = new Scanner(System.in);

        // Connect to external seeder and wait
        leecher.connect(getExternalSocketAddress()).get();
        logger.info(">>> [ Connected to " + getExternalSocketAddress() + " ]");

        Thread.sleep(3000);

        logger.info(">>> [ Press [ENTER] to start leeching and timers ]");
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        } else {
            return;
        }

        setupStart(eventLoopGroup);
        gui.startTimers();
        logger.info(">>> [ Leeching ]");

        dataInfoCompletionHandler.getCountDownLatch().await();
        setupStop(Instant.now());
        logger.info(">>> [ Press [ENTER] to stop seeding ]");

        if (scanner.hasNextLine()) {
            scanner.nextLine();
        } else {
            return;
        }

        logger.info(">>> [ Shutting down ]");

        seeder.close();
        leecher.close();
        CompletableFuture.allOf(seeder.getCloseFuture(), leecher.getCloseFuture()).get();
    }

    public StreamLeecherApp() throws IOException {
    }

    public static void main(String[] args) throws Exception {
        new StreamLeecherApp().parse(args);
    }
}
