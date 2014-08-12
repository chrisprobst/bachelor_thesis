package de.probst.ba.core.logic;

/**
 * The main peer interface. If you want
 * to implement a custom transport this
 * is the only interface you have to implement.
 * <p>
 * Created by chrisprobst on 10.08.14.
 */
public interface Peer {

    /**
     * @return The peer id.
     */
    Object getPeerId();

    /**
     * The peer state is internally updated periodically.
     *
     * @return The last peer state.
     */
    PeerState getPeerState();

    /**
     * Request to download data from the peer with
     * the given id using the given data info. If there
     * is already a pending download using the same id,
     * or the given arguments are invalid for some reason,
     * the new download request is ignored.
     *
     * @param remotePeerId
     * @param dataInfo
     */
    void download(Object remotePeerId, DataInfo dataInfo);
}
