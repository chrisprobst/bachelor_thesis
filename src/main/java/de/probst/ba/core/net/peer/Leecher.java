package de.probst.ba.core.net.peer;

import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.net.peer.handler.LeecherHandler;
import de.probst.ba.core.net.peer.state.LeecherDiagnosticState;
import de.probst.ba.core.net.peer.state.LeecherState;

import java.net.SocketAddress;

/**
 * Created by chrisprobst on 01.09.14.
 */
public interface Leecher extends Peer {

    void connect(SocketAddress remoteSocketAddress);

    void leech();

    @Override
    LeecherState getDataInfoState();

    @Override
    LeecherDiagnosticState getDiagnosticState();

    @Override
    LeecherHandler getPeerHandler();

    @Override
    LeecherDistributionAlgorithm getDistributionAlgorithm();
}
