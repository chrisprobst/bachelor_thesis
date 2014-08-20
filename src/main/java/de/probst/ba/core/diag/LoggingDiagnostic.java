package de.probst.ba.core.diag;

import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.TransferManager;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.PeerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

/**
 * Created by chrisprobst on 18.08.14.
 */
public class LoggingDiagnostic implements Diagnostic {

    private static final Logger logger =
            LoggerFactory.getLogger(LoggingDiagnostic.class);

    @Override
    public void announced(Peer peer, PeerId remotePeerId, Optional<Map<String, DataInfo>> dataInfo) {
        logger.debug("Peer " + peer.getNetworkState().getLocalPeerId() + " announced data to " + remotePeerId + ": " + dataInfo);
    }

    @Override
    public void collected(Peer peer, PeerId remotePeerId, Optional<Map<String, DataInfo>> dataInfo) {
        logger.debug("Peer " + peer.getNetworkState().getLocalPeerId() + " collected data from " + remotePeerId + ": " + dataInfo);
    }

    @Override
    public void uploadRejected(Peer peer, TransferManager transferManager, Throwable cause) {
        logger.debug("Peer " + peer.getNetworkState().getLocalPeerId() + " rejected upload " + transferManager + ", cause: " + cause);
    }

    @Override
    public void uploadStarted(Peer peer, TransferManager transferManager) {
        logger.debug("Peer " + peer.getNetworkState().getLocalPeerId() + " started upload " + transferManager);
    }

    @Override
    public void uploadSucceeded(Peer peer, TransferManager transferManager) {
        logger.debug("Peer " + peer.getNetworkState().getLocalPeerId() + " succeeded upload " + transferManager);
    }

    @Override
    public void downloadRequested(Peer peer, TransferManager transferManager) {
        logger.debug("Peer " + peer.getNetworkState().getLocalPeerId() + " requested download " + transferManager);
    }

    @Override
    public void downloadRejected(Peer peer, TransferManager transferManager, Throwable cause) {
        logger.debug("Peer " + peer.getNetworkState().getLocalPeerId() + " requested the download " + transferManager +
                ", but was rejected");
    }

    @Override
    public void downloadStarted(Peer peer, TransferManager transferManager) {
        logger.debug("Peer " + peer.getNetworkState().getLocalPeerId() + " started download " + transferManager);
    }

    @Override
    public void downloadProgressed(Peer peer, TransferManager transferManager) {
        logger.debug("Peer " + peer.getNetworkState().getLocalPeerId() + " progressed download " + transferManager);
    }

    @Override
    public void downloadSucceeded(Peer peer, TransferManager transferManager) {
        logger.debug("Peer " + peer.getNetworkState().getLocalPeerId() + " succeeded download " + transferManager);
    }

    @Override
    public void downloadFailed(Peer peer, TransferManager transferManager, Throwable cause) {
        logger.info("Peer " + peer.getNetworkState().getLocalPeerId() + " failed download " + transferManager + ", cause: " + cause);
    }

    @Override
    public void dataCompleted(Peer peer, DataInfo dataInfo, TransferManager lastTransferManager) {
        logger.info("Peer " + peer.getNetworkState().getLocalPeerId() + " completed the data " + dataInfo + " with " + lastTransferManager);
    }
}
