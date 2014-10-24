package de.probst.ba.core.distribution.algorithms;

import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Transfer;
import de.probst.ba.core.net.peer.state.LeecherDataInfoState;
import de.probst.ba.core.util.collections.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Created by chrisprobst on 01.09.14.
 */
public final class OrderedChunkedSwarmLeecherDistributionAlgorithm implements LeecherDistributionAlgorithm {

    private final Logger logger = LoggerFactory.getLogger(OrderedChunkedSwarmLeecherDistributionAlgorithm.class);

    @Override
    public List<Transfer> requestDownloads(Leecher leecher) {

        // Get the leecher state
        LeecherDataInfoState leecherDataInfoState = leecher.getDataInfoState();

        // Get lowest id
        OptionalLong lowestId = leecherDataInfoState.getEstimatedMissingRemoteDataInfo()
                                                    .values()
                                                    .stream()
                                                    .map(Map::values)
                                                    .flatMap(Collection::stream)
                                                    .mapToLong(DataInfo::getId)
                                                    .min();

        // This algorithm has no missing data info
        if (!lowestId.isPresent()) {
            logger.debug("Leecher algorithm of " + leecher.getPeerId() + " has nothing to download right now");
            return Collections.emptyList();
        }

        // We are only interested in the first data info
        List<Tuple2<PeerId, DataInfo>> remoteDataInfo =
                Transform.findFirstByIdAndSort(leecherDataInfoState.getEstimatedMissingRemoteDataInfo(),
                                               lowestId.getAsLong());

        if (remoteDataInfo.isEmpty()) {
            logger.debug("Leecher algorithm of " + leecher.getPeerId() + " pending, check later again");
            return Collections.emptyList();
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
        return transfers;
    }
}
