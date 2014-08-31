package de.probst.ba.core.net.peer.peers.netty.handlers.datainfo;

import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.peers.netty.NettyPeerId;
import de.probst.ba.core.net.peer.peers.netty.handlers.datainfo.messages.DataInfoMessage;
import de.probst.ba.core.util.Tuple;
import de.probst.ba.core.util.Tuple2;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;

import java.util.List;
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
                .map(CollectHandler::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(CollectHandler::getRemoteDataInfo)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(
                        Tuple::first,
                        Tuple::second));
    }

    public static Optional<CollectHandler> get(Channel remotePeer) {
        return Optional.ofNullable(remotePeer.pipeline().get(CollectHandler.class));
    }

    private final Peer peer;

    private volatile Optional<Tuple2<PeerId, Map<String, DataInfo>>> remoteDataInfo =
            Optional.empty();

    private Optional<Tuple2<PeerId, Map<String, DataInfo>>> getRemoteDataInfo() {
        return remoteDataInfo;
    }

    private void updateInterests(PeerId remotePeerId, Map<String, DataInfo> remoteDataInfo) {
        // Convert to empty data info
        List<DataInfo> dataInfoList = remoteDataInfo
                .values()
                .stream()
                .map(DataInfo::empty)
                .collect(Collectors.toList());

        // Try to add interest for all data info
        List<Boolean> succeeded = peer.getDataBase().addInterestsIf(
                dataInfoList, y -> peer.getBrain().addInterest(remotePeerId, y));

        for (int i = 0; i < succeeded.size(); i++) {
            if (succeeded.get(i)) {

                // DIAGNOSTIC
                peer.getDiagnostic().interestAdded(
                        peer, remotePeerId, dataInfoList.get(i));
            }
        }
    }

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
            // Not a valid message, we cannot load anything
            remoteDataInfo = Optional.empty();
        } else {
            // Update interests
            updateInterests(peerId, msg.getDataInfo().get());

            // Get map without empty data info
            Map<String, DataInfo> filteredRemoteDataInfo = msg.getDataInfo().get()
                    .entrySet()
                    .stream()
                    .filter(p -> !p.getValue().isEmpty())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue));

            // If the remote has only empty data info we cannot load anything
            remoteDataInfo = filteredRemoteDataInfo.isEmpty() ?
                    Optional.empty() : Optional.of(Tuple.of(peerId, filteredRemoteDataInfo));
        }

        // DIAGNOSTIC
        peer.getDiagnostic().collected(
                peer, peerId, getRemoteDataInfo().map(Tuple2::second));
    }

    public CollectHandler(Peer peer) {
        Objects.requireNonNull(peer);
        this.peer = peer;
    }
}
