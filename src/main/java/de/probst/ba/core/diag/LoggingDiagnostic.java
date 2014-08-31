package de.probst.ba.core.diag;

import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.TransferManager;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.PeerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by chrisprobst on 18.08.14.
 */
public final class LoggingDiagnostic implements Diagnostic {

    private final Logger logger =
            LoggerFactory.getLogger(LoggingDiagnostic.class);

    private final int totalDownloads;
    private final AtomicInteger currentDownloads = new AtomicInteger();

    public LoggingDiagnostic(int totalDownloads) {
        this.totalDownloads = totalDownloads;
    }

    @Override
    public void announced(Peer peer, PeerId remotePeerId, Optional<Map<String, DataInfo>> dataInfo) {
        logger.debug("Peer " + peer.getLocalPeerId() + " announced data to " + remotePeerId + ": " + dataInfo);
    }

    @Override
    public void collected(Peer peer, PeerId remotePeerId, Optional<Map<String, DataInfo>> dataInfo) {
        logger.debug("Peer " + peer.getLocalPeerId() + " collected data from " + remotePeerId + ": " + dataInfo);
    }

    @Override
    public void interestAdded(Peer peer, PeerId remotePeerId, DataInfo addedDataInfo) {
        logger.info("Peer " + peer.getLocalPeerId() + " added interest for " + addedDataInfo + " from " + remotePeerId);
    }

    @Override
    public void uploadRejected(Peer peer, TransferManager transferManager, Throwable cause) {
        logger.debug("Peer " + peer.getLocalPeerId() + " rejected upload " + transferManager + ", cause: " + cause);
    }

    @Override
    public void uploadStarted(Peer peer, TransferManager transferManager) {
        logger.debug("Peer " + peer.getLocalPeerId() + " started upload " + transferManager);
    }

    @Override
    public void uploadSucceeded(Peer peer, TransferManager transferManager) {
        logger.debug("Peer " + peer.getLocalPeerId() + " succeeded upload " + transferManager);
    }

    @Override
    public void downloadRequested(Peer peer, TransferManager transferManager) {
        logger.debug("Peer " + peer.getLocalPeerId() + " requested download " + transferManager);
    }

    @Override
    public void downloadRejected(Peer peer, TransferManager transferManager, Throwable cause) {
        logger.info("Peer " + peer.getLocalPeerId() + " requested the download " + transferManager +
                ", but was rejected");
    }

    @Override
    public void downloadStarted(Peer peer, TransferManager transferManager) {
        logger.debug("Peer " + peer.getLocalPeerId() + " started download " + transferManager);
    }

    @Override
    public void downloadProgressed(Peer peer, TransferManager transferManager) {
        logger.debug("Peer " + peer.getLocalPeerId() + " progressed download " + transferManager);
    }

    @Override
    public void downloadSucceeded(Peer peer, TransferManager transferManager) {
        logger.debug("Peer " + peer.getLocalPeerId() + " succeeded download " + transferManager);

        int remainingDownloads = totalDownloads - currentDownloads.incrementAndGet();
        if (remainingDownloads % 10 == 0) {
            logger.info(remainingDownloads + " chunks missing");
        }
    }

    @Override
    public void dataCompleted(Peer peer, DataInfo dataInfo, TransferManager lastTransferManager) {
        logger.info("Peer " + peer.getLocalPeerId() + " completed the data " + dataInfo + " with " + lastTransferManager);
    }
}
