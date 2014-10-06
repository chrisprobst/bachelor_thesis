package de.probst.ba.core.net.peer.handler;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Transfer;

import java.net.SocketAddress;
import java.util.Map;
import java.util.Set;

/**
 * Created by chrisprobst on 01.09.14.
 */
public interface LeecherPeerHandler extends PeerHandler {

    void discoveredSocketAddresses(Leecher leecher, Set<SocketAddress> socketAddresses);

    void collected(Leecher leecher, PeerId remotePeerId, Map<String, DataInfo> dataInfo);

    void downloadRequested(Leecher leecher, Transfer transfer);

    void downloadRejected(Leecher leecher, Transfer transfer, Throwable cause);

    void downloadStarted(Leecher leecher, Transfer transfer);

    void downloadProgressed(Leecher leecher, Transfer transfer);

    void downloadSucceeded(Leecher leecher, Transfer transfer);

    void dataCompleted(Leecher leecher, DataInfo dataInfo, Transfer transfer);
}
