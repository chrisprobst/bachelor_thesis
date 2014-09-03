package de.probst.ba.core.net.peer.peers.netty.handlers.datainfo.messages;

import de.probst.ba.core.media.database.DataInfo;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by chrisprobst on 11.08.14.
 */
public final class DataInfoMessage implements Serializable {

    private final Map<String, DataInfo> dataInfo;

    public DataInfoMessage(Optional<Map<String, DataInfo>> dataInfo) {
        Objects.requireNonNull(dataInfo);
        this.dataInfo = dataInfo.orElse(null);
    }

    public Optional<Map<String, DataInfo>> getDataInfo() {
        return Optional.ofNullable(dataInfo);
    }
}
