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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by chrisprobst on 01.09.14.
 */
public class OrderedChunkedSwarmLeecherDistributionAlgorithm implements LeecherDistributionAlgorithm {

    private final Logger logger = LoggerFactory.getLogger(OrderedChunkedSwarmLeecherDistributionAlgorithm.class);

    @Override
    public Optional<List<Transfer>> requestDownloads(Leecher leecher) {
        // Get the leecher state
        LeecherDataInfoState leecherDataInfoState = leecher.getDataInfoState();

        // Get lowest id
        Optional<Long> lowestId = leecherDataInfoState.getLowestUncompletedDataInfoId();

        // This brain has no missing data info
        if (!lowestId.isPresent()) {
            logger.debug("Leecher algorithm of " + leecher.getPeerId() + " has nothing to download right now");
            return Optional.empty();
        }

        // We are only interested in the first data info
        List<Tuple2<PeerId, DataInfo>> remoteDataInfo =
                Transform.findFirstByIdAndSort(leecherDataInfoState.getEstimatedMissingRemoteDataInfo(),
                                               lowestId.get());

        if (remoteDataInfo.isEmpty()) {
            logger.debug("Leecher algorithm of " + leecher.getPeerId() + " pending, check later again");
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

        logger.debug("Leecher algorithm of " + leecher.getPeerId() + " requesting " + transfers);

        // Request this as download
        return Optional.of(transfers);
    }

    @Override
    public boolean addInterest(Leecher leecher, PeerId remotePeerId, DataInfo newDataInfo) {
        return true;
    }
}
