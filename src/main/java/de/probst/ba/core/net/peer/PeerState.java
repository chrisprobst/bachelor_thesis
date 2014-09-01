package de.probst.ba.core.net.peer;

import de.probst.ba.core.media.database.DataInfo;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by chrisprobst on 01.09.14.
 */
public class PeerState {

    // The peer id
    private final PeerId peerId;

    // All already available local data info
    private final Map<String, DataInfo> dataInfo;

    public PeerState(PeerId peerId,
                     Map<String, DataInfo> dataInfo) {
        Objects.requireNonNull(peerId);
        Objects.requireNonNull(dataInfo);
        this.peerId = peerId;
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
     * @return The peer id.
     */
    public PeerId getPeerId() {
        return peerId;
    }

    /**
     * @return All already available local data info.
     */
    public Map<String, DataInfo> getDataInfo() {
        return dataInfo;
    }
}
