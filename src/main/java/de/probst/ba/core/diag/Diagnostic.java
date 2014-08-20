package de.probst.ba.core.diag;

import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.TransferManager;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.PeerId;

import java.util.Map;
import java.util.Optional;

/**
 * Created by chrisprobst on 18.08.14.
 */
public interface Diagnostic {

    // DATAINFO

    void announced(Peer peer,
                   PeerId remotePeerId,
                   Optional<Map<String, DataInfo>> dataInfo);

    void collected(Peer peer,
                   PeerId remotePeerId,
                   Optional<Map<String, DataInfo>> dataInfo);

    void interestAdded(Peer peer,
                       PeerId remotePeerId,
                       DataInfo addedDataInfo);

    // UPLOAD

    void uploadRejected(Peer peer,
                        TransferManager transferManager,
                        Throwable cause);

    void uploadStarted(Peer peer,
                       TransferManager transferManager);

    void uploadSucceeded(Peer peer,
                         TransferManager transferManager);

    // DOWNLOAD

    void downloadRequested(Peer peer,
                           TransferManager transferManager);

    void downloadRejected(Peer peer,
                          TransferManager transferManager,
                          Throwable cause);

    void downloadStarted(Peer peer,
                         TransferManager transferManager);

    void downloadProgressed(Peer peer,
                            TransferManager transferManager);

    void downloadSucceeded(Peer peer,
                           TransferManager transferManager);

    void downloadFailed(Peer peer,
                        TransferManager transferManager,
                        Throwable cause);

    void dataCompleted(Peer peer,
                       DataInfo dataInfo,
                       TransferManager lastTransferManager);
}
