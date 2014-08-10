package de.probst.ba.core.legacy.local;

import de.probst.ba.core.legacy.AbstractLocalActor;
import de.probst.ba.core.legacy.FulfillPolicy;
import de.probst.ba.core.legacy.RemoteActor;
import de.probst.ba.core.legacy.RemoteActorCloud;

/**
 * Created by chrisprobst on 09.08.14.
 */
public class InMemoryLocalActor extends AbstractLocalActor {

    private final RemoteActor remoteActor = new InMemoryRemoteActor(
            getRemoteActorCloud(),
            getId(),
            getDownloadRate(),
            getUploadRate(),
            this
    );

    protected InMemoryLocalActor(RemoteActorCloud remoteActorCloud,
                                 long id,
                                 long downloadRate,
                                 long uploadRate,
                                 FulfillPolicy fulfillPolicy) {

        super(remoteActorCloud, id, downloadRate, uploadRate, fulfillPolicy);
    }

    public RemoteActor getRemoteActor() {
        return remoteActor;
    }

}
