package de.probst.ba.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by chrisprobst on 08.08.14.
 */
public abstract class AbstractLocalActor extends AbstractActor implements LocalActor {

    private final ConcurrentMap<String, Data> allData = new ConcurrentHashMap<>();

    protected AbstractLocalActor(RemoteActorCloud remoteActorCloud, long id, long downloadRate, long uploadRate) {
        super(remoteActorCloud, id, downloadRate, uploadRate);
    }

    @Override
    public ConcurrentMap<String, Data> getAllData() {
        return allData;
    }
}
