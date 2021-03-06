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

    private static final class EmptySocketAddress extends SocketAddress {

        private final String uniqueAddress;

        private EmptySocketAddress(String uniqueAddress) {
            Objects.requireNonNull(uniqueAddress);
            this.uniqueAddress = uniqueAddress;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "@" + uniqueAddress;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EmptySocketAddress that = (EmptySocketAddress) o;

            if (!uniqueAddress.equals(that.uniqueAddress)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return uniqueAddress.hashCode();
        }
    }

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
        this.socketAddress = socketAddress.orElseGet(() -> new EmptySocketAddress(uniqueId.toString()));
        this.uniqueId = uniqueId;
    }

    public Optional<SocketAddress> getSocketAddress() {
        return Optional.ofNullable(socketAddress);
    }

    public Object getUniqueId() {
        return uniqueId;
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

        if (socketAddress != null ? !socketAddress.equals(peerId.socketAddress) : peerId.socketAddress != null)
            return false;
        if (!uniqueId.equals(peerId.uniqueId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = socketAddress != null ? socketAddress.hashCode() : 0;
        result = 31 * result + uniqueId.hashCode();
        return result;
    }
}
