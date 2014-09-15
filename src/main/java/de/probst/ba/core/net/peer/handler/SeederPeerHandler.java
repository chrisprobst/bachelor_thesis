package de.probst.ba.core.net.peer.handler;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.transfer.TransferManager;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;

import java.net.SocketAddress;
import java.util.Map;

/**
 * Created by chrisprobst on 01.09.14.
 */
public interface SeederPeerHandler extends PeerHandler {

    void discoveredSocketAddress(Seeder seeder, SocketAddress remoteSocketAddress);

    void announced(Seeder seeder, PeerId remotePeerId, Map<String, DataInfo> dataInfo);

    void uploadRejected(Seeder seeder, TransferManager transferManager, Throwable cause);

    void uploadStarted(Seeder seeder, TransferManager transferManager);

    void uploadSucceeded(Seeder seeder, TransferManager transferManager);
}
