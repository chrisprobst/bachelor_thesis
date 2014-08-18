package de.probst.ba.core.net.peer.peers.netty.handlers.datainfo;

import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.peers.netty.handlers.datainfo.messages.DataInfoMessage;
import de.probst.ba.core.util.Tuple;
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

    public static Map<Object, Map<String, DataInfo>> getRemoteDataInfo(ChannelGroup channelGroup) {
        return channelGroup.stream()
                .map(c -> Tuple.of(c,
                        c.pipeline().get(CollectHandler.class).getRemoteDataInfo()))
                .filter(h -> h.second().isPresent())
                .collect(Collectors.toMap(
                        p -> p.first().id(),
                        p -> p.second().get()));
    }

    private final Peer peer;

    // All remote data info are stored here
    private volatile Optional<Map<String, DataInfo>> remoteDataInfo =
            Optional.empty();

    private boolean isDataInfoMessageValid(DataInfoMessage dataInfoMessage) {
        return dataInfoMessage != null &&
                dataInfoMessage.getDataInfo() != null &&
                dataInfoMessage.getDataInfo().isPresent() &&
                !dataInfoMessage.getDataInfo().get().isEmpty();
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, DataInfoMessage msg) throws Exception {
        if (!isDataInfoMessageValid(msg)) {
            remoteDataInfo = Optional.empty();
        } else {
            remoteDataInfo = Optional.of(Collections.unmodifiableMap(
                    new HashMap<>(msg.getDataInfo().get())));
        }

        getPeer().getDiagnostic().peerCollectedDataInfo(
                getPeer(), ctx.channel().id(), getRemoteDataInfo());
    }

    public CollectHandler(Peer peer) {
        Objects.requireNonNull(peer);
        this.peer = peer;
    }

    public Peer getPeer() {
        return peer;
    }

    public Optional<Map<String, DataInfo>> getRemoteDataInfo() {
        return remoteDataInfo;
    }
}
