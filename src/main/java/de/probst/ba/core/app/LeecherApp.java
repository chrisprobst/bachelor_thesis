package de.probst.ba.core.app;

import com.beust.jcommander.Parameter;
import de.probst.ba.core.media.database.databases.DataBases;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.handler.LeecherHandlerList;
import de.probst.ba.core.net.peer.handler.handlers.DataInfoCompletionHandler;
import de.probst.ba.core.net.peer.peers.Peers;
import de.probst.ba.core.util.collections.Tuple2;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

/**
 * Created by chrisprobst on 08.09.14.
 */
public class LeecherApp extends AbstractSocketAddressApp {

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
    private Seeder seeder;
    private Leecher leecher;

    @Override
    protected void setupPeerHandlers() {
        dataInfoCompletionHandler = new DataInfoCompletionHandler(parts);
        leecherHandlerList = new LeecherHandlerList();
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
        Scanner scanner = new Scanner(System.in);

        // Connect to external seeder and wait
        leecher.connect(getExternalSocketAddress()).get();
        logger.info(">>> [ Connected to " + getExternalSocketAddress() + " ]");

        setupStart(eventLoopGroup);
        logger.info(">>> [ Leeching ]");

        dataInfoCompletionHandler.getCountDownLatch().await();
        setupStop();

        logger.info(">>> [ Press [ENTER] to stop seeding ]");

        if (scanner.hasNextLine()) {
            scanner.nextLine();
        } else {
            return;
        }
        logger.info(">>> [ Shutting down ]");

        seeder.close();
        leecher.close();
        CompletableFuture.allOf(seeder.getCloseFuture(), leecher.getCloseFuture()).get();
    }

    public LeecherApp() throws IOException {
    }

    public static void main(String[] args) throws Exception {
        new LeecherApp().parse(args);
    }
}
