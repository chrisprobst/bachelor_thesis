package de.probst.ba.core.net.peer.handlers.transfer.messages;

import de.probst.ba.core.media.DataInfo;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by chrisprobst on 14.08.14.
 */
public final class DownloadRequestMessage implements Serializable {

    private final DataInfo dataInfo;

    public DownloadRequestMessage(DataInfo dataInfo) {
        Objects.requireNonNull(dataInfo);
        this.dataInfo = dataInfo;
    }

    public DataInfo getDataInfo() {
        return dataInfo;
    }
}
