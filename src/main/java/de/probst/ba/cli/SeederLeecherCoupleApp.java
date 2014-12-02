package de.probst.ba.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParametersDelegate;
import de.probst.ba.cli.args.ArgsApp;
import de.probst.ba.cli.args.BandwidthArgs;
import de.probst.ba.cli.args.ConnectionArgs;
import de.probst.ba.cli.args.DataBaseHttpServerArgs;
import de.probst.ba.cli.args.DistributionArgs;
import de.probst.ba.cli.args.FileDataBaseArgs;
import de.probst.ba.cli.args.HelpArgs;
import de.probst.ba.cli.args.HostArgs;
import de.probst.ba.cli.args.SeederPortArgs;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.httpserver.httpservers.HttpServers;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.Transfer;
import de.probst.ba.core.net.peer.handler.LeecherPeerHandler;
import de.probst.ba.core.net.peer.handler.LeecherPeerHandlerAdapter;
import de.probst.ba.core.net.peer.peers.Peers;
import de.probst.ba.core.net.peer.peers.netty.NettyConfig;
import de.probst.ba.core.util.collections.Tuple2;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

/**
 * Created by chrisprobst on 11.10.14.
 */
public final class SeederLeecherCoupleApp extends ArgsApp {

    // The seeder-leecher-couple logger
    private final Logger logger = LoggerFactory.getLogger(SeederLeecherCoupleApp.class);

    // The event loop group of the seeder-leecher-couple (1 event-loop is enough)
    private final EventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);

    // The seeder-leecher-couple
    private Seeder seeder;
    private Leecher leecher;

    // The argument delegates
    @ParametersDelegate
    private final HelpArgs helpArgs = new HelpArgs();
    @ParametersDelegate
    private final SeederPortArgs seederPortArgs = new SeederPortArgs();
    @ParametersDelegate
    private final DistributionArgs distributionArgs = new DistributionArgs();
    @ParametersDelegate
    private final ConnectionArgs connectionArgs = new ConnectionArgs();
    @ParametersDelegate
    private final BandwidthArgs bandwidthArgs = new BandwidthArgs();
    @ParametersDelegate
    private final FileDataBaseArgs fileDataBaseArgs = new FileDataBaseArgs();
    @ParametersDelegate
    private final HostArgs hostArgs = new HostArgs();
    @ParametersDelegate
    private final DataBaseHttpServerArgs dataBaseHttpServerArgs = new DataBaseHttpServerArgs();

    private void setupSeederLeecherCouple(boolean openUrlFirstTime)
            throws IOException, ExecutionException, InterruptedException {

        LeecherPeerHandler handler = new LeecherPeerHandlerAdapter() {
            boolean once = false;

            @Override
            public synchronized void dataCompleted(Leecher leecher, DataInfo dataInfo, Transfer transfer) {
                if (!once) {
                    once = true;

                    try {
                        Desktop.getDesktop()
                               .browse(new URI("http://localhost:" + dataBaseHttpServerArgs.httpServerPort +
                                               "/stream?name=otis.mp4"));
                    } catch (IOException | URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        // Instantiate new seeder-leecher couple
        Tuple2<Seeder, Leecher> tuple = Peers.initSeederAndLeecher(
                Peers.PeerType.TCP,
                bandwidthArgs.maxUploadRate,
                bandwidthArgs.maxDownloadRate,
                new InetSocketAddress(seederPortArgs.seederPort),
                fileDataBaseArgs.getDataBase(),
                distributionArgs.getSeederDistributionAlgorithm(),
                distributionArgs.getLeecherDistributionAlgorithm(),
                Optional.empty(),
                openUrlFirstTime ? Optional.of(handler) : Optional.empty(),
                true,
                Optional.of(eventLoopGroup)).get();

        seeder = tuple.first();
        leecher = tuple.second();

        // Connect to host
        logger.info(">>> [ Leecher connecting to address: " + hostArgs.getSocketAddress() + " ]");
        leecher.connect(hostArgs.getSocketAddress()).join();

        logger.info(">>> [ Seeder listening on address: " + seeder.getPeerId().getSocketAddress() + " ]");
    }

    @Override
    protected void start() throws Exception {
        // Setup the netty implementation
        NettyConfig.setUseAutoConnect(true);
        NettyConfig.setupConfig(bandwidthArgs.getSmallestBandwidth(),
                                connectionArgs.maxLeecherConnections);

        // Setup seeder-leecher-couple
        setupSeederLeecherCouple(dataBaseHttpServerArgs.runDataBaseHttpServer);

        // Launch a http server for the database
        if (dataBaseHttpServerArgs.runDataBaseHttpServer) {
            HttpServers.defaultHttpServer(eventLoopGroup, leecher.getDataBase(), dataBaseHttpServerArgs.httpServerPort);
        }

        // Read for enter
        System.out.println("Press [ENTER] to close leecher");
        Scanner scanner = new Scanner(System.in);
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }

        // Close and wait
        seeder.close();
        leecher.close();
        seeder.getCloseFuture().join();
        leecher.getCloseFuture().join();
    }

    @Override
    public boolean check(JCommander jCommander) {
        return helpArgs.check(jCommander) &&
               seederPortArgs.check(jCommander) &&
               distributionArgs.check(jCommander) &&
               bandwidthArgs.check(jCommander) &&
               fileDataBaseArgs.check(jCommander) &&
               hostArgs.check(jCommander) &&
               dataBaseHttpServerArgs.check(jCommander);
    }

    public static void main(String[] args) throws Exception {
        new SeederLeecherCoupleApp().parse(args);
    }
}
