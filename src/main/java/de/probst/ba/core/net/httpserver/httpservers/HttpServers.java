package de.probst.ba.core.net.httpserver.httpservers;

import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.net.httpserver.HttpServer;
import de.probst.ba.core.net.httpserver.httpservers.netty.NettyHttpServer;
import io.netty.channel.EventLoopGroup;

/**
 * Created by chrisprobst on 12.10.14.
 */
public final class HttpServers {

    private HttpServers() {
    }

    public static HttpServer defaultHttpServer(EventLoopGroup eventLoopGroup, DataBase dataBase, int port)
            throws InterruptedException {
        return new NettyHttpServer(eventLoopGroup, dataBase, port);
    }
}
