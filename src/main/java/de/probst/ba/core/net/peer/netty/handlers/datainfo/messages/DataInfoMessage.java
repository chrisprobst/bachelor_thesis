package de.probst.ba.core.net.peer.netty.handlers.datainfo.messages;

import de.probst.ba.core.media.DataInfo;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by chrisprobst on 11.08.14.
 */
public final class DataInfoMessage implements Serializable {

    private final Map<String, DataInfo> dataInfo;

    public DataInfoMessage(Map<String, DataInfo> dataInfo) {
        Objects.requireNonNull(dataInfo);
        this.dataInfo = new HashMap<>(dataInfo);
    }

    public Map<String, DataInfo> getDataInfo() {
        return dataInfo;
    }
}
