package de.probst.ba.core.diag;

import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.TransferManager;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.PeerId;
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
    public void peerAnnouncedDataInfo(Peer peer, PeerId remotePeerId, Optional<Map<String, DataInfo>> dataInfo) {
        logger.debug("Peer " + peer.getNetworkState().getLocalPeerId() + " announced data to " + remotePeerId + ": " + dataInfo);
    }

    @Override
    public void peerCollectedDataInfo(Peer peer, PeerId remotePeerId, Optional<Map<String, DataInfo>> dataInfo) {
        logger.debug("Peer " + peer.getNetworkState().getLocalPeerId() + " collected data from " + remotePeerId + ": " + dataInfo);
    }

    @Override
    public void peerRejectedUpload(Peer peer, TransferManager transferManager, Throwable cause) {
        logger.debug("Peer " + peer.getNetworkState().getLocalPeerId() + " rejected upload " + transferManager + ", cause: " + cause);
    }

    @Override
    public void peerStartedUpload(Peer peer, TransferManager transferManager) {
        logger.debug("Peer " + peer.getNetworkState().getLocalPeerId() + " started upload " + transferManager);
    }

    @Override
    public void peerSucceededUpload(Peer peer, TransferManager transferManager) {
        logger.debug("Peer " + peer.getNetworkState().getLocalPeerId() + " succeeded upload " + transferManager);
    }

    @Override
    public void peerRequestedDownload(Peer peer, TransferManager transferManager) {
        logger.debug("Peer " + peer.getNetworkState().getLocalPeerId() + " requested download " + transferManager);
    }

    @Override
    public void peerRejectedDownload(Peer peer, TransferManager transferManager, Throwable cause) {
        logger.info("Peer " + peer.getNetworkState().getLocalPeerId() + " requested the download " + transferManager +
                ", but was rejected");
    }

    @Override
    public void peerStartedDownload(Peer peer, TransferManager transferManager) {
        logger.info("Peer " + peer.getNetworkState().getLocalPeerId() + " started download " + transferManager);
    }

    @Override
    public void peerProgressedDownload(Peer peer, TransferManager transferManager) {
        logger.debug("Peer " + peer.getNetworkState().getLocalPeerId() + " progressed download " + transferManager);
    }

    @Override
    public void peerSucceededDownload(Peer peer, TransferManager transferManager) {
        logger.info("Peer " + peer.getNetworkState().getLocalPeerId() + " succeeded download " + transferManager);
    }

    @Override
    public void peerFailedDownload(Peer peer, TransferManager transferManager, Throwable cause) {
        logger.info("Peer " + peer.getNetworkState().getLocalPeerId() + " failed download " + transferManager + ", cause: " + cause);
    }
}
