package de.probst.ba.core;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Created by chrisprobst on 08.08.14.
 */
public interface RemoteActor extends Actor {

    /**
     * Get a map of all data this remote actor can transfer asynchronously.
     *
     * @return
     */
    CompletableFuture<Map<String, Data>> getAllDataAsync();

    /**
     * Downloads the given chunk of the data with the given hash asynchronously.
     *
     * @param hash
     * @param chunkIndex
     * @param consumer
     * @return
     */
    CompletableFuture<Void> getDataContentChunkAsync(String hash,
                                                     int chunkIndex,
                                                     Consumer<byte[]> consumer);

    /**
     * Downloads the data with the given hash asynchronously.
     *
     * @param hash
     * @param consumer
     * @return
     */
    CompletableFuture<Void> getDataContentAsync(String hash,
                                                Consumer<byte[]> consumer);
}
