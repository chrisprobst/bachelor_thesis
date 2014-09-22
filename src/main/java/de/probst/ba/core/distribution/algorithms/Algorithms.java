package de.probst.ba.core.distribution.algorithms;

import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.distribution.SeederDistributionAlgorithm;

/**
 * Created by chrisprobst on 01.09.14.
 */
public final class Algorithms {

    public enum AlgorithmType {
        SuperSeederChunkedSwarm, ChunkedSwarm, Logarithmic, Sequential
    }

    private Algorithms() {
    }

    public static LeecherDistributionAlgorithm getLeecherDistributionAlgorithm(AlgorithmType algorithmType) {
        switch (algorithmType) {
            case SuperSeederChunkedSwarm:
            case ChunkedSwarm:
                return new OrderedChunkedSwarmLeecherDistributionAlgorithm();
            case Logarithmic:
            case Sequential:
                return new OrderedSingleMostLeecherDistributionAlgorithm();
            default:
                throw new UnsupportedOperationException("Not supported yet: " + algorithmType);
        }
    }

    public static SeederDistributionAlgorithm getSeederDistributionAlgorithm(AlgorithmType algorithmType) {
        switch (algorithmType) {
            case SuperSeederChunkedSwarm:
            case ChunkedSwarm:
            case Sequential:
                return new DefaultSeederDistributionAlgorithm();
            case Logarithmic:
                return new LimitedSeederDistributionAlgorithm(1);
            default:
                throw new UnsupportedOperationException("Not supported yet: " + algorithmType);
        }
    }

    public static SeederDistributionAlgorithm getSuperSeederOnlyDistributionAlgorithm(AlgorithmType algorithmType) {
        switch (algorithmType) {
            case SuperSeederChunkedSwarm:
                return new SuperSeederDistributionAlgorithm();
            default:
                return getSeederDistributionAlgorithm(algorithmType);
        }
    }
}
