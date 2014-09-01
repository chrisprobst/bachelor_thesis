package de.probst.ba.core.diagnostic;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.transfer.TransferManager;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;

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
    public void announced(Seeder seeder, PeerId remotePeerId, Optional<Map<String, DataInfo>> dataInfo) {
        diagnosticList.forEach(d -> d.announced(seeder, remotePeerId, dataInfo));
    }

    @Override
    public void collected(Leecher leecher, PeerId remotePeerId, Optional<Map<String, DataInfo>> dataInfo) {
        diagnosticList.forEach(d -> d.collected(leecher, remotePeerId, dataInfo));
    }

    @Override
    public void interestAdded(Leecher leecher, PeerId remotePeerId, DataInfo addedDataInfo) {
        diagnosticList.forEach(d -> d.interestAdded(leecher, remotePeerId, addedDataInfo));
    }

    @Override
    public void uploadRejected(Seeder seeder, TransferManager transferManager, Throwable cause) {
        diagnosticList.forEach(d -> d.uploadRejected(seeder, transferManager, cause));
    }

    @Override
    public void uploadStarted(Seeder seeder, TransferManager transferManager) {
        diagnosticList.forEach(d -> d.uploadStarted(seeder, transferManager));
    }

    @Override
    public void uploadSucceeded(Seeder seeder, TransferManager transferManager) {
        diagnosticList.forEach(d -> d.uploadSucceeded(seeder, transferManager));
    }

    @Override
    public void downloadRequested(Leecher leecher, TransferManager transferManager) {
        diagnosticList.forEach(d -> d.downloadRequested(leecher, transferManager));
    }

    @Override
    public void downloadRejected(Leecher leecher, TransferManager transferManager, Throwable cause) {
        diagnosticList.forEach(d -> d.downloadRejected(leecher, transferManager, cause));
    }

    @Override
    public void downloadStarted(Leecher leecher, TransferManager transferManager) {
        diagnosticList.forEach(d -> d.downloadStarted(leecher, transferManager));
    }

    @Override
    public void downloadProgressed(Leecher leecher, TransferManager transferManager) {
        diagnosticList.forEach(d -> d.downloadProgressed(leecher, transferManager));
    }

    @Override
    public void downloadSucceeded(Leecher leecher, TransferManager transferManager) {
        diagnosticList.forEach(d -> d.downloadSucceeded(leecher, transferManager));
    }

    @Override
    public void dataCompleted(Leecher leecher, DataInfo dataInfo, TransferManager lastTransferManager) {
        diagnosticList.forEach(d -> d.dataCompleted(leecher, dataInfo, lastTransferManager));
    }
}
