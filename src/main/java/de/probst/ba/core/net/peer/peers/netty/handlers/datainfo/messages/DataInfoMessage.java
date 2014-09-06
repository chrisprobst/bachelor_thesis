package de.probst.ba.core.net.peer.peers.netty.handlers.datainfo.messages;

import de.probst.ba.core.media.database.DataInfo;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by chrisprobst on 11.08.14.
 */
public final class DataInfoMessage implements Serializable {

    private final Map<String, DataInfo> dataInfo;

    public DataInfoMessage(Map<String, DataInfo> dataInfo) {
        this.dataInfo = dataInfo;
    }

    public Map<String, DataInfo> getDataInfo() {
        return dataInfo;
    }
}
