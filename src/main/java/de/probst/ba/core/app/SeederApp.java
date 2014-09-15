package de.probst.ba.core.app;

import de.probst.ba.core.media.database.databases.DataBases;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.peers.Peers;

import java.util.Optional;
import java.util.Scanner;

/**
 * Created by chrisprobst on 07.09.14.
 */
public class SeederApp extends AbstractSocketAddressApp {

    private Seeder seeder;

    @Override
    protected void setupPeers() throws Exception {
        seeder = Peers.seeder(peerType,
                              uploadRate,
                              downloadRate,
                              getSocketAddress(),
                              DataBases.fakeDataBase(),
                              getSeederOnlyDistributionAlgorithm(),
                              Optional.ofNullable(recordPeerHandler),
                              Optional.of(eventLoopGroup)).getInitFuture().get();
        dataBaseUpdatePeers.add(seeder);
        uploadBandwidthStatisticPeers.add(seeder);
    }

    @Override
    protected void start() throws Exception {
        setup();

        Scanner scanner = new Scanner(System.in);
        logger.info(">>> [ Press [ENTER] to start seeding ]");
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        } else {
            return;
        }

        setupStart(eventLoopGroup);
        logger.info(">>> [ Seeding on " + seeder.getPeerId() + " ]");
        seeder.getDataInfoState().getDataInfo().forEach((k, v) -> logger.info(">>> [ Announcing " + v + " ]"));

        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }

        setupStop();
        seeder.close();
        seeder.getCloseFuture().get();
    }

    public SeederApp() throws Exception {
    }

    public static void main(String[] args) throws Exception {
        new SeederApp().parse(args);
    }
}
