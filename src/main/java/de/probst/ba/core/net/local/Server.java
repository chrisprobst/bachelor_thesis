package de.probst.ba.core.net.local;

import de.probst.ba.core.logic.DataInfo;
import de.probst.ba.core.net.handlers.DataInfoHandler;
import de.probst.ba.core.net.handlers.messages.DataInfoMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by chrisprobst on 12.08.14.
 */
public class Server {

    public static void main(String[] args) throws InterruptedException {

        // Used for all
        DataInfoHandler dataInfoHandler = new DataInfoHandler();

        final LocalAddress addr = new LocalAddress("server-1");

        EventLoopGroup serverGroup = new DefaultEventLoopGroup();
        EventLoopGroup clientGroup = new DefaultEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(serverGroup)
                    .channel(LocalServerChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<LocalChannel>() {
                        @Override
                        public void initChannel(LocalChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(dataInfoHandler);
                        }
                    });

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(addr).sync();


            // Start one client
            Bootstrap cb = new Bootstrap();
            cb.group(clientGroup)
                    .channel(LocalChannel.class)
                    .handler(new ChannelInitializer<LocalChannel>() {
                        @Override
                        public void initChannel(LocalChannel ch) throws Exception {
                            ch.pipeline().addLast(
                                    new LoggingHandler(LogLevel.INFO),
                                    dataInfoHandler);
                        }
                    });


            // Start the client.
            Channel ch = cb.connect(addr).sync().channel();

            Map<String, DataInfo> map = new HashMap<>();
            map.put("Hello world", new DataInfo("Hello world", 1000, 10));

            ch.writeAndFlush(new DataInfoMessage(map));

            Thread.sleep(100);

            dataInfoHandler.getRemoteDataInfo().entrySet().stream()
                    .forEach(p -> System.out.println(((ChannelId) p.getKey()).asLongText() + " -> " + p.getValue()));


            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } finally {
            serverGroup.shutdownGracefully();
            clientGroup.shutdownGracefully();
        }

    }
}
