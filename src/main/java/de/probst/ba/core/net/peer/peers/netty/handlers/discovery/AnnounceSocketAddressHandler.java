package de.probst.ba.core.net.peer.peers.netty.handlers.discovery;

import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.peers.netty.handlers.discovery.messages.SocketAddressMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Created by chrisprobst on 06.09.14.
 */
public final class AnnounceSocketAddressHandler extends SimpleChannelInboundHandler<SocketAddressMessage> {

    private final Leecher leecher;
    private final Optional<SocketAddress> announceSocketAddress;
    private Set<SocketAddress> lastSocketAddresses = Collections.emptySet();

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, SocketAddressMessage msg) throws Exception {
        Set<SocketAddress> socketAddresses = new HashSet<>(msg.getSocketAddresses());
        if (announceSocketAddress.isPresent()) {
            socketAddresses.removeIf(announceSocketAddress.get()::equals);
        }

        if (!socketAddresses.equals(lastSocketAddresses)) {
            lastSocketAddresses = socketAddresses;

            if (leecher.isAutoConnect()) {
                socketAddresses.forEach(leecher::connect);
            }

            // HANDLER
            leecher.getPeerHandler().discoveredSocketAddresses(leecher, socketAddresses);
        }
    }

    public AnnounceSocketAddressHandler(Leecher leecher, Optional<SocketAddress> announceSocketAddress) {
        Objects.requireNonNull(leecher);
        Objects.requireNonNull(announceSocketAddress);
        this.leecher = leecher;
        this.announceSocketAddress = announceSocketAddress;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        announceSocketAddress.map(SocketAddressMessage::new).ifPresent(ctx::writeAndFlush);
        super.channelActive(ctx);
    }
}
