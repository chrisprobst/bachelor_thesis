package de.probst.ba.core.net.peer.peers.netty.handlers.discovery.messages;

import de.probst.ba.core.net.peer.PeerId;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by chrisprobst on 06.09.14.
 */
public final class PeerIdAnnounceMessage implements Serializable {

    private final PeerId peerId;

    public PeerIdAnnounceMessage(PeerId peerId) {
        Objects.requireNonNull(peerId);
        this.peerId = peerId;
    }

    public PeerId getPeerId() {
        return peerId;
    }
}
