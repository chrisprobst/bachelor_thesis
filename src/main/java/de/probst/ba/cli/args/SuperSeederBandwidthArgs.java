package de.probst.ba.cli.args;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by chrisprobst on 10.10.14.
 */
public final class SuperSeederBandwidthArgs implements Args {

    private final Logger logger = LoggerFactory.getLogger(BandwidthArgs.class);

    @Parameter(names = {"-su", "--super-seeder-upload-rate"},
               description = "The upload rate in bytes per second of the super seeder (" +
                             Validators.TransferRateValidator.MSG +
                             "), 0 means no restriction",
               validateValueWith = Validators.TransferRateValidator.class)
    public Integer maxSuperSeederUploadRate = 0;

    @Parameter(names = {"-sd", "--super-seeder-download-rate"},
               description = "The download rate in bytes per second of the super seeder (" +
                             Validators.TransferRateValidator.MSG +
                             "), 0 means no restriction",
               validateValueWith = Validators.TransferRateValidator.class)
    public Integer maxSuperSeederDownloadRate = 0;

    public Integer getSmallestBandwidth() {
        int actualUploadRate = maxSuperSeederUploadRate > 0 ? maxSuperSeederUploadRate : Integer.MAX_VALUE;
        int actualDownloadRate = maxSuperSeederDownloadRate > 0 ? maxSuperSeederDownloadRate : Integer.MAX_VALUE;
        return Math.min(actualUploadRate, actualDownloadRate);
    }

    @Override
    public boolean check(JCommander jCommander) {
        logger.info(">>> [ SuperSeeder Bandwidth Config ]");
        logger.info(">>> Max upload rate:   " + maxSuperSeederUploadRate);
        logger.info(">>> Max download rate: " + maxSuperSeederDownloadRate);
        return true;
    }
}
