package de.probst.ba.core.net.peer.handler;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.Transfer;

import java.net.SocketAddress;
import java.util.Map;

/**
 * Created by chrisprobst on 05.09.14.
 */
public class SeederHandlerList extends PeerHandlerList implements SeederPeerHandler {

    @Override
    public SeederHandlerList add(PeerHandler peerHandler) {
        return (SeederHandlerList) super.add(peerHandler);
    }

    @Override
    public SeederHandlerList remove(PeerHandler peerHandler) {
        return (SeederHandlerList) super.remove(peerHandler);
    }

    @Override
    public void discoveredSocketAddress(Seeder seeder, SocketAddress remoteSocketAddress) {
        peerHandlers.stream()
                    .filter(ph -> ph instanceof SeederPeerHandler)
                    .forEach(ph -> ((SeederPeerHandler) ph).discoveredSocketAddress(
                            seeder,
                            remoteSocketAddress));
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
    public void uploadRejected(Seeder seeder, Transfer transfer, Throwable cause) {
        peerHandlers.stream()
                    .filter(ph -> ph instanceof SeederPeerHandler)
                    .forEach(ph -> ((SeederPeerHandler) ph).uploadRejected(seeder, transfer, cause));
    }

    @Override
    public void uploadStarted(Seeder seeder, Transfer transfer) {
        peerHandlers.stream()
                    .filter(ph -> ph instanceof SeederPeerHandler)
                    .forEach(ph -> ((SeederPeerHandler) ph).uploadStarted(seeder, transfer));
    }

    @Override
    public void uploadSucceeded(Seeder seeder, Transfer transfer) {
        peerHandlers.stream()
                    .filter(ph -> ph instanceof SeederPeerHandler)
                    .forEach(ph -> ((SeederPeerHandler) ph).uploadSucceeded(seeder, transfer));
    }
}
