package de.probst.ba.cli.args;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by chrisprobst on 11.10.14.
 */
public final class SeederPortArgs implements Args {

    private final Logger logger = LoggerFactory.getLogger(SeederPortArgs.class);

    @Parameter(names = {"-sp", "--seeder-port"},
               description = "The seeder port (" + Validators.PortValidator.MSG + ")",
               validateValueWith = Validators.PortValidator.class,
               required = true)
    public Integer seederPort;

    @Override
    public boolean check(JCommander jCommander) {
        logger.info(">>> [ Seeder Port Config ]");
        logger.info(">>> Seeder port:   " + seederPort);
        return true;
    }
}
