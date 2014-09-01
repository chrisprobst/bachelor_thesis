package de.probst.ba.core.distribution.algorithms;

import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.SeederState;

import java.util.Map;
import java.util.Optional;

/**
 * Created by chrisprobst on 01.09.14.
 */
public class LimitedSeederDistributionAlgorithm implements SeederDistributionAlgorithm {

    private final int maxParallelUploads;

    public LimitedSeederDistributionAlgorithm(int maxParallelUploads) {
        if (maxParallelUploads < 0) {
            throw new IllegalArgumentException("maxParallelUploads < 0");
        }
        this.maxParallelUploads = maxParallelUploads;
    }

    @Override
    public Optional<Map<String, DataInfo>> transformUploadDataInfo(SeederState state,
                                                                   PeerId remotePeerId) {
        return state.getUploads().size() < maxParallelUploads ?
                Optional.of(state.getDataInfo()) : Optional.empty();
    }

    @Override
    public int getMaxParallelUploads() {
        return maxParallelUploads;
    }
}
