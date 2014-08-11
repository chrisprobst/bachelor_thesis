package de.probst.ba.core.logic;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a transfer.
 * <p>
 * Created by chrisprobst on 10.08.14.
 */
public final class Transfer implements Serializable {

    // The remote peer id
    private final long remotePeerId;

    // The data info which describes this transfer
    private final DataInfo dataInfo;

    public Transfer(long remotePeerId, DataInfo dataInfo) {
        Objects.requireNonNull(dataInfo);

        this.remotePeerId = remotePeerId;
        this.dataInfo = dataInfo;
    }

    /**
     * @return The remote peer id.
     */
    public long getRemotePeerId() {
        return remotePeerId;
    }

    /**
     * @return The data info which describes this transfer.
     */
    public DataInfo getDataInfo() {
        return dataInfo;
    }
}
