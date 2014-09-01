package de.probst.ba.core.diagnostic;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.transfer.TransferManager;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;
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
    public void announced(Seeder leecher, PeerId remotePeerId, Optional<Map<String, DataInfo>> dataInfo) {
        logger.debug("Seeder " + leecher.getPeerId() + " announced data to " + remotePeerId + ": " + dataInfo);
    }

    @Override
    public void collected(Leecher leecher, PeerId remotePeerId, Optional<Map<String, DataInfo>> dataInfo) {
        logger.debug("Leecher " + leecher.getPeerId() + " collected data from " + remotePeerId + ": " + dataInfo);
    }

    @Override
    public void interestAdded(Leecher leecher, PeerId remotePeerId, DataInfo addedDataInfo) {
        logger.info("Leecher " + leecher.getPeerId() + " added interest for " + addedDataInfo + " from " + remotePeerId);
    }

    @Override
    public void uploadRejected(Seeder leecher, TransferManager transferManager, Throwable cause) {
        logger.debug("Seeder " + leecher.getPeerId() + " rejected upload " + transferManager + ", cause: " + cause);
    }

    @Override
    public void uploadStarted(Seeder leecher, TransferManager transferManager) {
        logger.debug("Seeder " + leecher.getPeerId() + " started upload " + transferManager);
    }

    @Override
    public void uploadSucceeded(Seeder leecher, TransferManager transferManager) {
        logger.debug("Seeder " + leecher.getPeerId() + " succeeded upload " + transferManager);
    }

    @Override
    public void downloadRequested(Leecher leecher, TransferManager transferManager) {
        logger.debug("Leecher " + leecher.getPeerId() + " requested download " + transferManager);
    }

    @Override
    public void downloadRejected(Leecher leecher, TransferManager transferManager, Throwable cause) {
        logger.info("Leecher " + leecher.getPeerId() + " requested the download " + transferManager +
                ", but was rejected");
    }

    @Override
    public void downloadStarted(Leecher leecher, TransferManager transferManager) {
        logger.debug("Leecher " + leecher.getPeerId() + " started download " + transferManager);
    }

    @Override
    public void downloadProgressed(Leecher leecher, TransferManager transferManager) {
        logger.debug("Leecher " + leecher.getPeerId() + " progressed download " + transferManager);
    }

    @Override
    public void downloadSucceeded(Leecher leecher, TransferManager transferManager) {
        logger.debug("Leecher " + leecher.getPeerId() + " succeeded download " + transferManager);

        int remainingDownloads = totalDownloads - currentDownloads.incrementAndGet();
        if (remainingDownloads % 10 == 0) {
            logger.info(remainingDownloads + " chunks missing");
        }
    }

    @Override
    public void dataCompleted(Leecher leecher, DataInfo dataInfo, TransferManager lastTransferManager) {
        logger.info("Leecher " + leecher.getPeerId() + " completed the data " + dataInfo + " with " + lastTransferManager);
    }
}
