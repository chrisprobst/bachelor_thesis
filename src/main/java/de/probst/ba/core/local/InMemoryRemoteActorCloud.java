package de.probst.ba.core.local;

import de.probst.ba.core.AbstractRemoteActorCloud;
import de.probst.ba.core.LocalActor;

/**
 * Created by chrisprobst on 09.08.14.
 */
public class InMemoryRemoteActorCloud extends AbstractRemoteActorCloud {

    @Override
    public LocalActor registerLocalActor(long downloadRate, long uploadRate) {
        InMemoryLocalActor actor = new InMemoryLocalActor(this, getNextId(), downloadRate, uploadRate);
        actors.put(actor.getId(), actor.getRemoteActor());
        getScheduler().execute(actor);
        return actor;
    }
}
