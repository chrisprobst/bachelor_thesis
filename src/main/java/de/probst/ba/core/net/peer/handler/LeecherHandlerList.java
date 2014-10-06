package de.probst.ba.core.net.peer.handler;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Transfer;

import java.net.SocketAddress;
import java.util.Map;
import java.util.Set;

/**
 * Created by chrisprobst on 05.09.14.
 */
public class LeecherHandlerList extends PeerHandlerList implements LeecherPeerHandler {

    @Override
    public LeecherHandlerList add(PeerHandler peerHandler) {
        return (LeecherHandlerList) super.add(peerHandler);
    }

    @Override
    public LeecherHandlerList remove(PeerHandler peerHandler) {
        return (LeecherHandlerList) super.remove(peerHandler);
    }

    @Override
    public void discoveredSocketAddresses(Leecher leecher, Set<SocketAddress> socketAddresses) {
        peerHandlers.stream()
                    .filter(ph -> ph instanceof LeecherPeerHandler)
                    .forEach(ph -> ((LeecherPeerHandler) ph).discoveredSocketAddresses(
                            leecher,
                            socketAddresses));
    }

    @Override
    public void collected(Leecher leecher, PeerId remotePeerId, Map<String, DataInfo> dataInfo) {
        peerHandlers.stream()
                    .filter(ph -> ph instanceof LeecherPeerHandler)
                    .forEach(ph -> ((LeecherPeerHandler) ph).collected(
                            leecher,
                            remotePeerId,
                            dataInfo));
    }

    @Override
    public void downloadRequested(Leecher leecher, Transfer transfer) {
        peerHandlers.stream()
                    .filter(ph -> ph instanceof LeecherPeerHandler)
                    .forEach(ph -> ((LeecherPeerHandler) ph).downloadRequested(
                            leecher,
                            transfer));
    }

    @Override
    public void downloadRejected(Leecher leecher, Transfer transfer, Throwable cause) {
        peerHandlers.stream()
                    .filter(ph -> ph instanceof LeecherPeerHandler)
                    .forEach(ph -> ((LeecherPeerHandler) ph).downloadRejected(
                            leecher,
                            transfer,
                            cause));
    }

    @Override
    public void downloadStarted(Leecher leecher, Transfer transfer) {
        peerHandlers.stream()
                    .filter(ph -> ph instanceof LeecherPeerHandler)
                    .forEach(ph -> ((LeecherPeerHandler) ph).downloadStarted(
                            leecher,
                            transfer));
    }

    @Override
    public void downloadProgressed(Leecher leecher, Transfer transfer) {
        peerHandlers.stream()
                    .filter(ph -> ph instanceof LeecherPeerHandler)
                    .forEach(ph -> ((LeecherPeerHandler) ph).downloadProgressed(
                            leecher,
                            transfer));
    }

    @Override
    public void downloadSucceeded(Leecher leecher, Transfer transfer) {
        peerHandlers.stream()
                    .filter(ph -> ph instanceof LeecherPeerHandler)
                    .forEach(ph -> ((LeecherPeerHandler) ph).downloadSucceeded(
                            leecher,
                            transfer));
    }

    @Override
    public void dataCompleted(Leecher leecher, DataInfo dataInfo, Transfer transfer) {
        peerHandlers.stream()
                    .filter(ph -> ph instanceof LeecherPeerHandler)
                    .forEach(ph -> ((LeecherPeerHandler) ph).dataCompleted(
                            leecher,
                            dataInfo,
                            transfer));
    }
}
