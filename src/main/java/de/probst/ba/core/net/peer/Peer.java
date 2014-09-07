package de.probst.ba.core.net.peer;

import de.probst.ba.core.distribution.DistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.net.peer.handler.PeerHandler;
import de.probst.ba.core.net.peer.state.BandwidthStatisticState;
import de.probst.ba.core.net.peer.state.DataInfoState;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

/**
 * A peer is the base for a seeder or a leecher.
 * It contains all components necessary for both.
 * <p>
 * Created by chrisprobst on 15.08.14.
 */
public interface Peer extends Closeable {

    /**
     * @return The peer id.
     */
    PeerId getPeerId();

    /**
     * @return A completable future which is triggered
     * when this peer is ready for work.
     */
    CompletableFuture<?> getInitFuture();

    /**
     * @return A completable future which is triggered
     * when this peer is completely closed.
     */
    CompletableFuture<?> getCloseFuture();

    /**
     * @return The data info state created
     * by the data base.
     */
    DataInfoState getDataInfoState();

    /**
     * @return The bandwidth statistic state for this peer.
     */
    BandwidthStatisticState getBandwidthStatisticState();

    /**
     * @return The data base for this peer.
     */
    DataBase getDataBase();

    /**
     * @return The peer handler for this peer.
     */
    PeerHandler getPeerHandler();

    /**
     * @return The distribution algorithm for this peer.
     */
    DistributionAlgorithm getDistributionAlgorithm();
}
