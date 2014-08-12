package de.probst.ba.core.logic;

import java.util.Map;

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
     * The purpose of this method is to implement synchronization strategies.
     * In other words: To request downloads in the most effective way.
     *
     * @param peer
     */
    void process(Peer peer);

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
     * @param peerState
     * @param remotePeerId The remote peer id.
     * @return
     */
    default Map<String, DataInfo> transformUploadDataInfo(PeerState peerState,
                                                          Object remotePeerId) {
        return peerState.getDataInfo();
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
     * @param peerState
     * @param transfer
     * @return
     */
    default boolean isUploadAllowed(PeerState peerState,
                                    Transfer transfer) {
        return true;
    }
}
