package de.probst.ba.core.net.peer;

import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.net.peer.handler.SeederHandler;
import de.probst.ba.core.net.peer.state.SeederDataInfoState;
import de.probst.ba.core.net.peer.state.SeederStatisticState;

/**
 * Created by chrisprobst on 01.09.14.
 */
public interface Seeder extends Peer {

    @Override
    SeederDataInfoState getDataInfoState();

    @Override
    SeederHandler getPeerHandler();

    @Override
    SeederStatisticState getStatisticState();

    @Override
    SeederDistributionAlgorithm getDistributionAlgorithm();
}
