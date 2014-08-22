package de.probst.ba.core.diag;

import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.TransferManager;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.PeerId;

import java.util.Map;
import java.util.Optional;

/**
 * Created by chrisprobst on 20.08.14.
 */
public class DiagnosticAdapter implements Diagnostic {
    @Override
    public void announced(Peer peer, PeerId remotePeerId, Optional<Map<String, DataInfo>> dataInfo) {

    }

    @Override
    public void collected(Peer peer, PeerId remotePeerId, Optional<Map<String, DataInfo>> dataInfo) {

    }

    @Override
    public void interestAdded(Peer peer, PeerId remotePeerId, DataInfo addedDataInfo) {

    }

    @Override
    public void uploadRejected(Peer peer, TransferManager transferManager, Throwable cause) {

    }

    @Override
    public void uploadStarted(Peer peer, TransferManager transferManager) {

    }

    @Override
    public void uploadSucceeded(Peer peer, TransferManager transferManager) {

    }

    @Override
    public void downloadRequested(Peer peer, TransferManager transferManager) {

    }

    @Override
    public void downloadRejected(Peer peer, TransferManager transferManager, Throwable cause) {

    }

    @Override
    public void downloadStarted(Peer peer, TransferManager transferManager) {

    }

    @Override
    public void downloadProgressed(Peer peer, TransferManager transferManager) {

    }

    @Override
    public void downloadSucceeded(Peer peer, TransferManager transferManager) {

    }

    @Override
    public void downloadFailed(Peer peer, TransferManager transferManager, Throwable cause) {

    }

    @Override
    public void dataCompleted(Peer peer, DataInfo dataInfo, TransferManager lastTransferManager) {

    }
}
