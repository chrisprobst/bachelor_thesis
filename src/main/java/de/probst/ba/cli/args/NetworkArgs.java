package de.probst.ba.cli.args;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.distribution.algorithms.Algorithms;
import de.probst.ba.core.net.peer.peers.Peers;
import io.netty.channel.local.LocalAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Created by chrisprobst on 10.10.14.
 */
public final class NetworkArgs implements Args {

    public static final int SUPER_SEEDER_LOW_PORT = 10000;
    public static final int SEEDER_LEECHER_LOW_PORT = 20000;

    private final Logger logger = LoggerFactory.getLogger(DataInfoGeneratorArgs.class);

    @Parameter(names = {"-pt", "--peer-type"},
               description = "Peer type [Local, TCP]",
               converter = Converters.PeerTypeConverter.class,
               required = true)
    public Peers.PeerType peerType;

    @Parameter(names = {"-mc", "--max-connections"},
               description = "Maximum number of connections per leecher (" + Validators.MaxConnectionsValidator.MSG +
                             "), 0 means no restriction.",
               validateValueWith = Validators.MaxConnectionsValidator.class)
    public Integer maxConnections = 0;

    @Parameter(names = {"-at", "--algorithm-type"},
               description = "Distribution algorithm type [SuperSeederChunkedSwarm, ChunkedSwarm, Logarithmic, Sequential]",
               converter = Converters.AlgorithmTypeConverter.class,
               required = true)
    public Algorithms.AlgorithmType algorithmType;

    @Parameter(names = {"-bc", "--binary-codec"},
               description = "Use a binary codec for the meta data instead of using refs " +
                             "(This option is implicitly activated if a non-local peer type is used)")
    public Boolean binaryCodec = false;

    @Parameter(names = {"-mp", "--meta-data-size-percentage"},
               description =
                       "The meta data size percentage relative to the chunk size (" +
                       Validators.PercentageValidator.MSG + ") (Ignored if binary codec is activated)",
               validateValueWith = Validators.PercentageValidator.class)
    public Double metaDataSizePercentage = 0.0;


    public SeederDistributionAlgorithm getSuperSeederDistributionAlgorithm() {
        return Algorithms.getSuperSeederOnlyDistributionAlgorithm(algorithmType);
    }

    public SeederDistributionAlgorithm getSeederDistributionAlgorithm() {
        return Algorithms.getSeederDistributionAlgorithm(algorithmType);
    }

    public LeecherDistributionAlgorithm getLeecherDistributionAlgorithm() {
        return Algorithms.getLeecherDistributionAlgorithm(algorithmType);
    }

    public SocketAddress getSuperSeederSocketAddress(int i) {
        if (peerType == Peers.PeerType.TCP) {
            return new InetSocketAddress("0.0.0.0", SUPER_SEEDER_LOW_PORT + i);
        } else {
            return new LocalAddress("SS-" + i);
        }
    }

    public SocketAddress getSeederLeecherCoupleSocketAddress(int i) {
        if (peerType == Peers.PeerType.TCP) {
            return new InetSocketAddress("0.0.0.0", SEEDER_LEECHER_LOW_PORT + i);
        } else {
            return new LocalAddress("SL-" + i);
        }
    }

    @Override
    public boolean check(JCommander jCommander) {
        if (!binaryCodec && peerType != Peers.PeerType.Local) {
            System.out.println("Binary codec implicitly activated (Non-local peer type is used)");
            binaryCodec = true;
        }

        if (binaryCodec && metaDataSizePercentage > 0.0) {
            System.out.println("Ignoring meta data size percentage (Binary codec is activated)");
            metaDataSizePercentage = 0.0;
        }

        logger.info(">>> [ Network Config ]");
        logger.info(">>> Peer type:                 " + peerType);
        logger.info(">>> Algorithm type:            " + algorithmType);
        logger.info(">>> Leecher connection limit:  " + maxConnections);
        logger.info(">>> Meta data size percentage: " + metaDataSizePercentage + " %");
        logger.info(">>> Using codec:               " + binaryCodec);

        return true;
    }
}
