package de.probst.ba.core.net.peer.peers.netty.handlers.datainfo;

import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.peers.netty.NettyPeerId;
import de.probst.ba.core.net.peer.peers.netty.handlers.datainfo.messages.DataInfoMessage;
import de.probst.ba.core.util.Tuple;
import de.probst.ba.core.util.Tuple2;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 11.08.14.
 */
public final class CollectHandler extends SimpleChannelInboundHandler<DataInfoMessage> {

    public static Map<PeerId, Map<String, DataInfo>> getRemoteDataInfo(ChannelGroup channelGroup) {
        return channelGroup.stream()
                .map(c -> c.pipeline().get(CollectHandler.class).getRemoteDataInfo())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(
                        Tuple::first,
                        Tuple::second));
    }

    private final Peer peer;

    // All remote data info are stored here
    private volatile Optional<Tuple2<PeerId, Map<String, DataInfo>>> remoteDataInfo =
            Optional.empty();

    private boolean isDataInfoMessageValid(DataInfoMessage dataInfoMessage) {
        return dataInfoMessage != null &&
                dataInfoMessage.getDataInfo() != null &&
                dataInfoMessage.getDataInfo().isPresent() &&
                !dataInfoMessage.getDataInfo().get().isEmpty();
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, DataInfoMessage msg) throws Exception {
        PeerId peerId = new NettyPeerId(ctx.channel());

        if (!isDataInfoMessageValid(msg)) {
            remoteDataInfo = Optional.empty();
        } else {
            remoteDataInfo = Optional.of(Tuple2.of(
                    peerId,
                    Collections.unmodifiableMap(new HashMap<>(msg.getDataInfo().get()))));
        }

        getPeer().getDiagnostic().peerCollectedDataInfo(
                getPeer(), peerId, getRemoteDataInfo().map(Tuple2::second));
    }

    public CollectHandler(Peer peer) {
        Objects.requireNonNull(peer);
        this.peer = peer;
    }

    public Peer getPeer() {
        return peer;
    }

    public Optional<Tuple2<PeerId, Map<String, DataInfo>>> getRemoteDataInfo() {
        return remoteDataInfo;
    }
}
