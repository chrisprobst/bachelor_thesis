package de.probst.ba.core.net.peer.state;

import de.probst.ba.core.net.peer.Peer;

import java.util.Objects;

/**
 * Created by chrisprobst on 01.09.14.
 */
public class PeerState {

    // The peer id
    private final Peer peer;

    public PeerState(Peer peer) {
        Objects.requireNonNull(peer);
        this.peer = peer;
    }

    /**
     * @return The peer.
     */
    public Peer getPeer() {
        return peer;
    }
}
