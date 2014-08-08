package de.probst.ba.core.local;

import de.probst.ba.core.*;

import java.util.Collection;

/**
 * Created by chrisprobst on 08.08.14.
 */
public class LocalInMemoryActor extends AbstractActor implements LocalActor {

    protected LocalInMemoryActor(RemoteActorCloud remoteActorCloud, long id) {
        super(remoteActorCloud, id);
    }

    @Override
    public void run() {

    }

    @Override
    public Collection<Data> getAllData() {
        return null;
    }
}
