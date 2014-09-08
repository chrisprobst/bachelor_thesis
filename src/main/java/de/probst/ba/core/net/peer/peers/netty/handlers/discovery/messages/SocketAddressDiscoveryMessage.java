package de.probst.ba.core.net.peer.peers.netty.handlers.discovery.messages;

import java.io.Serializable;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.Set;

/**
 * Created by chrisprobst on 06.09.14.
 */
public final class SocketAddressDiscoveryMessage implements Serializable {

    private final Set<SocketAddress> socketAddresses;

    public SocketAddressDiscoveryMessage(Set<SocketAddress> socketAddresses) {
        Objects.requireNonNull(socketAddresses);
        this.socketAddresses = socketAddresses;
    }

    public Set<SocketAddress> getSocketAddresses() {
        return socketAddresses;
    }
}
