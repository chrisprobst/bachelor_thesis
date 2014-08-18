package de.probst.ba.core.logic.brains;

import de.probst.ba.core.logic.Brain;
import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.util.Tuple;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 16.08.14.
 */
public class AbstractOrderedBrain implements Brain {

    /**
     * This method returns a map with remote peer ids mapped
     * to a not empty data info which has the given data info id.
     * <p>
     * The returned map will be ordered according to the number of
     * completed chunks.
     *
     * @param remoteDataInfo
     * @param dataInfoId
     * @return
     */
    protected Map<PeerId, DataInfo> firstOrderedById(Map<PeerId, Map<String, DataInfo>> remoteDataInfo,
                                                     long dataInfoId) {
        return remoteDataInfo.entrySet().stream()
                .map(p -> Tuple.of(
                        p.getKey(),
                        p.getValue().values().stream().filter(d -> d.getId() == dataInfoId).findFirst()))
                .filter(p -> p.second().isPresent())
                .sorted(Comparator.comparing(p -> p.second().get().getCompletedChunkCount()))
                .collect(Collectors.toMap(
                        Tuple::first,
                        p -> p.second().get(),
                        (u, v) -> {
                            throw new IllegalStateException(String.format("Duplicate key %s", u));
                        },
                        LinkedHashMap::new
                ));
    }


}
