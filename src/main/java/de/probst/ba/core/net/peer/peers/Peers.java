package de.probst.ba.core.net.peer.peers;

import de.probst.ba.core.logic.Brain;
import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.peers.netty.LocalNettyPeer;

import java.net.SocketAddress;

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
}
