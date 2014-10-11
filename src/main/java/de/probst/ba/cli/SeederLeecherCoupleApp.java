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
import de.probst.ba.core.net.http.stream.HttpStreamServer;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.peers.Peers;
import de.probst.ba.core.net.peer.peers.netty.NettyConfig;
import de.probst.ba.core.util.collections.Tuple2;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
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

    private void setupSeederLeecherCouple() throws IOException, ExecutionException, InterruptedException {

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
                Optional.empty(),
                true,
                Optional.of(eventLoopGroup)).get();

        seeder = tuple.first();
        leecher = tuple.second();

        // Connect to host
        leecher.connect(hostArgs.getSocketAddress()).join();

        logger.info(">>> [ Seeder listening on address: " + seeder.getPeerId().getSocketAddress() + " ]");
    }

    @Override
    protected void start() throws Exception {
        // Setup the netty implementation
        NettyConfig.setupConfig(bandwidthArgs.getSmallestBandwidth(),
                                connectionArgs.maxLeecherConnections);

        // Setup seeder-leecher-couple
        setupSeederLeecherCouple();

        // Launch a http server for the database
        if (dataBaseHttpServerArgs.runDataBaseHttpServer) {
            new HttpStreamServer(eventLoopGroup, leecher.getDataBase(), dataBaseHttpServerArgs.httpServerPort);
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
