package de.probst.ba.core.net.local;

import de.probst.ba.core.logic.DataInfo;
import io.netty.channel.ChannelId;

/**
 * Created by chrisprobst on 12.08.14.
 */
public class Server {

    public static void main(String[] args) throws InterruptedException {

        // Create both clients
        LocalPeer localPeerA = new LocalPeer("peer-1");
        LocalPeer localPeerB = new LocalPeer("peer-2");

        // Wait for init
        localPeerA.getInitFuture().sync();
        localPeerB.getInitFuture().sync();

        // Connect both clients
        localPeerA.connect("peer-2").sync();
        localPeerB.connect("peer-1").sync();

        // Demo data
        DataInfo dataInfo = new DataInfo(1000, "Hello world", 11, String::valueOf)
                .withChunk(3)
                .withChunk(6)
                .withChunk(7);

        // Put in map
        localPeerA.getDataInfo().put(dataInfo.getHash(), dataInfo);


        while (true) {
            Thread.sleep(1000);

            // Receive announced data info
            localPeerB.getRemoteDataInfo().entrySet().stream()
                    .forEach(p -> System.out.println("PEER B -> " + ((ChannelId) p.getKey()).asLongText() + " -> " + p.getValue()));

            // Receive announced data info
            localPeerA.getRemoteDataInfo().entrySet().stream()
                    .forEach(p -> System.out.println("PEER A -> " + ((ChannelId) p.getKey()).asLongText() + " -> " + p.getValue()));
        }
    }
}
