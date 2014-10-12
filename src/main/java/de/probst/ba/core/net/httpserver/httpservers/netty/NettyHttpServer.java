package de.probst.ba.core.net.httpserver.httpservers.netty;

import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.net.httpserver.HttpServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.io.IOException;

public final class NettyHttpServer implements HttpServer {

    private final Channel channel;

    public NettyHttpServer(EventLoopGroup eventLoopGroup, DataBase dataBase, int port) throws InterruptedException {
        channel = new ServerBootstrap().group(eventLoopGroup, eventLoopGroup)
                                       .channel(NioServerSocketChannel.class)
                                       .handler(new LoggingHandler(LogLevel.DEBUG))
                                       .childHandler(new NettyHttpServerInitializer(dataBase))
                                       .bind(port).channel();
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}