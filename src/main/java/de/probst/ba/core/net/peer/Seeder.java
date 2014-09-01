package de.probst.ba.core.net.peer;

import de.probst.ba.core.distribution.SeederDistributionAlgorithm;

/**
 * Created by chrisprobst on 01.09.14.
 */
public interface Seeder extends Peer {

    @Override
    SeederState getPeerState();

    @Override
    SeederHandler getPeerHandler();

    @Override
    SeederDistributionAlgorithm getDistributionAlgorithm();
}
