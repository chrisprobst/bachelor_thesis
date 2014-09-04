package de.probst.ba.core.net.peer.peers.netty.handlers.datainfo;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.peers.netty.NettyPeerId;
import de.probst.ba.core.net.peer.peers.netty.handlers.datainfo.messages.DataInfoMessage;
import de.probst.ba.core.util.collections.Tuple;
import de.probst.ba.core.util.collections.Tuple2;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by chrisprobst on 11.08.14.
 */
public final class CollectHandler extends SimpleChannelInboundHandler<DataInfoMessage> {

    public static Map<PeerId, Map<String, DataInfo>> getRemoteDataInfo(ChannelGroup channelGroup) {
        return Collections.unmodifiableMap(channelGroup.stream()
                                                       .map(CollectHandler::get)
                                                       .map(CollectHandler::getRemoteDataInfo)
                                                       .filter(Optional::isPresent)
                                                       .map(Optional::get)
                                                       .collect(Collectors.toMap(Tuple::first, Tuple::second)));
    }

    public static CollectHandler get(Channel remotePeer) {
        return remotePeer.pipeline().get(CollectHandler.class);
    }

    private final Logger logger = LoggerFactory.getLogger(CollectHandler.class);
    private final Leecher leecher;
    private Optional<Map<String, DataInfo>> lastRemoteDataInfo = Optional.empty();
    private volatile Optional<Tuple2<PeerId, Map<String, DataInfo>>> remoteDataInfo = Optional.empty();

    public CollectHandler(Leecher leecher) {
        Objects.requireNonNull(leecher);
        this.leecher = leecher;
    }


    private Optional<Tuple2<PeerId, Map<String, DataInfo>>> getRemoteDataInfo() {
        return remoteDataInfo;
    }

    private void updateInterests(PeerId remotePeerId, Map<String, DataInfo> remoteDataInfo) {
        // Convert to empty data info
        List<DataInfo> dataInfoList =
                remoteDataInfo.values().stream().map(DataInfo::empty).collect(Collectors.toList());

        // Try to add interest for all data info
        List<Boolean> succeeded = leecher.getDataBase().addInterestsIf(dataInfoList,
                                                                       y -> leecher.getDistributionAlgorithm()
                                                                                   .addInterest(leecher,
                                                                                                remotePeerId,
                                                                                                y));

        IntStream.range(0, succeeded.size())
                 .filter(succeeded::get)
                 .mapToObj(dataInfoList::get)
                 .forEach(addedDataInfo -> {

                     logger.info("Leecher " + leecher.getPeerId() + " added interest for " + addedDataInfo + " from " +
                                 remotePeerId);

                     // HANDLER
                     leecher.getPeerHandler().interestAdded(leecher, remotePeerId, addedDataInfo);
                 });
    }

    private boolean isDataInfoMessageValid(DataInfoMessage dataInfoMessage) {
        return dataInfoMessage != null && dataInfoMessage.getDataInfo() != null &&
               dataInfoMessage.getDataInfo().isPresent() && !dataInfoMessage.getDataInfo().get().isEmpty();
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
            Map<String, DataInfo> filteredRemoteDataInfo = msg.getDataInfo()
                                                              .get()
                                                              .entrySet()
                                                              .stream()
                                                              .filter(p -> !p.getValue().isEmpty())
                                                              .collect(Collectors.toMap(Map.Entry::getKey,
                                                                                        Map.Entry::getValue));

            // If the remote has only empty data info we cannot load anything
            remoteDataInfo = filteredRemoteDataInfo.isEmpty() ?
                             Optional.empty() :
                             Optional.of(Tuple.of(peerId, Collections.unmodifiableMap(filteredRemoteDataInfo)));
        }

        Optional<Map<String, DataInfo>> dataInfo = remoteDataInfo.map(Tuple2::second);

        logger.debug("Leecher " + leecher.getPeerId() + " collected " + dataInfo + " from " + peerId);

        // HANDLER
        leecher.getPeerHandler().collected(leecher, peerId, dataInfo);

        // Leech if we received something which is different from the last received value
        dataInfo.filter(m -> !m.isEmpty()).filter(m -> !m.equals(lastRemoteDataInfo)).ifPresent(m -> leecher.leech());

        // Save last remote data info
        lastRemoteDataInfo = dataInfo;
    }
}
