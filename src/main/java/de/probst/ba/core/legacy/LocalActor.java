package de.probst.ba.core.legacy;

import de.probst.ba.core.logic.DataInfo;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by chrisprobst on 08.08.14.
 */
public interface LocalActor extends Actor, Runnable {

    /**
     * Represents the type of fulfillment.
     *
     * @return
     */
    FulfillPolicy getFulfillPolicy();

    /**
     * Represents the data store of this actor.
     * Feel free to modify this map.
     *
     * @return
     */
    ConcurrentMap<String, DataInfo> getAllData();

    default CompletableFuture<Map<RemoteActor, Map<String, DataInfo>>> getAllDataFromAllRemoteActorsAsync() {
        return getRemoteActorCloud().getAllDataFromAllRemoteActorsAsync(this);
    }

    @Override
    default void run() {
        System.out.println("Running actor with id: " + getId() + " and policy: " + getFulfillPolicy());

        if (getFulfillPolicy() == FulfillPolicy.Client) {

            // Look for missing files
            getAllDataFromAllRemoteActorsAsync().thenAccept(map -> {

                // Create a copy of our existing parts
                Map<String, DataInfo> ownMap = new HashMap<>(getAllData());

                // While we can download stuff
                while (!map.isEmpty()) {

                    // List of possible downloadable files
                    List<Map.Entry<RemoteActor, Map<String, DataInfo>>> list =
                            Util.onlyMissingAndSort(ownMap, map);

                    System.out.println("Client " + getId() + " could load " + list);

                    // No peer =(
                    if(list.isEmpty()) {
                        break;
                    }

                    // Get the first entry
                    Map.Entry<RemoteActor, Map<String, DataInfo>> entry = list.get(0);

                    // The actor
                    RemoteActor remoteActor = entry.getKey();

                    // Get next shuffled data
                    List<DataInfo> shuffledDataInfo = new ArrayList<>(entry.getValue().values());
                    Collections.shuffle(shuffledDataInfo);

                    // Get first
                    DataInfo firstDataInfo = shuffledDataInfo.iterator().next();

                    // Try to download part
                    remoteActor
                            .getDataContentAsync(firstDataInfo.getHash(), buffer -> {
                            })
                            .thenRun(() -> {
                                System.out.println("File finished: " + firstDataInfo.getHash());
                                getAllData().put(firstDataInfo.getHash(), firstDataInfo.full());
                            }).exceptionally(e -> {e.printStackTrace(); return null;});

                    // We are not interested in this data any more
                    ownMap.put(firstDataInfo.getHash(), firstDataInfo.full());
                    getAllData().put(firstDataInfo.getHash(), firstDataInfo.full());

                    // We do not load twice from the same actor
                    map.remove(remoteActor);
                }
            });
        }
    }
}
