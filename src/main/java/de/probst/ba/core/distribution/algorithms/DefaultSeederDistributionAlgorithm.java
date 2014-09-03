package de.probst.ba.core.distribution.algorithms;

import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;

import java.util.Map;
import java.util.Optional;

/**
 * Created by chrisprobst on 01.09.14.
 */
public class DefaultSeederDistributionAlgorithm implements SeederDistributionAlgorithm {

    @Override
    public Optional<Map<String, DataInfo>> transformUploadDataInfo(Seeder seeder,
                                                                   PeerId remotePeerId) {
        return Optional.of(seeder.getDataBase().getDataInfo());
    }

    @Override
    public int getMaxParallelUploads(Seeder seeder) {
        return Integer.MAX_VALUE;
    }
}
