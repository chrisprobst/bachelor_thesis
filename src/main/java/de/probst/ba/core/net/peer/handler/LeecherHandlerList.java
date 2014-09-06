package de.probst.ba.core.net.peer.handler;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.transfer.TransferManager;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.PeerId;

import java.util.Map;
import java.util.Optional;

/**
 * Created by chrisprobst on 05.09.14.
 */
public class LeecherHandlerList extends PeerHandlerList implements LeecherPeerHandler {

    @Override
    public LeecherHandlerList add(Optional<PeerHandler> peerHandler) {
        return (LeecherHandlerList) super.add(peerHandler);
    }

    @Override
    public LeecherHandlerList remove(Optional<PeerHandler> peerHandler) {
        return (LeecherHandlerList) super.remove(peerHandler);
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
    public void lookingFor(Leecher leecher, DataInfo addedDataInfo) {
        peerHandlers.stream()
                    .filter(ph -> ph instanceof LeecherPeerHandler)
                    .forEach(ph -> ((LeecherPeerHandler) ph).lookingFor(leecher, addedDataInfo));
    }

    @Override
    public void downloadRequested(Leecher leecher, TransferManager transferManager) {
        peerHandlers.stream()
                    .filter(ph -> ph instanceof LeecherPeerHandler)
                    .forEach(ph -> ((LeecherPeerHandler) ph).downloadRequested(
                            leecher,
                            transferManager));
    }

    @Override
    public void downloadRejected(Leecher leecher, TransferManager transferManager, Throwable cause) {
        peerHandlers.stream()
                    .filter(ph -> ph instanceof LeecherPeerHandler)
                    .forEach(ph -> ((LeecherPeerHandler) ph).downloadRejected(
                            leecher,
                            transferManager,
                            cause));
    }

    @Override
    public void downloadStarted(Leecher leecher, TransferManager transferManager) {
        peerHandlers.stream()
                    .filter(ph -> ph instanceof LeecherPeerHandler)
                    .forEach(ph -> ((LeecherPeerHandler) ph).downloadStarted(
                            leecher,
                            transferManager));
    }

    @Override
    public void downloadProgressed(Leecher leecher, TransferManager transferManager) {
        peerHandlers.stream()
                    .filter(ph -> ph instanceof LeecherPeerHandler)
                    .forEach(ph -> ((LeecherPeerHandler) ph).downloadProgressed(
                            leecher,
                            transferManager));
    }

    @Override
    public void downloadSucceeded(Leecher leecher, TransferManager transferManager) {
        peerHandlers.stream()
                    .filter(ph -> ph instanceof LeecherPeerHandler)
                    .forEach(ph -> ((LeecherPeerHandler) ph).downloadSucceeded(
                            leecher,
                            transferManager));
    }

    @Override
    public void dataCompleted(Leecher leecher, DataInfo dataInfo, TransferManager lastTransferManager) {
        peerHandlers.stream()
                    .filter(ph -> ph instanceof LeecherPeerHandler)
                    .forEach(ph -> ((LeecherPeerHandler) ph).dataCompleted(
                            leecher,
                            dataInfo,
                            lastTransferManager));
    }
}
