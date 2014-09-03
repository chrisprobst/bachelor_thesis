package de.probst.ba.core.distribution.algorithms;

import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.transfer.Transfer;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.state.LeecherDataInfoState;
import de.probst.ba.core.util.collections.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Created by chrisprobst on 01.09.14.
 */
public class OrderedLogarithmicLeecherDistributionAlgorithm implements LeecherDistributionAlgorithm {

    private final Logger logger =
            LoggerFactory.getLogger(OrderedLogarithmicLeecherDistributionAlgorithm.class);

    @Override
    public Optional<List<Transfer>> requestDownloads(Leecher leecher) {
        // Get the leecher state
        LeecherDataInfoState leecherDataInfoState = leecher.getDataInfoState();

        if (!leecherDataInfoState.getDownloads().isEmpty()) {
            logger.debug("Leecher algorithm of " + leecher.getPeerId() +
                    " is already downloading");
            return Optional.empty();
        }

        // Get lowest id
        Optional<Long> lowestId = leecherDataInfoState.getLowestUncompletedDataInfoId();

        // This brain has no missing data info
        if (!lowestId.isPresent()) {
            logger.debug("Leecher algorithm of " + leecher.getPeerId() +
                    " has nothing to download right now");
            return Optional.empty();
        }

        // We are only interested in the first data info
        List<Tuple2<PeerId, DataInfo>> remoteDataInfo = Transform.findFirstByIdAndSort(
                leecherDataInfoState.getEstimatedMissingRemoteDataInfo(),
                lowestId.get());

        if (remoteDataInfo.isEmpty()) {
            logger.debug("Leecher algorithm of " + leecher.getPeerId() +
                    " pending, check later again");
            return Optional.empty();
        }

        // Get the last entry (most chunks!)
        Tuple2<PeerId, DataInfo> lastEntry = remoteDataInfo.get(remoteDataInfo.size() - 1);

        if (!lastEntry.second().isCompleted()) {
            logger.debug("Leecher algorithm of " + leecher.getPeerId() +
                    " does not download incomplete data info");
            return Optional.empty();
        }

        logger.debug("Leecher algorithm of " + leecher.getPeerId() +
                " requesting " + lastEntry.second());

        // Request this as download
        return Optional.of(Arrays.asList(
                Transfer.download(lastEntry.first(), lastEntry.second())
        ));
    }

    @Override
    public boolean addInterest(Leecher leecher,
                               PeerId remotePeerId,
                               DataInfo newDataInfo) {
        return true;
    }
}
