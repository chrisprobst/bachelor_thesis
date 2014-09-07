package de.probst.ba.core.app;

import com.beust.jcommander.Parameter;

import java.net.InetAddress;

/**
 * Created by chrisprobst on 07.09.14.
 */
public class SeederApp extends AbstractPeerApp {

    @Parameter(names = {"-p", "--port"},
               description = "Port of the seeder (" + PortValidator.MSG + ")",
               validateValueWith = PortValidator.class)
    private Integer port = 10000;

    @Parameter(names = {"-h", "--host-name"},
               description = "Host name of the seeder",
               converter = HostNameConverter.class)
    private InetAddress hostName = InetAddress.getLocalHost();

    public SeederApp() throws Exception {
    }

    @Override
    protected void start() {

    }

    public static void main(String[] args) throws Exception {
        new SeederApp().parse(args);
    }
}
