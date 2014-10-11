package de.probst.ba.cli;

import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.database.databases.DataBases;
import de.probst.ba.core.net.http.stream.HttpStreamServer;
import io.netty.channel.nio.NioEventLoopGroup;

import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public final class HttpStreamServerApp {

    public static final int HTTP_STREAM_SERVER_PORT = 17000;

    public static void main(String[] args) throws Exception {
        // Create database
        DataBase db = DataBases.fileDataBase(Paths.get("/Users/chrisprobst/Desktop/database1"));

        System.out.println("Loaded movie into database, running http streaming now...");
        Thread thread = new Thread(() -> {
            try {
                new HttpStreamServer(new NioEventLoopGroup(), db, HTTP_STREAM_SERVER_PORT);
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
                                                                      1,
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