package de.probst.ba.core.net.http.stream;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public final class HttpStreamServer {

    public static final int HTTP_STREAM_SERVER_PORT = 17000;

    public static void run() throws InterruptedException {
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        try {
            new ServerBootstrap().group(eventLoopGroup, eventLoopGroup)
                                 .channel(NioServerSocketChannel.class)
                                 .handler(new LoggingHandler(LogLevel.DEBUG))
                                 .childHandler(new HttpStreamServerInitializer())
                                 .bind(HTTP_STREAM_SERVER_PORT).sync().channel().closeFuture().sync();
        } finally {
            eventLoopGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        run();
    }
}