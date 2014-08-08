package de.probst.ba.core.local;

import de.probst.ba.core.AbstractActor;
import de.probst.ba.core.RemoteActorCloud;
import de.probst.ba.core.Data;
import de.probst.ba.core.RemoteActor;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Created by chrisprobst on 08.08.14.
 */
public class RemoteInMemoryActor extends AbstractActor implements RemoteActor {


    protected RemoteInMemoryActor(RemoteActorCloud remoteActorCloud, long id) {
        super(remoteActorCloud, id);
    }

    @Override
    public CompletableFuture<Collection<Data>> getAllDataAsync() {
        return null;
    }

    @Override
    public CompletableFuture<byte[]> getDataChunkAsync(String hash, int chunkIndex) {
        return null;
    }
}
