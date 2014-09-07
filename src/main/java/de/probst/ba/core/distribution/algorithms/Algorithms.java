package de.probst.ba.core.distribution.algorithms;

import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.distribution.SeederDistributionAlgorithm;

/**
 * Created by chrisprobst on 01.09.14.
 */
public final class Algorithms {

    public enum AlgorithmType {
        Logarithmic, ChunkedSwarm
    }

    private Algorithms() {
    }

    public static LeecherDistributionAlgorithm getLeecherDistributionAlgorithm(AlgorithmType algorithmType) {
        return algorithmType == AlgorithmType.Logarithmic ?
               new OrderedLogarithmicLeecherDistributionAlgorithm() :
               new OrderedChunkedSwarmLeecherDistributionAlgorithm();
    }

    public static SeederDistributionAlgorithm getSeederDistributionAlgorithm(AlgorithmType algorithmType) {
        return algorithmType == AlgorithmType.Logarithmic ?
               new LimitedSeederDistributionAlgorithm(1) :
               new DefaultSeederDistributionAlgorithm();
    }
}
