package de.probst.ba.core;

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
    default CompletableFuture<Map<RemoteActor, Collection<Data>>> getAllData() {

        // Create list of futures
        List<AbstractMap.SimpleEntry<RemoteActor, CompletableFuture<Collection<Data>>>> allDataFutures =
                getRemoteActors()
                        .stream()
                        .map(a -> new AbstractMap.SimpleEntry<>(a, a.getAllDataAsync()))
                        .collect(Collectors.toList());

        CompletableFuture<?>[] array = allDataFutures.toArray(
                new CompletableFuture<?>[allDataFutures.size()]);

        // The result future
        CompletableFuture<Map<RemoteActor, Collection<Data>>> result = new CompletableFuture<>();

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
