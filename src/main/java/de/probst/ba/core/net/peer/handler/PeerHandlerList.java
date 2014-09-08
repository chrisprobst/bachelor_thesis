package de.probst.ba.core.net.peer.handler;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by chrisprobst on 05.09.14.
 */
public class PeerHandlerList implements PeerHandler {
    protected final Queue<PeerHandler> peerHandlers = new ConcurrentLinkedQueue<>();

    public PeerHandlerList add(PeerHandler peerHandler) {
        Objects.requireNonNull(peerHandler);
        peerHandlers.add(peerHandler);
        return this;
    }

    public PeerHandlerList remove(PeerHandler peerHandler) {
        Objects.requireNonNull(peerHandler);
        peerHandlers.remove(peerHandler);
        return this;
    }
}
