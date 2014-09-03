package de.probst.ba.core.net.peer.state;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.Peer;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by chrisprobst on 03.09.14.
 */
public class DataInfoState extends PeerState {


    // All already available local data info
    private final Map<String, DataInfo> dataInfo;

    public DataInfoState(Peer peer,
                         Map<String, DataInfo> dataInfo) {
        super(peer);
        Objects.requireNonNull(dataInfo);
        this.dataInfo = Collections.unmodifiableMap(new HashMap<>(dataInfo));
    }

    /**
     * @return The lowest id of all uncompleted
     * data info.
     */
    public Optional<Long> getLowestUncompletedDataInfoId() {
        return getDataInfo().entrySet().stream()
                .filter(p -> !p.getValue().isCompleted())
                .sorted(Comparator.comparing(p -> p.getValue().getId()))
                .findFirst().map(p -> p.getValue().getId());
    }

    /**
     * @return All already available local data info.
     */
    public Map<String, DataInfo> getDataInfo() {
        return dataInfo;
    }
}
