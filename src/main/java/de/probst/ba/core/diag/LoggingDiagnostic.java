package de.probst.ba.core.diag;

import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.TransferManager;
import de.probst.ba.core.net.peer.Peer;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Map;
import java.util.Optional;

/**
 * Created by chrisprobst on 18.08.14.
 */
public class LoggingDiagnostic implements Diagnostic {

    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(LoggingDiagnostic.class);

    @Override
    public void peerAnnouncedDataInfo(Peer peer, Object remotePeerId, Optional<Map<String, DataInfo>> dataInfo) {
        logger.debug("Peer " + peer.getNetworkState().getLocalAddress() + " announced data to " + remotePeerId + ": " + dataInfo);
    }

    @Override
    public void peerCollectedDataInfo(Peer peer, Object remotePeerId, Optional<Map<String, DataInfo>> dataInfo) {
        logger.debug("Peer " + peer.getNetworkState().getLocalAddress() + " collected data from " + remotePeerId + ": " + dataInfo);
    }

    @Override
    public void peerRejectedUpload(Peer peer, TransferManager transferManager, Throwable cause) {
        logger.info("Peer " + peer.getNetworkState().getLocalAddress() + " rejected upload " + transferManager + ": " + cause);
    }

    @Override
    public void peerStartedUpload(Peer peer, TransferManager transferManager) {
        logger.info("Peer " + peer.getNetworkState().getLocalAddress() + " started upload " + transferManager);
    }

    @Override
    public void peerSucceededUpload(Peer peer, TransferManager transferManager) {
        logger.info("Peer " + peer.getNetworkState().getLocalAddress() + " succeeded upload " + transferManager);
    }

    @Override
    public void peerRequestedDownload(Peer peer, TransferManager transferManager) {
        logger.info("Peer " + peer.getNetworkState().getLocalAddress() + " requested download " + transferManager);
    }

    @Override
    public void peerProgressedDownload(Peer peer, TransferManager transferManager) {
        logger.debug("Peer " + peer.getNetworkState().getLocalAddress() + " progressed download " + transferManager);
    }

    @Override
    public void peerSucceededDownload(Peer peer, TransferManager transferManager) {
        logger.info("Peer " + peer.getNetworkState().getLocalAddress() + " succeeded download " + transferManager);
    }
}
