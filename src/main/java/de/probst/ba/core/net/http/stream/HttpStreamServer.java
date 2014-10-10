package de.probst.ba.core.net.http.stream;

import de.probst.ba.core.media.database.DataBase;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.io.Closeable;
import java.io.IOException;

public final class HttpStreamServer implements Closeable {

    private final Channel channel;

    public HttpStreamServer(EventLoopGroup eventLoopGroup, DataBase dataBase, int port) throws InterruptedException {
        channel = new ServerBootstrap().group(eventLoopGroup, eventLoopGroup)
                                       .channel(NioServerSocketChannel.class)
                                       .handler(new LoggingHandler(LogLevel.DEBUG))
                                       .childHandler(new HttpStreamServerInitializer(dataBase))
                                       .bind(port).channel();
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}