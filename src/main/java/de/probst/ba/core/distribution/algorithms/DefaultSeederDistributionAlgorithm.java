package de.probst.ba.core.distribution.algorithms;

import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;

import java.util.Map;

/**
 * Created by chrisprobst on 01.09.14.
 */
public class DefaultSeederDistributionAlgorithm implements SeederDistributionAlgorithm {

    @Override
    public Map<String, DataInfo> transformUploadDataInfo(Seeder seeder,
                                                         Map<String, DataInfo> dataInfo,
                                                         PeerId remotePeerId) {
        return dataInfo;
    }

    @Override
    public int getMaxParallelUploads(Seeder seeder) {
        return Integer.MAX_VALUE;
    }
}
