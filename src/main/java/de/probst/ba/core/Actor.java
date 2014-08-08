package de.probst.ba.core;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by chrisprobst on 08.08.14.
 */
public interface Actor {

    /**
     * Get the associated remote actor cloud.
     *
     * @return
     */
    RemoteActorCloud getRemoteActorCloud();

    /**
     * The locally unique id.
     *
     * @return
     */
    long getId();

    default ScheduledExecutorService getScheduler() {
        return getRemoteActorCloud().getScheduler();
    }
}
