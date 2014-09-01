package de.probst.ba.core.net.peer.peers;

/**
 * Created by chrisprobst on 17.08.14.
 */
public final class Peers {

    private Peers() {
    }

    /*
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
        for (Peer client : peers) {
            for (Peer server : peers) {
                if (server != client) {
                    client.connect(server.getLocalPeerId().getAddress());
                }
            }
        }
    }*/
}
