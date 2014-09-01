package de.probst.ba.core.net.peer;

import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;

import java.net.SocketAddress;

/**
 * Created by chrisprobst on 01.09.14.
 */
public interface Leecher extends Peer {

    void connect(SocketAddress remoteSocketAddress);

    @Override
    LeecherState getPeerState();

    @Override
    LeecherHandler getPeerHandler();

    @Override
    LeecherDistributionAlgorithm getDistributionAlgorithm();
}
