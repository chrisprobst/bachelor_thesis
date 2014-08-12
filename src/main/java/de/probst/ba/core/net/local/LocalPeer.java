package de.probst.ba.core.net.local;

import de.probst.ba.core.net.AbstractPeer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;

/**
 * Created by chrisprobst on 12.08.14.
 */
public class LocalPeer extends AbstractPeer {

    @Override
    protected EventLoopGroup createEventGroup() {
        return new DefaultEventLoopGroup();
    }

    @Override
    protected <T extends ServerChannel> Class<T> getServerChannelClass() {
        return (Class<T>) LocalServerChannel.class;
    }

    @Override
    protected <T extends Channel> Class<T> getChannelClass() {
        return (Class<T>) LocalChannel.class;
    }

    public LocalPeer(String localAddress) {
        super(new LocalAddress(localAddress));
    }

    public ChannelFuture connect(String localAddress) {
        return connect(new LocalAddress(localAddress));
    }
}
