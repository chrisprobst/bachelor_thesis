package de.probst.ba.core.distribution.algorithms;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.util.collections.Tuple;
import de.probst.ba.core.util.collections.Tuple2;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 20.08.14.
 */
public final class Transform {

    private Transform() {
    }

    /**
     * This method returns a list of tuples with remote peer ids mapped
     * to a not empty data info which has the given data info id.
     * <p>
     * The returned list will be sorted according to the number of
     * completed chunks.
     *
     * @param remoteDataInfo
     * @param dataInfoId
     * @return
     */
    public static List<Tuple2<PeerId, DataInfo>> findFirstByIdAndSort(Map<PeerId, Map<String, DataInfo>> remoteDataInfo,
                                                                      long dataInfoId) {
        return remoteDataInfo.entrySet()
                             .stream()
                             .map(p -> Tuple.of(p.getKey(),
                                                p.getValue()
                                                 .values()
                                                 .stream()
                                                 .filter(d -> d.getId() == dataInfoId)
                                                 .findFirst()))
                             .filter(p -> p.second().isPresent())
                             .sorted(Comparator.comparing(p -> p.second().get().getCompletedChunkCount()))
                             .map(t -> Tuple.of(t.first(), t.second().get()))
                             .collect(Collectors.toList());
    }

    /**
     * This method removes the given data info from all tuples and
     * sort the list according to the number of completed chunks.
     *
     * @param remoteDataInfo
     * @param removeDataInfo
     * @return
     */
    public static List<Tuple2<PeerId, DataInfo>> removeFromAllAndSort(List<Tuple2<PeerId, DataInfo>> remoteDataInfo,
                                                                      DataInfo removeDataInfo) {
        // Remove the data info from the list
        remoteDataInfo.replaceAll(t -> Tuple.of(t.first(), t.second().substract(removeDataInfo)));

        // Do remove empty data info
        remoteDataInfo.removeIf(t -> t.second().isEmpty());

        // Reorder
        Collections.sort(remoteDataInfo, Comparator.comparing(t -> t.second().getCompletedChunkCount()));

        return remoteDataInfo;
    }
}
