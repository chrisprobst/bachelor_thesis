package de.probst.ba.core.net.peer;

import de.probst.ba.core.distribution.DistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Created by chrisprobst on 15.08.14.
 */
public interface Peer extends Closeable {

    PeerId getPeerId();

    CompletableFuture<?> getInitFuture();

    Future<?> getCloseFuture();

    PeerState getPeerState();

    DataBase getDataBase();

    PeerHandler getPeerHandler();

    DistributionAlgorithm getDistributionAlgorithm();
}
