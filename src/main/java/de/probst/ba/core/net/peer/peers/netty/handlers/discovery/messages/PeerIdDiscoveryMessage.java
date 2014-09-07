package de.probst.ba.core.net.peer.peers.netty.handlers.discovery.messages;

import de.probst.ba.core.net.peer.PeerId;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Created by chrisprobst on 06.09.14.
 */
public final class PeerIdDiscoveryMessage implements Serializable {

    private final List<PeerId> peerIds;

    public PeerIdDiscoveryMessage(List<PeerId> peerIds) {
        Objects.requireNonNull(peerIds);
        this.peerIds = peerIds;
    }

    public List<PeerId> getPeerIds() {
        return peerIds;
    }
}
