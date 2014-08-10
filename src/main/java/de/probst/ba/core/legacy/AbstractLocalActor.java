package de.probst.ba.core.legacy;

import de.probst.ba.core.logic.DataInfo;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by chrisprobst on 08.08.14.
 */
public abstract class AbstractLocalActor extends AbstractActor implements LocalActor {

    private final ConcurrentMap<String, DataInfo> allData = new ConcurrentHashMap<>();
    private final FulfillPolicy fulfillPolicy;

    protected AbstractLocalActor(RemoteActorCloud remoteActorCloud,
                                 long id,
                                 long downloadRate,
                                 long uploadRate,
                                 FulfillPolicy fulfillPolicy) {

        super(remoteActorCloud, id, downloadRate, uploadRate);
        this.fulfillPolicy = fulfillPolicy;
    }

    @Override
    public FulfillPolicy getFulfillPolicy() {
        return fulfillPolicy;
    }

    @Override
    public ConcurrentMap<String, DataInfo> getAllData() {
        return allData;
    }
}
