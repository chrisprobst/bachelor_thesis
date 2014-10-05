package de.probst.ba.core.net.http.stream;

import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.database.databases.DataBases;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.io.File;
import java.io.FileInputStream;
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
        File movie = new File("/Users/chrisprobst/Desktop/black.mp4");
        System.out.println(movie.length());
        DataInfo dataInfo = new DataInfo(0,
                                         movie.length(),
                                         Optional.of(movie.getName()),
                                         Optional.empty(),
                                         "Pseudo hash",
                                         40,
                                         String::valueOf).full();


        DataBase db = DataBases.inMemoryDataBase();
        db.insert(dataInfo, new FileInputStream(movie));
        System.out.println("Loaded movie into database, running http streaming now...");


        run(db);
    }
}