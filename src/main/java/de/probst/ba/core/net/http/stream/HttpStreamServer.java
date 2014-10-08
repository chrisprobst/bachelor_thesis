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
                                 .handler(new LoggingHandler(LogLevel.INFO))
                                 .childHandler(new HttpStreamServerInitializer(dataBase))
                                 .bind(HTTP_STREAM_SERVER_PORT).sync().channel().closeFuture().sync();
        } finally {
            eventLoopGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        // Create database
        DataBase db = DataBases.memoryDataBase();

        // Create data info of file
        try (FileChannel fileChannel = FileChannel.open(Paths.get("/Users/chrisprobst/Desktop/black.mp4"))) {

            // Create partitioned data info
            List<DataInfo> dataInfo = DataInfo.fromPartitionedChannel(10,
                                                                      fileChannel.size(),
                                                                      Optional.of("black.mp4"),
                                                                      Optional.empty(),
                                                                      40,
                                                                      fileChannel);

            // Insert all partitions into database
            fileChannel.position(0);

            db.insertManyFromPartitionedChannel(dataInfo, fileChannel);
        }

        for (DataInfo di : db.getDataInfo().values()) {
            System.out.println(di);
        }



/*
        // Create database
        DataBase db = DataBases.memoryDataBase();

        // Insert tuple
        IOUtil.transfer(tuple.second(), db.insert(tuple.first()).get());

        System.out.println("Loaded movie into database, running http streaming now...");
        run(db);*/
    }
}