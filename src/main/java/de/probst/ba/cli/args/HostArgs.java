package de.probst.ba.cli.args;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Created by chrisprobst on 11.10.14.
 */
public final class HostArgs implements Args {

    private final Logger logger = LoggerFactory.getLogger(HostArgs.class);

    @Parameter(names = {"-hn", "--host-name"},
               description = "The host name",
               converter = Converters.HostNameConverter.class,
               required = true)
    public InetAddress hostName;

    @Parameter(names = {"-hp", "--host-port"},
               description = "The host port (" + Validators.PortValidator.MSG + ")",
               validateValueWith = Validators.PortValidator.class,
               required = true)
    public Integer hostPort;

    public SocketAddress getSocketAddress() {
        return new InetSocketAddress(hostName, hostPort);
    }

    @Override
    public boolean check(JCommander jCommander) {
        logger.info(">>> [ Host Config ]");
        logger.info(">>> Host name: " + hostName);
        logger.info(">>> Host port: " + hostPort);
        return true;
    }
}
