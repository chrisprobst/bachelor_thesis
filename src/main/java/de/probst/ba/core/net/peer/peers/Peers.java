package de.probst.ba.core.net.peer.peers;

import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.Seeder;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * Created by chrisprobst on 17.08.14.
 */
public final class Peers {

    private Peers() {
    }


    public static void waitForInit(Collection<Peer> peers) throws ExecutionException, InterruptedException {
        Objects.requireNonNull(peers);
        for (Peer peer : peers) {
            peer.getInitFuture().get();
        }
    }

    public static void closeAndWait(Collection<Peer> peers) throws ExecutionException, InterruptedException, IOException {
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
        peers.stream().filter(leecher -> leecher instanceof Leecher).forEach(leecher -> {
            peers.stream().filter(seeder -> seeder instanceof Seeder).forEach(seeder -> {
                if (leecher != seeder) {
                    ((Leecher) leecher).connect(seeder.getPeerId().getAddress());
                }
            });
        });
    }
}
