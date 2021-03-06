package de.probst.ba.core.distribution;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.Transfer;

import java.util.Map;

/**
 * Created by chrisprobst on 01.09.14.
 */
public interface SeederDistributionAlgorithm extends DistributionAlgorithm {

    default boolean isSuperSeederDistributionAlgorithm() {
        return false;
    }

    /**
     * This method is called by the framework internally at
     * undefined intervals and can be triggered by network metrics,
     * timers and events randomly.
     * <p>
     * This method is called by different threads concurrently, so
     * make sure you have no race conditions.
     * <p>
     * The purpose of this method is to transform the local data info we are willing
     * to upload. This way for instance we could stop announcing data info if
     * we are already uploading the given data. This could help implement the
     * logarithmic strategy where every peer only uploads data to one other peer.
     *
     * @param seeder
     * @param dataInfo
     * @param remotePeerId The remote peer id.
     * @return The data info.
     */
    Map<String, DataInfo> transformUploadDataInfo(Seeder seeder, Map<String, DataInfo> dataInfo, PeerId remotePeerId);

    /**
     * Returns whether or not the given upload is allowed.
     * <p>
     * This method is thread-safe.
     * <p>
     * The framework internally already checks that one peer cannot start
     * two downloads in parallel because it would not make much sense.
     *
     * @param seeder
     * @param transfer
     * @return
     */
    boolean isUploadAllowed(Seeder seeder, Transfer transfer);
}
