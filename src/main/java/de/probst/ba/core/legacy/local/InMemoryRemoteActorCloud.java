package de.probst.ba.core.legacy.local;

import de.probst.ba.core.legacy.AbstractRemoteActorCloud;
import de.probst.ba.core.legacy.FulfillPolicy;
import de.probst.ba.core.legacy.LocalActor;

/**
 * Created by chrisprobst on 09.08.14.
 */
public class InMemoryRemoteActorCloud extends AbstractRemoteActorCloud {

    @Override
    public LocalActor registerLocalActor(long downloadRate, long uploadRate, FulfillPolicy fulfillPolicy) {
        InMemoryLocalActor actor = new InMemoryLocalActor(this, getNextId(), downloadRate, uploadRate, fulfillPolicy);
        actors.put(actor.getId(), actor.getRemoteActor());
        getScheduler().execute(actor);
        return actor;
    }
}
