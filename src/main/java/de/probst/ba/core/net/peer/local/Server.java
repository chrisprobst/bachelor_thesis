package de.probst.ba.core.net.peer.local;

import de.probst.ba.core.logic.Brain;
import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.media.databases.DefaultDataBase;
import de.probst.ba.core.net.peer.handlers.transfer.DownloadHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;

import java.util.Optional;

/**
 * Created by chrisprobst on 12.08.14.
 */
public class Server {

    public static void main(String[] args) throws InterruptedException {

        // Demo data
        DataInfo dataInfo = new DataInfo(1000 * 1000,
                Optional.empty(),
                Optional.empty(),
                "Hello world",
                100, String::valueOf)
                .withChunk(3)
                .withChunk(6)
                .withChunk(7)
                .withChunk(55)
                .withChunk(12)
                .withChunk(51)
                .withChunk(33);

        Brain defaultBrain = new Brain() {
        };

        // Create both clients
        LocalNettyPeer localPeerA = new LocalNettyPeer("peer-1", new DefaultDataBase(dataInfo), defaultBrain);
        LocalNettyPeer localPeerB = new LocalNettyPeer("peer-2", new DefaultDataBase(), defaultBrain);

        // Wait for init
        localPeerA.getInitFuture().sync();
        localPeerB.getInitFuture().sync();

        // Connect both clients
        localPeerA.connect("peer-2").sync();
        localPeerB.connect("peer-1").sync();


        Thread.sleep(4000);

        // Receive announced data info
        localPeerA.getRemoteDataInfo().entrySet().stream()
                .forEach(p -> {
                    System.out.println("Peer A requests -> " + ((ChannelId) p.getKey()).asLongText() + " -> " + p.getValue());
                });

        // Receive announced data info
        localPeerB.getRemoteDataInfo().entrySet().stream()
                .limit(1)
                .forEach(p -> {
                    System.out.println("Peer B requests -> " + ((ChannelId) p.getKey()).asLongText() + " -> " + p.getValue());

                    // Locate the uploader
                    Channel uploader = localPeerB.getChannelGroup().find((ChannelId) p.getKey());

                    DataInfo dataInfoDownload = p.getValue().values().iterator().next();
                    localPeerB.getDataBase().addInterest(dataInfoDownload.empty());

                    // Register a new transfer
                    DownloadHandler.request(localPeerB.getDataBase(), uploader, dataInfo);
                });


        for (int i = 0; i < 100; i++) {

            // Receive announced data info
            localPeerB.getRemoteDataInfo().entrySet().stream()
                    .forEach(p -> {
                        System.out.println("Peer B requests -> " + ((ChannelId) p.getKey()).asLongText() + " -> " + p.getValue());
                    });

            // Receive announced data info
            localPeerA.getRemoteDataInfo().entrySet().stream()
                    .forEach(p -> {
                        System.out.println("Peer A requests -> " + ((ChannelId) p.getKey()).asLongText() + " -> " + p.getValue());
                    });

            Thread.sleep(1000);
        }

        localPeerA.getEventLoopGroup().shutdownGracefully();
        localPeerB.getEventLoopGroup().shutdownGracefully();

        localPeerA.getChannelGroup().forEach(System.out::println);
    }
}
