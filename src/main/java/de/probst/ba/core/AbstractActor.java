package de.probst.ba.core;

import java.util.Objects;

/**
 * Created by chrisprobst on 08.08.14.
 */
public abstract class AbstractActor implements Actor {

    private final RemoteActorCloud remoteActorCloud;
    private final long id;

    protected AbstractActor(RemoteActorCloud remoteActorCloud, long id) {
        Objects.requireNonNull(remoteActorCloud);
        this.remoteActorCloud = remoteActorCloud;
        this.id = id;
    }

    @Override
    public RemoteActorCloud getRemoteActorCloud() {
        return remoteActorCloud;
    }

    public long getId() {
        return id;
    }
}
