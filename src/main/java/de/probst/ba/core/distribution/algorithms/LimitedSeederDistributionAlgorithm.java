package de.probst.ba.core.distribution.algorithms;

import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.Transfer;
import de.probst.ba.core.net.peer.state.SeederDataInfoState;

import java.util.Collections;
import java.util.Map;

/**
 * Created by chrisprobst on 01.09.14.
 */
public final class LimitedSeederDistributionAlgorithm implements SeederDistributionAlgorithm {

    private final int maxParallelUploads;

    public LimitedSeederDistributionAlgorithm(int maxParallelUploads) {
        if (maxParallelUploads < 0) {
            throw new IllegalArgumentException("maxParallelUploads < 0");
        }
        this.maxParallelUploads = maxParallelUploads;
    }

    @Override
    public Map<String, DataInfo> transformUploadDataInfo(Seeder seeder,
                                                         Map<String, DataInfo> dataInfo,
                                                         PeerId remotePeerId) {
        SeederDataInfoState seederDataInfoState = seeder.getDataInfoState();
        return seederDataInfoState.getUploads().size() < maxParallelUploads ?
               dataInfo :
               Collections.emptyMap();
    }

    @Override
    public boolean isUploadAllowed(Seeder seeder, Transfer transfer) {
        return seeder.getDataInfoState().getUploads().size() < maxParallelUploads;
    }
}
