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
    private final Optional<PeerId> peerId;
    private volatile ChannelHandlerContext ctx;

    public PeerIdAnnounceHandler(Leecher leecher, Optional<PeerId> peerId) {
        Objects.requireNonNull(leecher);
        Objects.requireNonNull(peerId);
        this.leecher = leecher;
        this.peerId = peerId;
    }

    public void writePeerId() {
        peerId.map(PeerIdAnnounceMessage::new).ifPresent(ctx::writeAndFlush);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        writePeerId();
        super.channelActive(ctx);
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, PeerIdDiscoveryMessage msg) throws Exception {
        msg.getPeerIds().stream().map(PeerId::getAddress).forEach(leecher::connect);
    }
}
