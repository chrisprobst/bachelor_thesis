package de.probst.ba.core.net.peer.peers;

import de.probst.ba.core.logic.Brain;
import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.peers.netty.LocalNettyPeer;

import java.net.SocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * Created by chrisprobst on 17.08.14.
 */
public class Peers {

    private Peers() {
    }

    public static Peer localPeer(long uploadRate,
                                 long downloadRate,
                                 SocketAddress localAddress,
                                 DataBase dataBase,
                                 Brain brain) {
        return new LocalNettyPeer(uploadRate, downloadRate, localAddress, dataBase, brain);
    }

    public static void waitForInit(List<Peer> peers) throws ExecutionException, InterruptedException {
        Objects.requireNonNull(peers);
        for (Peer peer : peers) {
            peer.getInitFuture().get();
        }
    }

    public static void waitForClose(List<Peer> peers) throws ExecutionException, InterruptedException {
        Objects.requireNonNull(peers);
        for (Peer peer : peers) {
            peer.getCloseFuture().get();
        }
    }

    public static void connectGrid(List<Peer> peers) {
        Objects.requireNonNull(peers);
        for (Peer client : peers) {
            for (Peer server : peers) {
                if (server != client) {
                    client.connect(server.getNetworkState().getLocalAddress());
                }
            }
        }
    }
}
