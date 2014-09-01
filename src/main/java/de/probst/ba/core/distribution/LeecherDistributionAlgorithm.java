package de.probst.ba.core.distribution;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.transfer.Transfer;
import de.probst.ba.core.net.peer.LeecherState;
import de.probst.ba.core.net.peer.PeerId;

import java.util.List;
import java.util.Optional;

/**
 * Created by chrisprobst on 01.09.14.
 */
public interface LeecherDistributionAlgorithm extends DistributionAlgorithm {

    /**
     * This method is called by the framework internally at
     * undefined intervals and can be triggered by network metrics,
     * timers and events randomly.
     * <p>
     * This method is thread-safe.
     * <p>
     * The purpose of this method is to implement distribution strategies.
     * In other words: To request downloads in the most effective way.
     *
     * @param state
     * @return An optional list of download transfers.
     * The framework will take care that you do not download
     * from the same peer in parallel different chunks.
     */
    Optional<List<Transfer>> requestDownloads(LeecherState state);

    /**
     * This method is called by the framework when there is need to decide
     * whether or not the given new data info should be added to the
     * interest set.
     *
     * @param remotePeerId
     * @param newDataInfo
     * @return
     */
    boolean addInterest(PeerId remotePeerId,
                        DataInfo newDataInfo);
}
