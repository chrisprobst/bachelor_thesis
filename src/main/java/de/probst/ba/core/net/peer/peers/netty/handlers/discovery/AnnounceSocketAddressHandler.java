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
    private volatile ChannelHandlerContext ctx;

    private void connect(Set<SocketAddress> socketAddresses) {
        socketAddresses.forEach(leecher::connect);
    }

    private void writeSocketAddress() {
        announceSocketAddress.map(SocketAddressMessage::new).ifPresent(ctx::writeAndFlush);
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, SocketAddressMessage msg) throws Exception {
        Set<SocketAddress> socketAddresses = new HashSet<>(msg.getSocketAddresses());
        if (announceSocketAddress.isPresent()) {
            socketAddresses.removeIf(announceSocketAddress.get()::equals);
        }

        if (!socketAddresses.equals(lastSocketAddresses)) {
            lastSocketAddresses = socketAddresses;

            if (leecher.isAutoConnect()) {
                connect(socketAddresses);
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
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        writeSocketAddress();
        super.channelActive(ctx);
    }
}
