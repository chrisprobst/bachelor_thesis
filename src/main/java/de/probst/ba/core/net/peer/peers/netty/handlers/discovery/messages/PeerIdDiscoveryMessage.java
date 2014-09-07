package de.probst.ba.core.net.peer.peers.netty.handlers.discovery.messages;

import de.probst.ba.core.net.peer.PeerId;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

/**
 * Created by chrisprobst on 06.09.14.
 */
public final class PeerIdDiscoveryMessage implements Serializable {

    private final Set<PeerId> peerIds;

    public PeerIdDiscoveryMessage(Set<PeerId> peerIds) {
        Objects.requireNonNull(peerIds);
        this.peerIds = peerIds;
    }

    public Set<PeerId> getPeerIds() {
        return peerIds;
    }
}
