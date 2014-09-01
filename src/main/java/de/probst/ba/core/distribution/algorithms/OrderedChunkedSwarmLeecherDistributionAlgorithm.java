package de.probst.ba.core.distribution.algorithms;

import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.transfer.Transfer;
import de.probst.ba.core.net.peer.LeecherState;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.util.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by chrisprobst on 01.09.14.
 */
public class OrderedChunkedSwarmLeecherDistributionAlgorithm implements LeecherDistributionAlgorithm {

    private final Logger logger =
            LoggerFactory.getLogger(OrderedChunkedSwarmLeecherDistributionAlgorithm.class);

    @Override
    public Optional<List<Transfer>> requestDownloads(LeecherState state) {

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

        // We will generate a few downloads now
        List<Transfer> transfers = new ArrayList<>();
        while (!remoteDataInfo.isEmpty()) {
            // Get next entry
            Tuple2<PeerId, DataInfo> next = remoteDataInfo.get(0);

            // Choose one random chunk
            DataInfo nextDataInfo = next.second().withOneCompletedChunk();

            // Remove the first entry
            remoteDataInfo.remove(0);

            // Delete the next remote data info
            remoteDataInfo = Transform.removeFromAllAndSort(remoteDataInfo, nextDataInfo);

            // Add the transfer
            transfers.add(Transfer.download(next.first(), nextDataInfo));
        }

        logger.debug(state.getPeerId() + ": Requesting " + transfers);

        // Request this as download
        return Optional.of(transfers);
    }

    @Override
    public boolean addInterest(PeerId remotePeerId, DataInfo newDataInfo) {
        return true;
    }
}
