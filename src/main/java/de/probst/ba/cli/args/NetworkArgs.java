package de.probst.ba.cli.args;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
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

    private final Logger logger = LoggerFactory.getLogger(NetworkArgs.class);

    @Parameter(names = {"-pt", "--peer-type"},
               description = "Peer type [Local, TCP]",
               converter = Converters.PeerTypeConverter.class,
               required = true)
    public Peers.PeerType peerType;

    @Parameter(names = {"-bc", "--binary-codec"},
               description = "Use a binary codec for the meta data instead of using refs " +
                             "(This option is implicitly activated if a non-local peer type is used)")
    public Boolean binaryCodec = false;

    @Parameter(names = {"-mds", "--meta-data-size"},
               description = "The meta data size in bytes (" + Validators.MetaDataSizeValidator.MSG +
                             ") (Ignored if binary codec is activated)",
               validateValueWith = Validators.MetaDataSizeValidator.class)
    public Long metaDataSize = 0L;

    public SocketAddress getSuperSeederSocketAddress(int port) {
        if (peerType == Peers.PeerType.TCP) {
            return new InetSocketAddress("0.0.0.0", port);
        } else {
            return new LocalAddress("SS-" + port);
        }
    }

    public SocketAddress getSeederLeecherCoupleSocketAddress(int port) {
        if (peerType == Peers.PeerType.TCP) {
            return new InetSocketAddress("0.0.0.0", port);
        } else {
            return new LocalAddress("SL-" + port);
        }
    }

    @Override
    public boolean check(JCommander jCommander) {
        if (!binaryCodec && peerType != Peers.PeerType.Local) {
            System.out.println("Binary codec implicitly activated (Non-local peer type is used)");
            binaryCodec = true;
        }

        if (binaryCodec && metaDataSize > 0) {
            System.out.println("Ignoring meta data size percentage (Binary codec is activated)");
            metaDataSize = 0L;
        }

        logger.info(">>> [ Network Config ]");
        logger.info(">>> Peer type:         " + peerType);
        logger.info(">>> Meta data size:    " + metaDataSize + " bytes");
        logger.info(">>> Using codec:       " + binaryCodec);

        return true;
    }
}
