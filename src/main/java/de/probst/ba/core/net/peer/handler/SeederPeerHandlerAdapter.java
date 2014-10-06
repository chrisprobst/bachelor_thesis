package de.probst.ba.core.net.peer.handler;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.Transfer;

import java.net.SocketAddress;
import java.util.Map;

/**
 * Created by chrisprobst on 03.09.14.
 */
public class SeederPeerHandlerAdapter implements SeederPeerHandler {

    @Override
    public void discoveredSocketAddress(Seeder seeder, SocketAddress remoteSocketAddress) {

    }

    @Override
    public void announced(Seeder seeder, PeerId remotePeerId, Map<String, DataInfo> dataInfo) {

    }

    @Override
    public void uploadRejected(Seeder seeder, Transfer transfer, Throwable cause) {

    }

    @Override
    public void uploadStarted(Seeder seeder, Transfer transfer) {

    }

    @Override
    public void uploadSucceeded(Seeder seeder, Transfer transfer) {

    }
}
