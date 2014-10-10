package de.probst.ba.cli.args;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;

/**
 * Created by chrisprobst on 10.10.14.
 */
public abstract class ArgsApp implements Args {

    protected abstract void start() throws Exception;

    public void parse(String[] args) throws Exception {
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
        JCommander jCommander = new JCommander(this);

        try {
            jCommander.parse(args);
            if (check(jCommander)) {
                start();
            } else {
                System.out.println();
                jCommander.usage();
            }
        } catch (ParameterException e) {
            System.out.println(e.getMessage());
            System.out.println();
            jCommander.usage();
        }
    }
}
