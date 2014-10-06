package de.probst.ba.core.net.http.stream;

import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.database.databases.DataBases;
import de.probst.ba.core.util.collections.Tuple2;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.Optional;

public final class HttpStreamServer {

    public static final int HTTP_STREAM_SERVER_PORT = 17000;

    public static void run(DataBase dataBase) throws InterruptedException {
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        try {
            new ServerBootstrap().group(eventLoopGroup, eventLoopGroup)
                                 .channel(NioServerSocketChannel.class)
                                 .handler(new LoggingHandler(LogLevel.INFO))
                                 .childHandler(new HttpStreamServerInitializer(dataBase))
                                 .bind(HTTP_STREAM_SERVER_PORT).sync().channel().closeFuture().sync();
        } finally {
            eventLoopGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        // Create data info of file
        Tuple2<DataInfo, FileChannel> tuple = DataInfo.fromFile(0,
                                                                Optional.of("RobinHood.mp4"),
                                                                Optional.empty(),
                                                                40,
                                                                Paths.get("/Users/chrisprobst/Desktop/RobinHood.mp4"));

        // Create database
        DataBase db = DataBases.inMemoryDataBase();

        // Insert tuple
        db.insert(tuple.first(), tuple.second());

        System.out.println("Loaded movie into database, running http streaming now...");
        run(db);
    }
}