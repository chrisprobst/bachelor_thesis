package de.probst.ba.core.legacy;

import de.probst.ba.core.logic.DataInfo;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 08.08.14.
 */
public interface RemoteActorCloud {

    /**
     * Registers a local actor.
     *
     * @param downloadRate
     * @param uploadRate
     * @param fulfillPolicy
     * @return
     */
    LocalActor registerLocalActor(long downloadRate, long uploadRate, FulfillPolicy fulfillPolicy);

    /**
     * Get all associated remote actors.
     *
     * @return
     */
    Collection<RemoteActor> getRemoteActors();

    /**
     * Get the remote actor with the given id.
     *
     * @param id
     * @return
     */
    RemoteActor getRemoteActor(long id);

    /**
     * Get the default scheduler.
     *
     * @return
     */
    ScheduledExecutorService getScheduler();

    /**
     * Get all data from all remote actors asynchronously.
     *
     * @return
     */
    default CompletableFuture<Map<RemoteActor, Map<String, DataInfo>>> getAllDataFromAllRemoteActorsAsync(LocalActor initiator) {

        // Create list of futures
        List<AbstractMap.SimpleEntry<RemoteActor, CompletableFuture<Map<String, DataInfo>>>> allDataFutures =
                getRemoteActors()
                        .stream()
                        .filter(a -> initiator == null || a.getId() != initiator.getId())
                        .map(a -> new AbstractMap.SimpleEntry<>(a, a.getAllDataAsync()))
                        .collect(Collectors.toList());

        // Create an array of the futures
        CompletableFuture<?>[] array = allDataFutures
                .stream()
                .map(AbstractMap.SimpleEntry::getValue)
                .toArray(CompletableFuture<?>[]::new);

        // The result future
        CompletableFuture<Map<RemoteActor, Map<String, DataInfo>>> result = new CompletableFuture<>();

        // Wait for completion and complete the future
        CompletableFuture.allOf(array)
                .whenComplete((v, e) -> result.complete(allDataFutures
                        .stream()
                        .filter(c -> c.getValue().isDone() &&
                                !c.getValue().isCompletedExceptionally())
                        .map(c -> new AbstractMap.SimpleEntry<>(c.getKey(), c.getValue().join()))
                        .collect(Collectors.toMap(
                                AbstractMap.SimpleEntry::getKey,
                                AbstractMap.SimpleEntry::getValue))));

        return result;
    }
}
