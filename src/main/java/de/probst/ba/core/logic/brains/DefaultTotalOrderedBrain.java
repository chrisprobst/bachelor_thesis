package de.probst.ba.core.logic.brains;

import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.NetworkState;
import de.probst.ba.core.net.Transfer;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.ArrayList;
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
public class DefaultTotalOrderedBrain extends AbstractOrderedBrain {

    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(DefaultTotalOrderedBrain.class);

    @Override
    public Optional<List<Transfer>> process(NetworkState networkState) {

        if (!networkState.getDownloads().isEmpty()) {
            logger.info("We are downloading already");
            return Optional.empty();
        }

        // We are only interested in the first data info
        Map<Object, DataInfo> nextOrderedDataInfo = firstOrderedById(
                networkState.getEstimatedMissingRemoteDataInfo(),
                networkState.getLowestUncompletedDataInfoId());

        if (nextOrderedDataInfo.isEmpty()) {
            logger.info("Nothing to download right now");
            return Optional.empty();
        }

        // As list
        List<Map.Entry<Object, DataInfo>> list =
                new ArrayList<>(nextOrderedDataInfo.entrySet());

        // Get the last entry (most chunks!)
        Map.Entry<Object, DataInfo> lastEntry = list.get(list.size() - 1);

        logger.info("Requesting " + lastEntry);

        // Request this as download
        return Optional.of(Arrays.asList(
                Transfer.download(lastEntry.getKey(), lastEntry.getValue())
        ));
    }

    @Override
    public Optional<Map<String, DataInfo>> transformUploadDataInfo(NetworkState networkState, Object remotePeerId) {
        return networkState.getUploads().isEmpty() ? Optional.of(networkState.getDataInfo()) : Optional.empty();
    }

    @Override
    public boolean isUploadAllowed(NetworkState networkState, Transfer transfer) {
        return networkState.getUploads().isEmpty();
    }
}
