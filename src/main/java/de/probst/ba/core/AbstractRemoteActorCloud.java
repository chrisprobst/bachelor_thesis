package de.probst.ba.core;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by chrisprobst on 08.08.14.
 */
public abstract class AbstractRemoteActorCloud implements RemoteActorCloud {

    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors());

    private static final AtomicLong ID_GENERATOR = new AtomicLong();

    private final ConcurrentMap<Long, RemoteActor> actors = new ConcurrentHashMap<>();

    protected long getNextId() {
        return ID_GENERATOR.getAndIncrement();
    }

    @Override
    public RemoteActor getRemoteActor(long id) {
        return actors.get(id);
    }

    @Override
    public Collection<RemoteActor> getRemoteActors() {
        return actors.values();
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return SCHEDULER;
    }
}
