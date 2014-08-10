package de.probst.ba.core.legacy;

import de.probst.ba.core.logic.DataInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 09.08.14.
 */
public class Util {

    private Util() {}

    public static List<Map.Entry<RemoteActor, Map<String, DataInfo>>> onlyMissingAndSort(
            Map<String, DataInfo> ownMap,
            Map<RemoteActor, Map<String, DataInfo>> map) {

        // Remove data from interest set
        for(Map.Entry<RemoteActor, Map<String, DataInfo>> entry : new ArrayList<>(map.entrySet())) {
            // Only missing parts!
            entry.getValue().keySet().removeAll(ownMap.keySet());

            // If this actor cannot offer us parts we need
            // we simply remove him from the map
            if(entry.getValue().keySet().isEmpty()) {
                map.remove(entry.getKey());
            }
        }

        // Look for missing files and sort according to the number of data
        return map.entrySet().stream()
                .sorted((a, b) -> Integer.compare(a.getValue().size(), b.getValue().size()))
                .collect(Collectors.toList());
    }
}
