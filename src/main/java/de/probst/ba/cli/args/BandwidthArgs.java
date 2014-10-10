package de.probst.ba.cli.args;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by chrisprobst on 10.10.14.
 */
public final class BandwidthArgs implements Args {

    private final Logger logger = LoggerFactory.getLogger(BandwidthArgs.class);

    @Parameter(names = {"-u", "--upload-rate"},
               description = "The upload rate in bytes per second of the seeder(s) (" +
                             Validators.TransferRateValidator.MSG +
                             "), 0 means no restriction",
               validateValueWith = Validators.TransferRateValidator.class)
    public Integer maxUploadRate = 0;

    @Parameter(names = {"-d", "--download-rate"},
               description = "The download rate in bytes per second of the leecher(s) (" +
                             Validators.TransferRateValidator.MSG +
                             "), 0 means no restriction",
               validateValueWith = Validators.TransferRateValidator.class)
    public Integer maxDownloadRate = 0;

    public Integer getSmallestBandwidth() {
        int actualDownloadRate = maxDownloadRate > 0 ? maxDownloadRate : Integer.MAX_VALUE;
        int actualUploadRate = maxUploadRate > 0 ? maxUploadRate : Integer.MAX_VALUE;
        return Math.min(actualUploadRate, actualDownloadRate);
    }

    @Override
    public boolean check(JCommander jCommander) {
        logger.info(">>> [ Bandwidth Config ]");
        logger.info(">>> Maximal upload rate:   " + maxUploadRate);
        logger.info(">>> Maximal download rate: " + maxDownloadRate);
        return true;
    }
}
