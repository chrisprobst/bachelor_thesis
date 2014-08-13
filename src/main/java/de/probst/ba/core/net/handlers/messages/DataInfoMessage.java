package de.probst.ba.core.net.handlers.messages;

import de.probst.ba.core.logic.DataInfo;

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

        if (dataInfo.isEmpty()) {
            throw new IllegalArgumentException("dataInfo is empty");
        }

        this.dataInfo = new HashMap<>(dataInfo);
    }

    public Map<String, DataInfo> getDataInfo() {
        return dataInfo;
    }
}
