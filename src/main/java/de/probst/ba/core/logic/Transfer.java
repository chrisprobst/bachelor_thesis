package de.probst.ba.core.logic;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by chrisprobst on 10.08.14.
 */
public final class Transfer implements Serializable {

    private final long peerId;
    private final DataInfo dataInfo;

    public Transfer(long peerId, DataInfo dataInfo) {
        Objects.requireNonNull(dataInfo);

        this.peerId = peerId;
        this.dataInfo = dataInfo;
    }

    public long getPeerId() {
        return peerId;
    }

    public DataInfo getDataInfo() {
        return dataInfo;
    }
}
