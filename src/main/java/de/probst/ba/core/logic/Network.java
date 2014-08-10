package de.probst.ba.core.logic;

/**
 * Created by chrisprobst on 10.08.14.
 */
public interface Network extends Runnable {

    /**
     * The network state is internally updated periodically.
     *
     * @return The last network state.
     */
    NetworkState getNetworkState();

    /**
     * Request to download data from the peer with
     * the given id using the given data info. If there
     * is already a pending download using the same id,
     * the new download request is ignored.
     *
     * @param peerId
     * @param dataInfo
     */
    void download(long peerId, DataInfo dataInfo);
}
