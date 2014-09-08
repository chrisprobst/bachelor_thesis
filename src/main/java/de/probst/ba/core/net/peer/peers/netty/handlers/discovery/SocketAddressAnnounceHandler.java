package de.probst.ba.core.net.peer.peers.netty.handlers.discovery;

import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.peers.netty.handlers.discovery.messages.SocketAddressAnnounceMessage;
import de.probst.ba.core.net.peer.peers.netty.handlers.discovery.messages.SocketAddressDiscoveryMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.ScheduledFuture;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by chrisprobst on 06.09.14.
 */
public final class SocketAddressAnnounceHandler extends SimpleChannelInboundHandler<SocketAddressDiscoveryMessage> {

    public static final long RECONNECT_DELAY = 15000;

    private final Leecher leecher;
    private final Optional<SocketAddress> announceSocketAddress;
    private ScheduledFuture<?> reconnectFuture;
    private Set<SocketAddress> lastSocketAddresses = Collections.emptySet();
    private volatile ChannelHandlerContext ctx;

    private void connect(Set<SocketAddress> socketAddresses) {
        socketAddresses.forEach(leecher::connect);
    }

    private ScheduledFuture<?> scheduleReconnect() {
        return ctx.channel().eventLoop().scheduleWithFixedDelay(() -> connect(lastSocketAddresses),
                                                                RECONNECT_DELAY,
                                                                RECONNECT_DELAY,
                                                                TimeUnit.MILLISECONDS);
    }

    private void doAnnounceSocketAddress() {
        announceSocketAddress.map(SocketAddressAnnounceMessage::new).ifPresent(ctx::writeAndFlush);
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, SocketAddressDiscoveryMessage msg) throws Exception {
        Set<SocketAddress> socketAddresses = new HashSet<>(msg.getSocketAddresses());
        if (announceSocketAddress.isPresent()) {
            socketAddresses.removeIf(announceSocketAddress.get()::equals);
        }

        if (!socketAddresses.equals(lastSocketAddresses)) {
            lastSocketAddresses = socketAddresses;

            if (leecher.isAutoConnect()) {
                connect(socketAddresses);

                if (reconnectFuture == null) {
                    reconnectFuture = scheduleReconnect();
                }
            }

            // HANDLER
            leecher.getPeerHandler().discoveredSocketAddresses(leecher, socketAddresses);
        }
    }

    public SocketAddressAnnounceHandler(Leecher leecher, Optional<SocketAddress> announceSocketAddress) {
        Objects.requireNonNull(leecher);
        Objects.requireNonNull(announceSocketAddress);
        this.leecher = leecher;
        this.announceSocketAddress = announceSocketAddress;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        doAnnounceSocketAddress();
        super.channelActive(ctx);
    }
}
