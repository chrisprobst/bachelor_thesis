package de.probst.ba.core.logic;

import java.util.Map;

/**
 * Created by chrisprobst on 10.08.14.
 */
public interface Logic {

    /**
     * This method is called by the framework internally at
     * undefined intervals and can be triggered by network metrics,
     * timers and events randomly.
     * <p>
     * The purpose of this method is to implement synchronization strategies.
     * In other words: To request downloads in the most effective way.
     *
     * @param network
     */
    void process(Network network);

    /**
     * This method is called by the framework internally at
     * undefined intervals and can be triggered by network metrics,
     * timers and events randomly.
     * <p>
     * The purpose of this method is to transform the data info we are willing
     * to upload. This way for instance we could stop announcing data info if
     * we are already uploading the given data. This could help implement the
     * logarithmic strategy where every peer only uploads data to one other peer.
     * <p>
     * The default implementation does not do any transformations.
     *
     * @param networkState
     * @return
     */
    default Map<String, DataInfo> transformUploadDataInfo(NetworkState networkState) {
        return networkState.getDataInfo();
    }

    /**
     * This is an interceptor method to implement the possibility to
     * control the number of parallel uploads.
     * <p>
     * The framework internally already checks that one peer cannot start
     * two downloads in parallel because it would not make much sense.
     * <p>
     * The default implementation always returns true so every distinct peer
     * will be served.
     *
     * @param peerId
     * @param dataInfo
     * @return
     */
    default boolean isUploadAllowed(NetworkState networkState,
                                    long peerId,
                                    DataInfo dataInfo) {
        return true;
    }
}
