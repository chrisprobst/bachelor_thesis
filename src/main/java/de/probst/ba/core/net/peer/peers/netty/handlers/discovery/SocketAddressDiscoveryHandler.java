package de.probst.ba.core.net.peer.peers.netty.handlers.discovery;

import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.peers.netty.handlers.discovery.messages.SocketAddressAnnounceMessage;
import de.probst.ba.core.net.peer.peers.netty.handlers.discovery.messages.SocketAddressDiscoveryMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 06.09.14.
 */
public final class SocketAddressDiscoveryHandler extends SimpleChannelInboundHandler<SocketAddressAnnounceMessage> {

    /**
     * This delay determines in milliseconds how often the discovered
     * peers will be exchanged.
     */
    public static final long DISCOVERY_EXCHANGE_DELAY = 1000;

    public static Set<SocketAddress> collectSocketAddresses(ChannelGroup channelGroup) {
        return Collections.unmodifiableSet(channelGroup.stream()
                                                       .map(SocketAddressDiscoveryHandler::get)
                                                       .map(SocketAddressDiscoveryHandler::getSocketAddress)
                                                       .filter(Optional::isPresent)
                                                       .map(Optional::get)
                                                       .collect(Collectors.toSet()));
    }

    public static SocketAddressDiscoveryHandler get(Channel remotePeer) {
        return remotePeer.pipeline().get(SocketAddressDiscoveryHandler.class);
    }

    private final Logger logger = LoggerFactory.getLogger(SocketAddressDiscoveryHandler.class);
    private final Seeder seeder;
    private final ChannelGroup channelGroup;
    private ChannelHandlerContext ctx;
    private ChannelFuture writeFuture;
    private Set<SocketAddress> lastSocketAddresses = Collections.emptySet();
    private volatile Optional<SocketAddress> socketAddress = Optional.empty();

    private void doAnnounceSocketAddresses() {
        if (writeFuture != null) {
            return;
        }

        Set<SocketAddress> socketAddresses = new HashSet<>(collectSocketAddresses(channelGroup));
        socketAddresses.add(seeder.getPeerId().getSocketAddress().get());

        // Do not announce the same socket addresses over and over again
        if (!socketAddresses.equals(lastSocketAddresses)) {
            lastSocketAddresses = socketAddresses;
            (writeFuture = ctx.writeAndFlush(new SocketAddressDiscoveryMessage(socketAddresses))).addListener(fut -> {
                writeFuture = null;
                if (fut.isSuccess()) {
                    schedule();
                } else if (!(fut.cause() instanceof ClosedChannelException)) {
                    // Close if this exception was not expected
                    ctx.close();

                    logger.warn(
                            "Seeder " + seeder.getPeerId() + " failed to write discovery message, connection closed",
                            fut.cause());
                }
            });
        }
    }

    private void schedule() {
        ctx.channel()
           .eventLoop()
           .schedule(this::doAnnounceSocketAddresses, DISCOVERY_EXCHANGE_DELAY, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, SocketAddressAnnounceMessage msg)
            throws Exception {
        Optional<SocketAddress> newSocketAddress = Optional.of(msg.getSocketAddress());

        if (!this.socketAddress.equals(newSocketAddress)) {
            this.socketAddress = newSocketAddress;
            doAnnounceSocketAddresses();

            // HANDLER
            seeder.getPeerHandler().discoveredSocketAddress(seeder, newSocketAddress.get());
        }
    }

    public SocketAddressDiscoveryHandler(Seeder seeder, ChannelGroup channelGroup) {
        Objects.requireNonNull(seeder);
        Objects.requireNonNull(channelGroup);
        this.seeder = seeder;
        this.channelGroup = channelGroup;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        schedule();
        super.channelActive(ctx);
    }

    public Optional<SocketAddress> getSocketAddress() {
        return socketAddress;
    }
}
