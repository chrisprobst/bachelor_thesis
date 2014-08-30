package de.probst.ba.core.diag;

import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.TransferManager;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.PeerId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by chrisprobst on 20.08.14.
 */
public final class CombinedDiagnostic implements Diagnostic {

    private final List<Diagnostic> diagnosticList;

    public CombinedDiagnostic(Diagnostic... diagnosticArray) {
        diagnosticList = new ArrayList<>(diagnosticArray.length);
        Collections.addAll(diagnosticList, diagnosticArray);
    }

    @Override
    public void announced(Peer peer, PeerId remotePeerId, Optional<Map<String, DataInfo>> dataInfo) {
        diagnosticList.forEach(d -> d.announced(peer, remotePeerId, dataInfo));
    }

    @Override
    public void collected(Peer peer, PeerId remotePeerId, Optional<Map<String, DataInfo>> dataInfo) {
        diagnosticList.forEach(d -> d.collected(peer, remotePeerId, dataInfo));
    }

    @Override
    public void interestAdded(Peer peer, PeerId remotePeerId, DataInfo addedDataInfo) {
        diagnosticList.forEach(d -> d.interestAdded(peer, remotePeerId, addedDataInfo));
    }

    @Override
    public void uploadRejected(Peer peer, TransferManager transferManager, Throwable cause) {
        diagnosticList.forEach(d -> d.uploadRejected(peer, transferManager, cause));
    }

    @Override
    public void uploadStarted(Peer peer, TransferManager transferManager) {
        diagnosticList.forEach(d -> d.uploadStarted(peer, transferManager));
    }

    @Override
    public void uploadSucceeded(Peer peer, TransferManager transferManager) {
        diagnosticList.forEach(d -> d.uploadSucceeded(peer, transferManager));
    }

    @Override
    public void downloadRequested(Peer peer, TransferManager transferManager) {
        diagnosticList.forEach(d -> d.downloadRequested(peer, transferManager));
    }

    @Override
    public void downloadRejected(Peer peer, TransferManager transferManager, Throwable cause) {
        diagnosticList.forEach(d -> d.downloadRejected(peer, transferManager, cause));
    }

    @Override
    public void downloadStarted(Peer peer, TransferManager transferManager) {
        diagnosticList.forEach(d -> d.downloadStarted(peer, transferManager));
    }

    @Override
    public void downloadProgressed(Peer peer, TransferManager transferManager) {
        diagnosticList.forEach(d -> d.downloadProgressed(peer, transferManager));
    }

    @Override
    public void downloadSucceeded(Peer peer, TransferManager transferManager) {
        diagnosticList.forEach(d -> d.downloadSucceeded(peer, transferManager));
    }

    @Override
    public void dataCompleted(Peer peer, DataInfo dataInfo, TransferManager lastTransferManager) {
        diagnosticList.forEach(d -> d.dataCompleted(peer, dataInfo, lastTransferManager));
    }
}
