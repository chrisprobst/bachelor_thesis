package de.probst.ba.core.net.peer.handler;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.transfer.TransferManager;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.PeerId;

import java.net.SocketAddress;
import java.util.Map;
import java.util.Set;

/**
 * Created by chrisprobst on 03.09.14.
 */
public class LeecherPeerHandlerAdapter implements LeecherPeerHandler {

    @Override
    public void discoveredSocketAddresses(Leecher leecher, Set<SocketAddress> socketAddresses) {

    }

    @Override
    public void collected(Leecher leecher, PeerId remotePeerId, Map<String, DataInfo> dataInfo) {

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
}
