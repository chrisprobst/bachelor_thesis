package de.probst.ba.cli.args;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by chrisprobst on 11.10.14.
 */
public final class DataBaseHttpServerArgs implements Args {

    private final Logger logger = LoggerFactory.getLogger(DataBaseHttpServerArgs.class);

    @Parameter(names = {"-ds", "--database-http-server"},
               description = "Run a database http server")
    public Boolean runDataBaseHttpServer = false;

    @Parameter(names = {"-htp", "--http-port"},
               description = "The http port (" + Validators.PortValidator.MSG + ")",
               validateValueWith = Validators.PortValidator.class)
    public Integer httpServerPort = 8080;

    @Override
    public boolean check(JCommander jCommander) {
        logger.info(">>> [ DataBase HTTP Server Config ]");
        logger.info(">>> Run database http server:  " + runDataBaseHttpServer);
        logger.info(">>> HTTP server port:          " + httpServerPort);
        return true;
    }
}
