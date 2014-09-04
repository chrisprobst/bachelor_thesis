package de.probst.ba.core.net.peer;

import java.io.Serializable;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.UUID;

/**
 * A peer id is a combination of a socket address
 * and a guid object which can be anything but should
 * be unique in one process.
 * <p>
 * Created by chrisprobst on 18.08.14.
 */
public class PeerId implements Serializable {

    private final SocketAddress address;
    private final Object guid;

    public PeerId(SocketAddress address) {
        this(address, UUID.randomUUID());
    }

    public PeerId(SocketAddress address, Object guid) {
        Objects.requireNonNull(address);
        Objects.requireNonNull(guid);

        this.address = address;
        this.guid = guid;
    }

    public SocketAddress getAddress() {
        return address;
    }

    public Object getGuid() {
        return guid;
    }

    @Override
    public String toString() {
        return "PeerId{" +
                "address=" + address +
                ", guid=" + guid +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PeerId peerId = (PeerId) o;

        if (!address.equals(peerId.address)) return false;
        if (!guid.equals(peerId.guid)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = address.hashCode();
        result = 31 * result + guid.hashCode();
        return result;
    }
}
