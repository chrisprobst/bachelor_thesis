package de.probst.ba.core.net.peer;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.transfer.TransferManager;

import java.util.Map;
import java.util.Optional;

/**
 * Created by chrisprobst on 01.09.14.
 */
public interface SeederHandler extends PeerHandler {

    void announced(Seeder seeder,
                   PeerId remotePeerId,
                   Optional<Map<String, DataInfo>> dataInfo);

    void uploadRejected(Seeder seeder,
                        TransferManager transferManager,
                        Throwable cause);

    void uploadStarted(Seeder seeder,
                       TransferManager transferManager);

    void uploadSucceeded(Seeder seeder,
                         TransferManager transferManager);
}
