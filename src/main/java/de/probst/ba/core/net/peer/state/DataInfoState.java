package de.probst.ba.core.net.peer.state;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.Peer;

import java.util.Map;
import java.util.Objects;

/**
 * Created by chrisprobst on 03.09.14.
 */
public class DataInfoState extends PeerState {


    // All already available local data info
    private final Map<String, DataInfo> dataInfo;

    public DataInfoState(Peer peer, Map<String, DataInfo> dataInfo) {
        super(peer);
        Objects.requireNonNull(dataInfo);
        this.dataInfo = dataInfo;
    }

    /**
     * @return All already available local data info.
     */
    public Map<String, DataInfo> getDataInfo() {
        return dataInfo;
    }
}
