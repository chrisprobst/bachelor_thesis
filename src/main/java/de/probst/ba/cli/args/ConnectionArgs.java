package de.probst.ba.cli.args;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by chrisprobst on 11.10.14.
 */
public final class ConnectionArgs implements Args {

    private final Logger logger = LoggerFactory.getLogger(ConnectionArgs.class);

    @Parameter(names = {"-mc", "--max-connections"},
               description = "Maximum number of connections per leecher (" + Validators.MaxConnectionsValidator.MSG +
                             "), 0 means no restriction",
               validateValueWith = Validators.MaxConnectionsValidator.class)
    public Integer maxLeecherConnections = 0;

    @Override
    public boolean check(JCommander jCommander) {
        logger.info(">>> [ Connection Config ]");
        logger.info(">>> Maximum connections per leecher:   " + maxLeecherConnections);
        return true;
    }
}
