package de.probst.ba.core.net.peer.peers.netty;

import de.probst.ba.core.diag.Diagnostic;
import de.probst.ba.core.logic.Brain;
import de.probst.ba.core.media.DataBase;
import io.netty.channel.Channel;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;

import java.net.SocketAddress;
import java.util.Optional;

/**
 * Created by chrisprobst on 12.08.14.
 */
public final class LocalNettyPeer extends AbstractServerClientNettyPeer {

    @Override
    protected Class<? extends Channel> getChannelClass() {
        return LocalChannel.class;
    }

    @Override
    protected Class<? extends ServerChannel> getServerChannelClass() {
        return LocalServerChannel.class;
    }

    @Override
    protected EventLoopGroup createEventGroup() {
        return new DefaultEventLoopGroup();
    }

    public LocalNettyPeer(long uploadRate,
                          long downloadRate,
                          SocketAddress localAddress,
                          DataBase dataBase,
                          Brain brain,
                          Diagnostic diagnostic,
                          Optional<EventLoopGroup> eventLoopGroup) {
        super(uploadRate, downloadRate, localAddress, dataBase, brain, diagnostic, eventLoopGroup);
    }
}
