package de.probst.ba.core.net.peer.peers;

import de.probst.ba.core.diag.Diagnostic;
import de.probst.ba.core.logic.Brain;
import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.peers.netty.LocalNettyPeer;
import io.netty.channel.EventLoopGroup;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Created by chrisprobst on 17.08.14.
 */
public final class Peers {

    private Peers() {
    }

    public static Peer localPeer(long uploadRate,
                                 long downloadRate,
                                 SocketAddress localAddress,
                                 DataBase dataBase,
                                 Brain brain,
                                 Diagnostic diagnostic,
                                 Optional<EventLoopGroup> eventLoopGroup) {
        return new LocalNettyPeer(uploadRate, downloadRate, new PeerId(localAddress), dataBase, brain, diagnostic, eventLoopGroup);
    }

    public static void waitForInit(Collection<Peer> peers) throws ExecutionException, InterruptedException {
        Objects.requireNonNull(peers);
        for (Peer peer : peers) {
            peer.getInitFuture().get();
        }
    }

    public static void closeAndWait(Collection<Peer> peers) throws ExecutionException, InterruptedException {
        Objects.requireNonNull(peers);
        for (Peer peer : peers) {
            peer.close();
        }

        for (Peer peer : peers) {
            peer.getCloseFuture().get();
        }
    }

    public static void waitForClose(Collection<Peer> peers) throws ExecutionException, InterruptedException {
        Objects.requireNonNull(peers);
        for (Peer peer : peers) {
            peer.getCloseFuture().get();
        }
    }

    public static void connectGrid(Collection<Peer> peers) {
        Objects.requireNonNull(peers);
        for (Peer client : peers) {
            for (Peer server : peers) {
                if (server != client) {
                    client.connect(server.getNetworkState().getLocalPeerId().getAddress());
                }
            }
        }
    }
}
