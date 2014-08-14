package de.probst.ba.core.net.peer.local;

import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.media.databases.DefaultDataBase;
import de.probst.ba.core.net.Transfer;
import de.probst.ba.core.net.peer.handlers.transfer.DownloadHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by chrisprobst on 12.08.14.
 */
public class Server {

    public static void main(String[] args) throws InterruptedException {

        // Demo data
        DataInfo dataInfo = new DataInfo(1000,
                Optional.empty(),
                Optional.empty(),
                "Hello world",
                11, String::valueOf)
                .withChunk(3)
                .withChunk(6)
                .withChunk(7);

        // Create both clients
        LocalPeer localPeerA = new LocalPeer("peer-1", new DefaultDataBase(dataInfo));
        LocalPeer localPeerB = new LocalPeer("peer-2", new DefaultDataBase());

        // Wait for init
        localPeerA.getInitFuture().sync();
        localPeerB.getInitFuture().sync();

        // Connect both clients
        localPeerA.connect("peer-2").sync();
        localPeerB.connect("peer-1").sync();


        for (int i = 0; i < 5; i++) {

            Thread.sleep(1000);

            // Receive announced data info
            localPeerB.getRemoteDataInfo().entrySet().stream()
                    .forEach(p -> {
                        System.out.println("Peer B requests -> " + ((ChannelId) p.getKey()).asLongText() + " -> " + p.getValue());

                        // Locate the uploader
                        Channel uploader = localPeerB.getChannelGroup().find((ChannelId) p.getKey());



                        // Register a new transfer
                        uploader.pipeline().addLast(
                                new DownloadHandler(localPeerB.getDataBase(),
                                new Transfer(uploader.id(), p.getValue().values().iterator().next())));
                    });


            Thread.sleep(2000);
        }

        localPeerA.getEventLoopGroup().shutdownGracefully();
        localPeerB.getEventLoopGroup().shutdownGracefully();

        localPeerA.getChannelGroup().forEach(System.out::println);
    }
}
