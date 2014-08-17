package de.probst.ba.core;

import de.probst.ba.core.logic.Brain;
import de.probst.ba.core.logic.brains.DefaultTotalOrderedBrain;
import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.media.databases.DefaultDataBase;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.peers.Peers;
import io.netty.channel.local.LocalAddress;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Created by chrisprobst on 12.08.14.
 */
public class App {

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        // Demo data
        DataInfo dataInfo = new DataInfo(
                0,
                1000 * 1000,
                Optional.empty(),
                Optional.empty(),
                "Hello world",
                60,
                String::valueOf)
                .withChunk(3)
                .withChunk(6)
                .withChunk(7)
                .withChunk(55)
                .withChunk(12)
                .withChunk(51)
                .withChunk(33);

        DataInfo dataInfo1 = new DataInfo(
                1,
                1000 * 1000,
                Optional.empty(),
                Optional.empty(),
                "Hello world 2",
                100,
                String::valueOf)
                .withChunk(3)
                .withChunk(6)
                .withChunk(7)
                .withChunk(55)
                .withChunk(12)
                .withChunk(51)
                .withChunk(33);

        DataInfo dataInfo2 = dataInfo.withoutChunk(55);

        Brain defaultBrain = new DefaultTotalOrderedBrain();
        Brain defaultBrain2 = new DefaultTotalOrderedBrain();

        // Create both clients
        Peer localPeerA = Peers.localPeer(1000, 1000,
                new LocalAddress("peer-1"),
                new DefaultDataBase(dataInfo, dataInfo1),
                defaultBrain2);

        Peer localPeerB = Peers.localPeer(1000, 1000,
                new LocalAddress("peer-2"),
                new DefaultDataBase(dataInfo2),
                defaultBrain);

        // Wait for init
        localPeerA.getInitFuture().join();
        localPeerB.getInitFuture().join();

        // Connect both clients
        localPeerA.connect(new LocalAddress("peer-2"));
        localPeerB.connect(new LocalAddress("peer-1"));

        localPeerA.getCloseFuture().get();
        localPeerB.getCloseFuture().get();

/*
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

        localPeerA.getChannelGroup().forEach(System.out::println);*/
    }
}
