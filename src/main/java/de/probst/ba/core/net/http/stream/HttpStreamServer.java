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

import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public final class HttpStreamServer {

    public static final int HTTP_STREAM_SERVER_PORT = 17000;

    public static void run(DataBase dataBase) throws InterruptedException {
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        try {
            new ServerBootstrap().group(eventLoopGroup, eventLoopGroup)
                                 .channel(NioServerSocketChannel.class)
                                 .handler(new LoggingHandler(LogLevel.DEBUG))
                                 .childHandler(new HttpStreamServerInitializer(dataBase))
                                 .bind(HTTP_STREAM_SERVER_PORT).sync().channel().closeFuture().sync();
        } finally {
            eventLoopGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        // Create database
        DataBase db = DataBases.fileDataBase(Paths.get("/Users/chrisprobst/Desktop/database1"));

        System.out.println("Loaded movie into database, running http streaming now...");
        Thread thread = new Thread(() -> {
            try {
                run(db);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread.start();

        Thread.sleep(5000);


        // Create data info of file
        try (FileChannel fileChannel = FileChannel.open(Paths.get("/Users/chrisprobst/Desktop/black.mp4"))) {
            List<DataInfo> dataInfo = DataInfo.fromPartitionedChannel(10,
                                                                      fileChannel.size(),
                                                                      Optional.of("black.mp4"),
                                                                      Optional.empty(),
                                                                      40,
                                                                      fileChannel);

            fileChannel.position(0);
            for (DataInfo di : dataInfo) {
                db.insertFromChannel(di, fileChannel, false);


                for (DataInfo di2 : db.getDataInfo().values()) {
                    System.out.println(di2);
                }

                Thread.sleep(2000);
            }

        }
    }
}