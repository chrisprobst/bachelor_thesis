package de.probst.ba.core.logic.brains.intelligent;

import de.probst.ba.core.logic.Brain;
import de.probst.ba.core.logic.Transform;
import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.NetworkState;
import de.probst.ba.core.net.Transfer;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.util.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by chrisprobst on 18.08.14.
 */
public final class IntelligentOrderedBrain implements Brain {

    private final Logger logger =
            LoggerFactory.getLogger(IntelligentOrderedBrain.class);

    @Override
    public Optional<List<Transfer>> process(NetworkState networkState) {

        // Get lowest id
        Optional<Long> lowestId = networkState.getLowestUncompletedDataInfoId();

        // This brain has no missing data info
        if (!lowestId.isPresent()) {
            logger.debug(networkState.getLocalPeerId() + ": Nothing to download right now");
            return Optional.empty();
        }

        // We are only interested in the first data info
        List<Tuple2<PeerId, DataInfo>> remoteDataInfo = Transform.findFirstByIdAndSort(
                networkState.getEstimatedMissingRemoteDataInfo(),
                lowestId.get());

        if (remoteDataInfo.isEmpty()) {
            logger.debug(networkState.getLocalPeerId() + ": Pending, check later again.");
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

        logger.debug(networkState.getLocalPeerId() + ": Requesting " + transfers);

        // Request this as download
        return Optional.of(transfers);
    }

    @Override
    public int getMaxParallelUploads() {
        return Integer.MAX_VALUE;
    }
}
