package de.probst.ba.core.distribution.algorithms;

import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.transfer.Transfer;
import de.probst.ba.core.net.peer.LeecherState;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.util.Tuple2;
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
    public Optional<List<Transfer>> requestDownloads(LeecherState state) {

        if (!state.getDownloads().isEmpty()) {
            logger.debug(state.getPeerId() + ": We are downloading already");
            return Optional.empty();
        }

        // Get lowest id
        Optional<Long> lowestId = state.getLowestUncompletedDataInfoId();

        // This brain has no missing data info
        if (!lowestId.isPresent()) {
            logger.debug(state.getPeerId() + ": Nothing to download right now");
            return Optional.empty();
        }

        // We are only interested in the first data info
        List<Tuple2<PeerId, DataInfo>> remoteDataInfo = Transform.findFirstByIdAndSort(
                state.getEstimatedMissingRemoteDataInfo(),
                lowestId.get());

        if (remoteDataInfo.isEmpty()) {
            logger.debug(state.getPeerId() + ": Pending, check later again.");
            return Optional.empty();
        }

        // Get the last entry (most chunks!)
        Tuple2<PeerId, DataInfo> lastEntry = remoteDataInfo.get(remoteDataInfo.size() - 1);

        if (!lastEntry.second().isCompleted()) {
            logger.debug(state.getPeerId() +
                    ": This brain does not download incomplete data info");
            return Optional.empty();
        }

        logger.debug(state.getPeerId() + ": Requesting " + lastEntry);

        // Request this as download
        return Optional.of(Arrays.asList(
                Transfer.download(lastEntry.first(), lastEntry.second())
        ));
    }

    @Override
    public boolean addInterest(PeerId remotePeerId, DataInfo newDataInfo) {
        return true;
    }
}
