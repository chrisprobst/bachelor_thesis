package de.probst.ba.core.diag;

import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.TransferManager;
import de.probst.ba.core.net.peer.Peer;

import java.util.Map;
import java.util.Optional;

/**
 * Created by chrisprobst on 18.08.14.
 */
public interface Diagnostic {

    // DATAINFO

    void peerAnnouncedDataInfo(Peer peer,
                               Object remotePeerId,
                               Optional<Map<String, DataInfo>> dataInfo);

    void peerCollectedDataInfo(Peer peer,
                               Object remotePeerId,
                               Optional<Map<String, DataInfo>> dataInfo);

    // UPLOAD

    void peerRejectedUpload(Peer peer,
                            TransferManager transferManager,
                            Throwable cause);

    void peerStartedUpload(Peer peer,
                           TransferManager transferManager);

    void peerSucceededUpload(Peer peer,
                             TransferManager transferManager);

    // DOWNLOAD

    void peerRequestedDownload(Peer peer,
                               TransferManager transferManager);

    void peerProgressedDownload(Peer peer,
                                TransferManager transferManager);

    void peerSucceededDownload(Peer peer,
                               TransferManager transferManager);
}
