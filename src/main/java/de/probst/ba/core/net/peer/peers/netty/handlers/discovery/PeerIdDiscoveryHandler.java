package de.probst.ba.core.net.peer.peers.netty.handlers.discovery;

import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.peers.netty.handlers.discovery.messages.PeerIdAnnounceMessage;
import de.probst.ba.core.net.peer.peers.netty.handlers.discovery.messages.PeerIdDiscoveryMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public final class PeerIdDiscoveryHandler extends SimpleChannelInboundHandler<PeerIdAnnounceMessage> {

    /**
     * This delay determines in milliseconds how often the discovered
     * peers will be exchanged.
     */
    public static final long DISCOVERY_EXCHANGE_DELAY = 1000;

    public static Set<PeerId> collectPeerIds(ChannelGroup channelGroup) {
        return Collections.unmodifiableSet(channelGroup.stream()
                                                       .map(PeerIdDiscoveryHandler::get)
                                                       .map(PeerIdDiscoveryHandler::getPeerId)
                                                       .filter(Optional::isPresent)
                                                       .map(Optional::get)
                                                       .collect(Collectors.toSet()));
    }

    public static PeerIdDiscoveryHandler get(Channel remotePeer) {
        return remotePeer.pipeline().get(PeerIdDiscoveryHandler.class);
    }

    private final Logger logger = LoggerFactory.getLogger(PeerIdDiscoveryHandler.class);
    private final Seeder seeder;
    private final ChannelGroup channelGroup;
    private ChannelHandlerContext ctx;
    private ChannelFuture writeFuture;
    private Set<PeerId> lastPeerIds = Collections.emptySet();
    private volatile Optional<PeerId> peerId = Optional.empty();

    private void writePeerIds() {
        if (writeFuture != null) {
            return;
        }

        Set<PeerId> peerIds = new HashSet<>(collectPeerIds(channelGroup));
        peerIds.add(seeder.getPeerId());

        // Do not announce the same peers over and over again
        if (!peerIds.equals(lastPeerIds)) {
            lastPeerIds = peerIds;
            (writeFuture = ctx.writeAndFlush(new PeerIdDiscoveryMessage(peerIds))).addListener(fut -> {
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
        ctx.channel().eventLoop().schedule(this::writePeerIds, DISCOVERY_EXCHANGE_DELAY, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, PeerIdAnnounceMessage peerIdMessage)
            throws Exception {

        // Save the peer id
        this.peerId = Optional.ofNullable(peerIdMessage.getPeerId());

        // HANDLER
        if (this.peerId.isPresent()) {
            writePeerIds();
            seeder.getPeerHandler().discoveredPeer(seeder, this.peerId.get());
        }
    }

    public PeerIdDiscoveryHandler(Seeder seeder, ChannelGroup channelGroup) {
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

    public Optional<PeerId> getPeerId() {
        return peerId;
    }
}
