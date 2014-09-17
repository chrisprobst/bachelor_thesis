package de.probst.ba.core.net.peer;

import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.net.peer.handler.LeecherPeerHandler;
import de.probst.ba.core.net.peer.state.LeecherDataInfoState;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * A leecher is a peer which is connected
 * to a specific number of seeders and tries to
 * download data based on the distribution algorithm.
 * <p>
 * Created by chrisprobst on 01.09.14.
 */
public interface Leecher extends Peer {

    /**
     * Try to connect to the given peer.
     *
     * @param socketAddress
     * @return
     */
    CompletableFuture<Leecher> connect(SocketAddress socketAddress);

    Map<SocketAddress, Boolean> getConnections();

    /**
     * @return True if this leecher automatically connects to
     * new discovered peers or not.
     */
    boolean isAutoConnect();

    @Override
    CompletableFuture<Leecher> getCloseFuture();

    @Override
    CompletableFuture<Leecher> getInitFuture();

    /**
     * @return The leecher data info state.
     */
    @Override
    LeecherDataInfoState getDataInfoState();

    /**
     * @return The leecher peer handler.
     */
    @Override
    LeecherPeerHandler getPeerHandler();

    /**
     * @return The leecher distribution algorithm.
     */
    @Override
    LeecherDistributionAlgorithm getDistributionAlgorithm();
}
