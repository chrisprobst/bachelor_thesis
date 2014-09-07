package de.probst.ba.core.net.peer.peers.netty.handlers.discovery;

import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.peers.netty.handlers.discovery.messages.PeerIdAnnounceMessage;
import de.probst.ba.core.net.peer.peers.netty.handlers.discovery.messages.PeerIdDiscoveryMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Objects;
import java.util.Optional;

/**
 * Created by chrisprobst on 06.09.14.
 */
public final class PeerIdAnnounceHandler extends SimpleChannelInboundHandler<PeerIdDiscoveryMessage> {

    private final Leecher leecher;
    private final Optional<PeerId> announcePeerId;
    private volatile ChannelHandlerContext ctx;

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, PeerIdDiscoveryMessage msg) throws Exception {
        msg.getPeerIds()
           .stream()
           .filter(peerId -> !announcePeerId.isPresent() || !announcePeerId.get().equals(peerId))
           .forEach(leecher::connect);
    }

    public PeerIdAnnounceHandler(Leecher leecher, Optional<PeerId> announcePeerId) {
        Objects.requireNonNull(leecher);
        Objects.requireNonNull(announcePeerId);
        this.leecher = leecher;
        this.announcePeerId = announcePeerId;
    }

    public void writePeerId() {
        announcePeerId.map(PeerIdAnnounceMessage::new).ifPresent(ctx::writeAndFlush);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        writePeerId();
        super.channelActive(ctx);
    }
}
