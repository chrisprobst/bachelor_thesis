package de.probst.ba.core.net.peer.peers.netty.handlers.discovery;

import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.peers.netty.handlers.discovery.messages.PeerIdAnnounceMessage;
import de.probst.ba.core.net.peer.peers.netty.handlers.discovery.messages.PeerIdDiscoveryMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by chrisprobst on 06.09.14.
 */
public final class PeerIdAnnounceHandler extends SimpleChannelInboundHandler<PeerIdDiscoveryMessage> {

    public static final long RECONNECT_DELAY = 15000;

    private final Leecher leecher;
    private final Optional<PeerId> announcePeerId;
    private ScheduledFuture<?> reconnectFuture;
    private Set<PeerId> lastPeerIds = Collections.emptySet();
    private volatile ChannelHandlerContext ctx;

    private void connect(Set<PeerId> peerIds) {
        peerIds.forEach(leecher::connect);
    }

    private ScheduledFuture<?> scheduleReconnect() {
        return ctx.channel().eventLoop().scheduleWithFixedDelay(() -> connect(lastPeerIds),
                                                                RECONNECT_DELAY,
                                                                RECONNECT_DELAY,
                                                                TimeUnit.MILLISECONDS);
    }

    private void writePeerId() {
        announcePeerId.map(PeerIdAnnounceMessage::new).ifPresent(ctx::writeAndFlush);
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, PeerIdDiscoveryMessage msg) throws Exception {
        // Filter out our own announced address
        Set<PeerId> peerIds = new HashSet<>(msg.getPeerIds());
        if (announcePeerId.isPresent()) {
            peerIds.removeIf(peerId -> announcePeerId.get().getAddress().equals(peerId.getAddress()));
        }

        if (!peerIds.equals(lastPeerIds)) {
            lastPeerIds = peerIds;

            if (leecher.isAutoConnect()) {
                connect(peerIds);

                if (reconnectFuture == null) {
                    reconnectFuture = scheduleReconnect();
                }
            }

            // HANDLER
            leecher.getPeerHandler().discoveredPeers(leecher, peerIds);
        }
    }

    public PeerIdAnnounceHandler(Leecher leecher, Optional<PeerId> announcePeerId) {
        Objects.requireNonNull(leecher);
        Objects.requireNonNull(announcePeerId);
        this.leecher = leecher;
        this.announcePeerId = announcePeerId;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        writePeerId();
        super.channelActive(ctx);
    }
}
