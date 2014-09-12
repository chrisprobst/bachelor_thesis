package de.probst.ba.core.net.peer;

import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.net.peer.handler.SeederPeerHandler;
import de.probst.ba.core.net.peer.state.SeederDataInfoState;

import java.util.concurrent.CompletableFuture;

/**
 * A seeder is a peer which is connected
 * to a couple of leechers to serve their
 * needs based on the distribution algorithm.
 * <p>
 * Created by chrisprobst on 01.09.14.
 */
public interface Seeder extends Peer {

    /**
     * Announces the local data info.
     * <p>
     * Please not, that this method is already called
     * by the framework internally at undefined intervals
     * and can be triggered by network metrics, timers and
     * events randomly.
     * <p>
     * Please only call this method if you think that you are
     * smarter than the framework because running the algorithm
     * is not for free.
     */
    void announce();

    @Override
    CompletableFuture<Seeder> getCloseFuture();

    @Override
    CompletableFuture<Seeder> getInitFuture();

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
