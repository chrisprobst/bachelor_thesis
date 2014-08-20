package de.probst.ba.core.logic;

import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.NetworkState;
import de.probst.ba.core.net.Transfer;
import de.probst.ba.core.net.peer.PeerId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The main brain interface. If you want to
 * implement a custom download logic this is
 * the only interface you have to implement.
 * <p>
 * Created by chrisprobst on 10.08.14.
 */
public interface Brain {

    /**
     * This method is called by the framework internally at
     * undefined intervals and can be triggered by network metrics,
     * timers and events randomly.
     * <p>
     * This method is thread-safe.
     * <p>
     * The purpose of this method is to implement synchronization strategies.
     * In other words: To request downloads in the most effective way.
     * <p>
     * The default implementation does not download anything.
     *
     * @param networkState
     * @return An optional list of download transfers.
     * The framework will take care that you do not download
     * from the same peer in parallel different chunks.
     */
    default Optional<List<Transfer>> process(NetworkState networkState) {
        return Optional.empty();
    }

    /**
     * This method is called by the framework internally at
     * undefined intervals and can be triggered by network metrics,
     * timers and events randomly.
     * <p>
     * This method is called by different threads concurrently, so
     * make sure you have no race conditions.
     * <p>
     * The purpose of this method is to transform the local data info we are willing
     * to upload. This way for instance we could stop announcing data info if
     * we are already uploading the given data. This could help implement the
     * logarithmic strategy where every peer only uploads data to one other peer.
     * <p>
     * The default implementation does not do any transformations.
     *
     * @param networkState
     * @param remotePeerId The remote peer id.
     * @return An optional data info.
     */
    default Optional<Map<String, DataInfo>> transformUploadDataInfo(NetworkState networkState,
                                                                    PeerId remotePeerId) {
        return Optional.of(networkState.getDataInfo());
    }

    default boolean addInterest(PeerId remotePeerId,
                                DataInfo newDataInfo) {
        return true;
    }

    /**
     * Returns the maximum number of active uploads running in
     * parallel.
     * <p>
     * This method is called by different threads concurrently, so
     * make sure you have no race conditions.
     * <p>
     * Default is one.
     * <p>
     * The framework internally already checks that one peer cannot start
     * two downloads in parallel because it would not make much sense.
     * So this number is for distinct peers.
     *
     * @return A positive integer > 0.
     */
    default int getMaxParallelUploads() {
        return 1;
    }
}
