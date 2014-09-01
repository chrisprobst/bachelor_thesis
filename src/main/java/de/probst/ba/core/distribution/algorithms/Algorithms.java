package de.probst.ba.core.distribution.algorithms;

import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.distribution.SeederDistributionAlgorithm;

/**
 * Created by chrisprobst on 01.09.14.
 */
public final class Algorithms {

    private Algorithms() {
    }

    public static SeederDistributionAlgorithm defaultSeederDistributionAlgorithm() {
        return new DefaultSeederDistributionAlgorithm();
    }

    public static SeederDistributionAlgorithm limitedSeederDistributionAlgorithm(int maxParallelUploads) {
        return new LimitedSeederDistributionAlgorithm(maxParallelUploads);
    }

    public static LeecherDistributionAlgorithm orderedChunkedSwarmLeecherDistributionAlgorithm() {
        return new OrderedChunkedSwarmLeecherDistributionAlgorithm();
    }

    public static LeecherDistributionAlgorithm orderedLogarithmicLeecherDistributionAlgorithm() {
        return new OrderedLogarithmicLeecherDistributionAlgorithm();
    }
}
