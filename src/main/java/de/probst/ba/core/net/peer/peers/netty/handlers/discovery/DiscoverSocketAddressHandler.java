package de.probst.ba.core.net.peer.peers.netty.handlers.discovery;

import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.peers.netty.handlers.discovery.messages.SocketAddressMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
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
public final class DiscoverSocketAddressHandler extends SimpleChannelInboundHandler<SocketAddressMessage> {

    /**
     * This delay determines in milliseconds how often the discovered
     * peers will be exchanged.
     */
    public static final long DISCOVERY_EXCHANGE_DELAY = 1000;

    public static Set<SocketAddress> collectSocketAddresses(ChannelGroup channelGroup) {
        return Collections.unmodifiableSet(channelGroup.stream()
                                                       .map(DiscoverSocketAddressHandler::get)
                                                       .map(DiscoverSocketAddressHandler::getSocketAddress)
                                                       .filter(Optional::isPresent)
                                                       .map(Optional::get)
                                                       .collect(Collectors.toSet()));
    }

    public static DiscoverSocketAddressHandler get(Channel remotePeer) {
        return remotePeer.pipeline().get(DiscoverSocketAddressHandler.class);
    }

    private final Logger logger = LoggerFactory.getLogger(DiscoverSocketAddressHandler.class);
    private final Seeder seeder;
    private final ChannelGroup channelGroup;

    private Future<?> scheduleFuture;
    private ChannelFuture writeFuture;
    private Set<SocketAddress> lastSocketAddresses = Collections.emptySet();
    private volatile Optional<SocketAddress> socketAddress = Optional.empty();

    private void schedule(ChannelHandlerContext ctx) {
        scheduleFuture = ctx.channel().eventLoop().schedule(() -> writeSocketAddresses(ctx, true),
                                                            DISCOVERY_EXCHANGE_DELAY,
                                                            TimeUnit.MILLISECONDS);
    }

    private void writeSocketAddresses(ChannelHandlerContext ctx, boolean scheduled) {
        if (scheduled) {
            // Set schedule future to null if this was invoked by
            // the scheduler
            scheduleFuture = null;
        }

        if (writeFuture != null) {
            if (scheduled) {
                // Do schedule again if this was invoked by
                // the scheduler
                schedule(ctx);
            }

            // Do not write if there is a pending write request
            return;
        }

        // Collect all known socket addresses and add them into a set including our own address
        Set<SocketAddress> socketAddresses = new HashSet<>(collectSocketAddresses(channelGroup));
        socketAddresses.add(seeder.getPeerId().getSocketAddress().get());

        // Do not announce the same socket addresses over and over again
        if (!socketAddresses.equals(lastSocketAddresses)) {
            lastSocketAddresses = socketAddresses;

            // Write and flush and save the write future
            (writeFuture = ctx.writeAndFlush(new SocketAddressMessage(socketAddresses))).addListener(fut -> {
                if (fut.isSuccess()) {
                    // The write operation completed,
                    // set the future to null
                    writeFuture = null;

                    if (scheduled) {
                        // Do schedule again if this was invoked by
                        // the scheduler
                        schedule(ctx);
                    }
                }
            });
        } else if (scheduled) {
            // Do schedule again if this was invoked by
            // the scheduler
            schedule(ctx);
        }
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, SocketAddressMessage msg)
            throws Exception {
        Optional<SocketAddress> newSocketAddress = msg.getSocketAddresses().stream().findAny();

        if (newSocketAddress.isPresent() && !this.socketAddress.equals(newSocketAddress)) {
            this.socketAddress = newSocketAddress;

            writeSocketAddresses(ctx, false);

            // HANDLER
            seeder.getPeerHandler().discoveredSocketAddress(seeder, newSocketAddress.get());
        }
    }

    public DiscoverSocketAddressHandler(Seeder seeder, ChannelGroup channelGroup) {
        Objects.requireNonNull(seeder);
        Objects.requireNonNull(channelGroup);
        this.seeder = seeder;
        this.channelGroup = channelGroup;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (scheduleFuture != null) {
            scheduleFuture.cancel(false);
        }
        super.channelInactive(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        schedule(ctx);
        super.channelActive(ctx);
    }

    public Optional<SocketAddress> getSocketAddress() {
        return socketAddress;
    }
}
