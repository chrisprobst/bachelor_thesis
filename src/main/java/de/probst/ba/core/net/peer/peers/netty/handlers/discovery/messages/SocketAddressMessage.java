package de.probst.ba.core.net.peer.peers.netty.handlers.discovery.messages;

import de.probst.ba.core.net.peer.peers.netty.handlers.traffic.EstimatedMessageSize;

import java.io.Serializable;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Created by chrisprobst on 06.09.14.
 */
@EstimatedMessageSize(0)
public final class SocketAddressMessage implements Serializable {

    private final Set<SocketAddress> socketAddresses;

    public SocketAddressMessage(SocketAddress socketAddresses) {
        this(Collections.singleton(socketAddresses));
    }

    public SocketAddressMessage(Set<SocketAddress> socketAddresses) {
        Objects.requireNonNull(socketAddresses);
        this.socketAddresses = socketAddresses;
    }

    public Set<SocketAddress> getSocketAddresses() {
        return socketAddresses;
    }
}
