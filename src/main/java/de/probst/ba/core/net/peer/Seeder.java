package de.probst.ba.core.net.peer;

import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.net.peer.handler.SeederPeerHandler;
import de.probst.ba.core.net.peer.state.SeederDataInfoState;

/**
 * A seeder is a peer which is connected
 * to a couple of leechers to serve their
 * needs based on the distribution algorithm.
 * <p>
 * Created by chrisprobst on 01.09.14.
 */
public interface Seeder extends Peer {

    /**
     * @return The seeder data info state.
     */
    @Override
    SeederDataInfoState getDataInfoState();

    /**
     * @return The seeder peer handler.
     */
    @Override
    SeederPeerHandler getPeerHandler();

    /**
     * @return The seeder distribution algorithm.
     */
    @Override
    SeederDistributionAlgorithm getDistributionAlgorithm();
}
