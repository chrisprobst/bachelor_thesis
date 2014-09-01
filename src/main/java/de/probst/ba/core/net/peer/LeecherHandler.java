package de.probst.ba.core.net.peer;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.transfer.TransferManager;

import java.util.Map;
import java.util.Optional;

/**
 * Created by chrisprobst on 01.09.14.
 */
public interface LeecherHandler extends PeerHandler {

    void collected(Leecher leecher,
                   PeerId remotePeerId,
                   Optional<Map<String, DataInfo>> dataInfo);

    void interestAdded(Leecher leecher,
                       PeerId remotePeerId,
                       DataInfo addedDataInfo);

    void downloadRequested(Leecher leecher,
                           TransferManager transferManager);

    void downloadRejected(Leecher leecher,
                          TransferManager transferManager,
                          Throwable cause);

    void downloadStarted(Leecher leecher,
                         TransferManager transferManager);

    void downloadProgressed(Leecher leecher,
                            TransferManager transferManager);

    void downloadSucceeded(Leecher leecher,
                           TransferManager transferManager);

    void dataCompleted(Leecher leecher,
                       DataInfo dataInfo,
                       TransferManager lastTransferManager);
}
