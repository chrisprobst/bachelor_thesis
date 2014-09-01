package de.probst.ba.core.diagnostic;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.transfer.TransferManager;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;

import java.util.Map;
import java.util.Optional;

/**
 * Created by chrisprobst on 20.08.14.
 */
public class DiagnosticAdapter implements Diagnostic {

    @Override
    public void collected(Leecher leecher, PeerId remotePeerId, Optional<Map<String, DataInfo>> dataInfo) {

    }

    @Override
    public void interestAdded(Leecher leecher, PeerId remotePeerId, DataInfo addedDataInfo) {

    }

    @Override
    public void downloadRequested(Leecher leecher, TransferManager transferManager) {

    }

    @Override
    public void downloadRejected(Leecher leecher, TransferManager transferManager, Throwable cause) {

    }

    @Override
    public void downloadStarted(Leecher leecher, TransferManager transferManager) {

    }

    @Override
    public void downloadProgressed(Leecher leecher, TransferManager transferManager) {

    }

    @Override
    public void downloadSucceeded(Leecher leecher, TransferManager transferManager) {

    }

    @Override
    public void dataCompleted(Leecher leecher, DataInfo dataInfo, TransferManager lastTransferManager) {

    }

    @Override
    public void announced(Seeder seeder, PeerId remotePeerId, Optional<Map<String, DataInfo>> dataInfo) {

    }

    @Override
    public void uploadRejected(Seeder seeder, TransferManager transferManager, Throwable cause) {

    }

    @Override
    public void uploadStarted(Seeder seeder, TransferManager transferManager) {

    }

    @Override
    public void uploadSucceeded(Seeder seeder, TransferManager transferManager) {

    }
}
