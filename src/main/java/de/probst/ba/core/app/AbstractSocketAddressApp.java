package de.probst.ba.core.app;

import com.beust.jcommander.Parameter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Created by chrisprobst on 08.09.14.
 */
public abstract class AbstractSocketAddressApp extends AbstractPeerApp {

    @Parameter(names = {"-p", "--port"},
               description = "Port of the seeder (" + PortValidator.MSG + ")",
               validateValueWith = PortValidator.class)
    protected Integer port = 0;

    @Parameter(names = {"-h", "--host-name"},
               description = "Host name of the seeder",
               converter = HostNameConverter.class)
    protected InetAddress hostName = InetAddress.getByName("0.0.0.0");

    protected final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    protected InetSocketAddress getSocketAddress() {
        return new InetSocketAddress(hostName, port);
    }

    public AbstractSocketAddressApp() throws IOException {
    }
}
