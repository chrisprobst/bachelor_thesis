package de.probst.ba.core.net.peer.peers.netty;

import de.probst.ba.core.net.peer.PeerId;
import io.netty.channel.Channel;

import java.util.Optional;

/**
 * Created by chrisprobst on 18.08.14.
 */
public class NettyPeerId extends PeerId {

    public NettyPeerId(Channel channel) {
        super(Optional.of(channel.remoteAddress()), channel.id());
    }
}
