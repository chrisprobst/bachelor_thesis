package de.probst.ba.core.legacy;

/**
 * Created by chrisprobst on 08.08.14.
 */
public abstract class AbstractRemoteActor extends AbstractActor implements RemoteActor {

    protected AbstractRemoteActor(RemoteActorCloud remoteActorCloud, long id, long downloadRate, long uploadRate) {
        super(remoteActorCloud, id, downloadRate, uploadRate);
    }
}
