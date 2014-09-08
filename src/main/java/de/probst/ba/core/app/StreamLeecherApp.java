package de.probst.ba.core.app;

import com.beust.jcommander.Parameter;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.database.databases.DataBases;
import de.probst.ba.core.media.transfer.TransferManager;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.handler.LeecherHandlerList;
import de.probst.ba.core.net.peer.handler.LeecherPeerHandler;
import de.probst.ba.core.net.peer.handler.LeecherPeerHandlerAdapter;
import de.probst.ba.core.net.peer.handler.handlers.DataInfoCompletionHandler;
import de.probst.ba.core.net.peer.peers.Peers;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
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

    private DataInfoCompletionHandler dataInfoCompletionHandler;
    private LeecherPeerHandler streamHandler = new LeecherPeerHandlerAdapter() {

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
                            eventLoopGroup.schedule(this, 7000, TimeUnit.MILLISECONDS);
                        } else {
                            System.out.println(" ******** BUG BUG BUG BUG ********");
                        }
                    }
                }, 7000, TimeUnit.MILLISECONDS);
            }
        }
    };
    private volatile Leecher leecher;

    @Override
    protected void setupPeerHandlers() {
        dataInfoCompletionHandler = new DataInfoCompletionHandler(parts);
    }


    protected InetSocketAddress getExternalSocketAddress() {
        return new InetSocketAddress(externalHostName, externalPort);
    }

    @Override
    protected void setupLeechers() {
        // Add the seeder part
        Seeder seeder = Peers.seeder(peerType,
                                     uploadRate,
                                     downloadRate,
                                     getSocketAddress(),
                                     DataBases.fakeDataBase(),
                                     getSeederDistributionAlgorithm(),
                                     Optional.ofNullable(recordPeerHandler),
                                     Optional.of(eventLoopGroup));
        seeder.getInitFuture().thenAccept(s -> {
            LeecherHandlerList leecherHandlerList = new LeecherHandlerList();
            leecherHandlerList.add(streamHandler);
            leecherHandlerList.add(dataInfoCompletionHandler);
            if (recordPeerHandler != null) {
                leecherHandlerList.add(recordPeerHandler);
            }

            // Add the leecher part
            leecher = Peers.leecher(peerType,
                                    uploadRate,
                                    downloadRate,
                                    Optional.of(s.getPeerId()),
                                    s.getDataBase(),
                                    getLeecherDistributionAlgorithm(),
                                    Optional.of(leecherHandlerList),
                                    true,
                                    Optional.of(eventLoopGroup),
                                    s.getPeerId().getSocketAddress());

            downloadBandwidthStatisticPeers.add(leecher);
            initClosePeerQueue.add(leecher);
        });


        uploadBandwidthStatisticPeers.add(seeder);
        initClosePeerQueue.add(seeder);
    }

    @Override
    protected void start() throws Exception {
        setup();
        initPeers();

        Scanner scanner = new Scanner(System.in);
        logger.info("[== Press [ENTER] to start leeching ==]");
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        } else {
            return;
        }

        // Connect to external seeder and wait
        CompletableFuture<Leecher> fut = leecher.connect(getExternalSocketAddress());
        fut.join();

        setupStart(eventLoopGroup);
        logger.info("[== Leeching ==]");
        dataInfoCompletionHandler.getCountDownLatch().await();
        setupStop();
        logger.info("[== Press [ENTER] to stop seeding ==]");

        if (scanner.hasNextLine()) {
            scanner.nextLine();
        } else {
            return;
        }
        logger.info("[== Shutting down ==]");

        closePeers();
    }

    public StreamLeecherApp() throws IOException {
    }

    public static void main(String[] args) throws Exception {
        new StreamLeecherApp().parse(args);
    }
}
