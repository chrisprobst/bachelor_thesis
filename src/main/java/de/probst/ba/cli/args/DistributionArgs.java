package de.probst.ba.cli.args;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.probst.ba.core.distribution.DistributionAlgorithm;
import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.distribution.algorithms.Algorithms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by chrisprobst on 11.10.14.
 */
public final class DistributionArgs implements Args {

    private final Logger logger = LoggerFactory.getLogger(DistributionAlgorithm.class);

    @Parameter(names = {"-at", "--algorithm-type"},
               description = "Distribution algorithm type [SuperSeederChunkedSwarm, ChunkedSwarm, Logarithmic, Sequential]",
               converter = Converters.AlgorithmTypeConverter.class,
               required = true)
    public Algorithms.AlgorithmType algorithmType;

    public SeederDistributionAlgorithm getSuperSeederDistributionAlgorithm() {
        return Algorithms.getSuperSeederOnlyDistributionAlgorithm(algorithmType);
    }

    public SeederDistributionAlgorithm getSeederDistributionAlgorithm() {
        return Algorithms.getSeederDistributionAlgorithm(algorithmType);
    }

    public LeecherDistributionAlgorithm getLeecherDistributionAlgorithm() {
        return Algorithms.getLeecherDistributionAlgorithm(algorithmType);
    }

    @Override
    public boolean check(JCommander jCommander) {
        logger.info(">>> [ Distribution Config ]");
        logger.info(">>> Algorithm type:    " + algorithmType);
        return true;
    }
}
