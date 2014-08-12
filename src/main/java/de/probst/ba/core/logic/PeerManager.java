package de.probst.ba.core.logic;

import java.io.Closeable;

/**
 * Created by chrisprobst on 11.08.14.
 */
public interface PeerManager extends Closeable {

    /**
     * Creates a new peer using the given brain.
     * <p>
     * This method starts the peer and manages the
     * whole life time of the new peer which includes
     * running and scheduling the brain.
     * <p>
     * The implementation can decide what happens if
     * the user tries to create multiple peers using the
     * same peer manager. It might or might not be useful.
     *
     * @param brain
     * @return
     */
    Peer startPeer(Brain brain);
}
