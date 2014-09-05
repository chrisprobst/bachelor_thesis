package de.probst.ba.core.net.peer.handler;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by chrisprobst on 05.09.14.
 */
public class PeerHandlerList implements PeerHandler {
    protected final Queue<PeerHandler> peerHandlers = new ConcurrentLinkedQueue<>();

    public PeerHandlerList add(Optional<PeerHandler> peerHandler) {
        peerHandler.ifPresent(peerHandlers::add);
        return this;
    }

    public PeerHandlerList remove(Optional<PeerHandler> peerHandler) {
        peerHandler.ifPresent(peerHandlers::remove);
        return this;
    }
}
