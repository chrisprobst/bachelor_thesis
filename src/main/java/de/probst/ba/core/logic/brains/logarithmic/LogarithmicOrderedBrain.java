package de.probst.ba.core.logic.brains.logarithmic;

import de.probst.ba.core.logic.Brain;
import de.probst.ba.core.logic.Transform;
import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.NetworkState;
import de.probst.ba.core.net.Transfer;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.util.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This brain does only allow one upload in total.
 * <p>
 * This brain downloads from the peer with the most
 * chunks. This brain will not schedule more than one
 * download transfer in parallel.
 * <p>
 * Created by chrisprobst on 16.08.14.
 */
public final class LogarithmicOrderedBrain implements Brain {

    private final Logger logger =
            LoggerFactory.getLogger(LogarithmicOrderedBrain.class);

    @Override
    public Optional<List<Transfer>> process(NetworkState networkState) {

        if (!networkState.getDownloads().isEmpty()) {
            logger.debug(networkState.getLocalPeerId() + ": We are downloading already");
            return Optional.empty();
        }

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

        // Get the last entry (most chunks!)
        Tuple2<PeerId, DataInfo> lastEntry = remoteDataInfo.get(remoteDataInfo.size() - 1);

        if (!lastEntry.second().isCompleted()) {
            logger.debug(networkState.getLocalPeerId() +
                    ": This brain does not download incomplete data info");
            return Optional.empty();
        }

        logger.debug(networkState.getLocalPeerId() + ": Requesting " + lastEntry);

        // Request this as download
        return Optional.of(Arrays.asList(
                Transfer.download(lastEntry.first(), lastEntry.second())
        ));
    }

    @Override
    public Optional<Map<String, DataInfo>> transformUploadDataInfo(NetworkState networkState, PeerId remotePeerId) {
        logger.debug(networkState.getLocalPeerId() + ": Is transforming: " + networkState.getDataInfo());
        return networkState.getUploads().isEmpty() ? Optional.of(networkState.getDataInfo()) : Optional.empty();
    }
}
