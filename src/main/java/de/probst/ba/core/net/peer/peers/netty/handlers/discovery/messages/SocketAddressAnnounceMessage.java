package de.probst.ba.core.net.peer.peers.netty.handlers.discovery.messages;

import java.io.Serializable;
import java.net.SocketAddress;
import java.util.Objects;

/**
 * Created by chrisprobst on 06.09.14.
 */
public final class SocketAddressAnnounceMessage implements Serializable {

    private final SocketAddress socketAddress;

    public SocketAddressAnnounceMessage(SocketAddress socketAddress) {
        Objects.requireNonNull(socketAddress);
        this.socketAddress = socketAddress;
    }

    public SocketAddress getSocketAddress() {
        return socketAddress;
    }
}
