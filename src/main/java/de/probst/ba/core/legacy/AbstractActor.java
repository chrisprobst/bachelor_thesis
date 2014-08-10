package de.probst.ba.core.legacy;

import java.util.Objects;

/**
 * Created by chrisprobst on 08.08.14.
 */
public abstract class AbstractActor implements Actor {

    private final RemoteActorCloud remoteActorCloud;
    private final long id;
    private final long downloadRate;
    private final long uploadRate;

    protected AbstractActor(RemoteActorCloud remoteActorCloud,
                            long id,
                            long downloadRate,
                            long uploadRate) {

        Objects.requireNonNull(remoteActorCloud);
        this.remoteActorCloud = remoteActorCloud;
        this.id = id;
        this.downloadRate = downloadRate;
        this.uploadRate = uploadRate;
    }

    @Override
    public RemoteActorCloud getRemoteActorCloud() {
        return remoteActorCloud;
    }

    public long getId() {
        return id;
    }

    @Override
    public long getUploadRate() {
        return uploadRate;
    }

    @Override
    public long getDownloadRate() {
        return downloadRate;
    }
}
