package de.probst.ba.cli.args;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

/**
 * Created by chrisprobst on 11.10.14.
 */
public final class SeederHostArgs implements Args {

    private final Logger logger = LoggerFactory.getLogger(SeederHostArgs.class);

    @Parameter(names = {"-sh", "--seeder-host-name"},
               description = "The seeder host name",
               converter = Converters.HostNameConverter.class,
               required = true)
    public InetAddress seederHostName;

    @Parameter(names = {"-sp", "--seeder-port"},
               description = "The seeder port (" + Validators.PortValidator.MSG + ")",
               validateValueWith = Validators.PortValidator.class,
               required = true)
    public Integer seederHostPort;

    @Override
    public boolean check(JCommander jCommander) {
        logger.info(">>> [ Seeder Port Config ]");
        logger.info(">>> Seeder host name: " + seederHostName);
        logger.info(">>> Seeder host port:   " + seederHostPort);
        return true;
    }
}
