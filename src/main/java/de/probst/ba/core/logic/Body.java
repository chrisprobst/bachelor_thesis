package de.probst.ba.core.logic;

import de.probst.ba.core.net.NetworkState;
import de.probst.ba.core.net.Transfer;

import java.util.concurrent.ScheduledExecutorService;

/**
 * A useful abstraction for the brain worker
 * to do its job.
 * <p>
 * Created by chrisprobst on 17.08.14.
 */
public interface Body {

    /**
     * @return The network state.
     */
    NetworkState getNetworkState();

    /**
     * @return The brain.
     */
    Brain getBrain();

    /**
     * @return The scheduler.
     */
    ScheduledExecutorService getScheduler();

    /**
     * Requests a transfer.
     *
     * @param transfer
     */
    void requestTransfer(Transfer transfer);

    /**
     * If the brain has an exception,
     * this method is called.
     *
     * @param e
     */
    void brainDead(Exception e);
}
