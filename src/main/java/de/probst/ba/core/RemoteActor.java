package de.probst.ba.core;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Created by chrisprobst on 08.08.14.
 */
public interface RemoteActor extends Actor {

    /**
     * Get a set of all data this remote actor can transfer asynchronously.
     *
     * @return
     */
    CompletableFuture<Collection<Data>> getAllDataAsync();

    /**
     * Downloads the given chunk of the data with the given hash asynchronously.
     *
     * @param hash
     * @param chunkIndex
     * @return
     */
    CompletableFuture<byte[]> getDataChunkAsync(String hash, int chunkIndex);
}
