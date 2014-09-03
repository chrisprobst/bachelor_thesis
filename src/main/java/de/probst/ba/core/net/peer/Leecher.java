package de.probst.ba.core.net.peer;

import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.net.peer.handler.LeecherHandler;
import de.probst.ba.core.net.peer.state.LeecherDataInfoState;
import de.probst.ba.core.net.peer.state.LeecherStatisticState;

import java.net.SocketAddress;

/**
 * Created by chrisprobst on 01.09.14.
 */
public interface Leecher extends Peer {

    void connect(SocketAddress remoteSocketAddress);

    void leech();

    @Override
    LeecherDataInfoState getDataInfoState();

    @Override
    LeecherStatisticState getStatisticState();

    @Override
    LeecherHandler getPeerHandler();

    @Override
    LeecherDistributionAlgorithm getDistributionAlgorithm();
}
