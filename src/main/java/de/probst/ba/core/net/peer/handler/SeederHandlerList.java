package de.probst.ba.core.net.peer.handler;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.transfer.TransferManager;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;

import java.util.Map;
import java.util.Optional;

/**
 * Created by chrisprobst on 05.09.14.
 */
public class SeederHandlerList extends PeerHandlerList implements SeederPeerHandler {

    @Override
    public SeederHandlerList add(Optional<PeerHandler> peerHandler) {
        return (SeederHandlerList) super.add(peerHandler);
    }

    @Override
    public SeederHandlerList remove(Optional<PeerHandler> peerHandler) {
        return (SeederHandlerList) super.remove(peerHandler);
    }

    @Override
    public void discoveredPeer(Seeder seeder, PeerId remotePeerId) {
        peerHandlers.stream()
                    .filter(ph -> ph instanceof SeederPeerHandler)
                    .forEach(ph -> ((SeederPeerHandler) ph).discoveredPeer(
                            seeder,
                            remotePeerId));
    }

    @Override
    public void announced(Seeder seeder, PeerId remotePeerId, Map<String, DataInfo> dataInfo) {
        peerHandlers.stream()
                    .filter(ph -> ph instanceof SeederPeerHandler)
                    .forEach(ph -> ((SeederPeerHandler) ph).announced(seeder,
                                                                      remotePeerId,
                                                                      dataInfo));
    }

    @Override
    public void uploadRejected(Seeder seeder, TransferManager transferManager, Throwable cause) {
        peerHandlers.stream()
                    .filter(ph -> ph instanceof SeederPeerHandler)
                    .forEach(ph -> ((SeederPeerHandler) ph).uploadRejected(seeder, transferManager, cause));
    }

    @Override
    public void uploadStarted(Seeder seeder, TransferManager transferManager) {
        peerHandlers.stream()
                    .filter(ph -> ph instanceof SeederPeerHandler)
                    .forEach(ph -> ((SeederPeerHandler) ph).uploadStarted(seeder, transferManager));
    }

    @Override
    public void uploadSucceeded(Seeder seeder, TransferManager transferManager) {
        peerHandlers.stream()
                    .filter(ph -> ph instanceof SeederPeerHandler)
                    .forEach(ph -> ((SeederPeerHandler) ph).uploadSucceeded(seeder, transferManager));
    }
}
