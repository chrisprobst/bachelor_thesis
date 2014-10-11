package de.probst.ba.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParametersDelegate;
import de.probst.ba.cli.args.ArgsApp;
import de.probst.ba.cli.args.DistributionArgs;
import de.probst.ba.cli.args.FileDataBaseArgs;
import de.probst.ba.cli.args.FileDataInfoGeneratorArgs;
import de.probst.ba.cli.args.HelpArgs;
import de.probst.ba.cli.args.SeederPortArgs;
import de.probst.ba.cli.args.SuperSeederBandwidthArgs;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.peers.Peers;
import de.probst.ba.core.net.peer.peers.netty.NettyConfig;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

/**
 * Created by chrisprobst on 11.10.14.
 */
public final class SuperSeederApp extends ArgsApp {

    // The super seeder logger
    private final Logger logger = LoggerFactory.getLogger(SuperSeederApp.class);

    // The event loop group of the super seeder (1 event-loop is enough)
    private final EventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);

    // The super seeder
    private Seeder superSeeder;

    // The argument delegates
    @ParametersDelegate
    private final HelpArgs helpArgs = new HelpArgs();
    @ParametersDelegate
    private final SeederPortArgs seederPortArgs = new SeederPortArgs();
    @ParametersDelegate
    private final DistributionArgs distributionArgs = new DistributionArgs();
    @ParametersDelegate
    private final SuperSeederBandwidthArgs superSeederBandwidthArgs = new SuperSeederBandwidthArgs();
    @ParametersDelegate
    private final FileDataBaseArgs fileDataBaseArgs = new FileDataBaseArgs();
    @ParametersDelegate
    private final FileDataInfoGeneratorArgs fileDataInfoGeneratorArgs = new FileDataInfoGeneratorArgs();

    private void setupSuperSeeder() throws IOException, ExecutionException, InterruptedException {
        // Instantiate the new super seeder
        superSeeder = Peers.seeder(Peers.PeerType.TCP,
                                   superSeederBandwidthArgs.maxSuperSeederUploadRate,
                                   superSeederBandwidthArgs.maxSuperSeederDownloadRate,
                                   new InetSocketAddress(seederPortArgs.seederPort),
                                   fileDataBaseArgs.getDataBase(),
                                   distributionArgs.getSuperSeederDistributionAlgorithm(),
                                   Optional.empty(),
                                   Optional.of(eventLoopGroup)).getInitFuture().get();

        logger.info(">>> [ Seeder listening on address: " + superSeeder.getPeerId().getSocketAddress() + " ]");
    }

    @Override
    protected void start() throws Exception {
        // Setup the netty implementation
        NettyConfig.setupConfig(superSeederBandwidthArgs.getSmallestBandwidth(),
                                fileDataInfoGeneratorArgs.chunkSize);

        // Setup the super seeder
        setupSuperSeeder();

        // At first generate the file data info, this might take a while
        logger.info(">>> [ Generating data info from file: " + fileDataInfoGeneratorArgs.dataFile + " ]");
        List<DataInfo> fileDataInfo = fileDataInfoGeneratorArgs.generateDataInfo();
        logger.info(">>> [ Generated data info from file: " + fileDataInfoGeneratorArgs.dataFile + " ]");

        // Read for enter
        Scanner scanner = new Scanner(System.in);
        System.out.println("Press [ENTER] to start seeding");
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }

        // Insert file into data base, this also might take a while
        if (!superSeeder.getDataBase()
                        .insertManyFromChannel(fileDataInfo, fileDataInfoGeneratorArgs.openFileChannel(), true)) {
            throw new IllegalStateException("!superSeeder.getDataBase().insertManyFromChannel(fileDataInfo, " +
                                            "fileDataInfoGeneratorArgs.openFileChannel(), true)");
        }

        // Read for enter
        System.out.println("Press [ENTER] to stop seeding");
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }

        // Close and wait
        superSeeder.close();
        superSeeder.getCloseFuture().join();
    }

    @Override
    public boolean check(JCommander jCommander) {

        return helpArgs.check(jCommander) &&
               seederPortArgs.check(jCommander) &&
               distributionArgs.check(jCommander) &&
               superSeederBandwidthArgs.check(jCommander) &&
               fileDataBaseArgs.check(jCommander) &&
               fileDataInfoGeneratorArgs.check(jCommander);
    }

    public static void main(String[] args) throws Exception {
        new SuperSeederApp().parse(args);
    }
}
