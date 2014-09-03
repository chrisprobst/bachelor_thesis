package de.probst.ba.core.net.peer;

import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.net.peer.handler.SeederHandler;
import de.probst.ba.core.net.peer.state.SeederDiagnosticState;
import de.probst.ba.core.net.peer.state.SeederState;

/**
 * Created by chrisprobst on 01.09.14.
 */
public interface Seeder extends Peer {

    @Override
    SeederState getDataInfoState();

    @Override
    SeederHandler getPeerHandler();

    @Override
    SeederDiagnosticState getDiagnosticState();

    @Override
    SeederDistributionAlgorithm getDistributionAlgorithm();
}
