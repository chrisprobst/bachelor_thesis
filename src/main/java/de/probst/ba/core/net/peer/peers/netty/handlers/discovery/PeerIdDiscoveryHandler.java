package de.probst.ba.core.net.peer.peers.netty.handlers.discovery;

import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.peers.netty.handlers.discovery.messages.PeerIdAnnounceMessage;
import de.probst.ba.core.net.peer.peers.netty.handlers.discovery.messages.PeerIdDiscoveryMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 06.09.14.
 */
public final class PeerIdDiscoveryHandler extends SimpleChannelInboundHandler<PeerIdAnnounceMessage> {

    public static List<PeerId> collectPeerIds(ChannelGroup channelGroup) {
        return Collections.unmodifiableList(channelGroup.stream()
                                                        .map(PeerIdDiscoveryHandler::get)
                                                        .map(PeerIdDiscoveryHandler::getPeerId)
                                                        .filter(Optional::isPresent)
                                                        .map(Optional::get)
                                                        .collect(Collectors.toList()));
    }

    public static PeerIdDiscoveryHandler get(Channel remotePeer) {
        return remotePeer.pipeline().get(PeerIdDiscoveryHandler.class);
    }

    private final ChannelGroup channelGroup;
    private volatile Optional<PeerId> peerId = Optional.empty();

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, PeerIdAnnounceMessage peerIdMessage)
            throws Exception {

        // Save the peer id
        this.peerId = Optional.ofNullable(peerIdMessage.getPeerId());

        // Send a discovery message to all peers
        channelGroup.writeAndFlush(new PeerIdDiscoveryMessage(collectPeerIds(channelGroup)));
    }

    public PeerIdDiscoveryHandler(ChannelGroup channelGroup) {
        Objects.requireNonNull(channelGroup);
        this.channelGroup = channelGroup;
    }

    public Optional<PeerId> getPeerId() {
        return peerId;
    }
}
