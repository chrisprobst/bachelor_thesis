package de.probst.ba.core.net.peer;

import java.io.Serializable;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A peer id is a combination of a socket address
 * and a unique id object which can be anything but should
 * be unique in one process.
 * <p>
 * Created by chrisprobst on 18.08.14.
 */
public class PeerId implements Serializable {

    private final SocketAddress socketAddress;
    private final Object uniqueId;

    public PeerId() {
        this(Optional.empty());
    }

    public PeerId(SocketAddress socketAddress) {
        this(Optional.of(socketAddress));
    }

    public PeerId(Optional<SocketAddress> socketAddress) {
        this(socketAddress, UUID.randomUUID());
    }

    public PeerId(SocketAddress socketAddress, Object uniqueId) {
        this(Optional.of(socketAddress), uniqueId);
    }

    public PeerId(Optional<SocketAddress> socketAddress, Object uniqueId) {
        Objects.requireNonNull(socketAddress);
        Objects.requireNonNull(uniqueId);
        this.socketAddress = socketAddress.orElse(null);
        this.uniqueId = uniqueId;
    }

    public boolean isConnectable() {
        return socketAddress != null;
    }

    public Optional<SocketAddress> getSocketAddress() {
        return Optional.ofNullable(socketAddress);
    }

    public Object getUniqueId() {
        return uniqueId;
    }

    public PeerId withSocketAddress(SocketAddress socketAddress) {
        return new PeerId(Optional.of(socketAddress), uniqueId);
    }

    @Override
    public String toString() {
        return "PeerId{" +
               "socketAddress=" + socketAddress +
               ", uniqueId=" + uniqueId +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PeerId peerId = (PeerId) o;

        if (!socketAddress.equals(peerId.socketAddress)) return false;
        if (!uniqueId.equals(peerId.uniqueId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = socketAddress.hashCode();
        result = 31 * result + uniqueId.hashCode();
        return result;
    }
}
