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

    private Map<String, DataInfo> lastRemoteDataInfo = Collections.emptyMap();
    private Map<String, DataInfo> lastFilteredRemoteDataInfo = Collections.emptyMap();
    private Map<String, DataInfo> remoteDataInfo;

    private volatile Optional<Tuple2<PeerId, Map<String, DataInfo>>> externalRemoteDataInfo = Optional.empty();

    public CollectHandler(Leecher leecher) {
        Objects.requireNonNull(leecher);
        this.leecher = leecher;
    }


    private Optional<Tuple2<PeerId, Map<String, DataInfo>>> getRemoteDataInfo() {
        return externalRemoteDataInfo;
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
        return dataInfoMessage != null && dataInfoMessage.getDataInfo() != null;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, DataInfoMessage msg) throws Exception {
        // An invalid message is kind of a bug or
        // shows that the remote peer can not be trusted
        if (!isDataInfoMessageValid(msg)) {
            ctx.close();
            logger.warn("Leecher " + leecher.getPeerId() + " received invalid data info message, connection closed");
            return;
        }

        PeerId peerId = new NettyPeerId(ctx.channel());
        remoteDataInfo = msg.getDataInfo();

        // Ignore identical remote data info
        if (lastRemoteDataInfo.equals(remoteDataInfo)) {
            return;
        }

        // Set last remote data info
        lastRemoteDataInfo = remoteDataInfo;

        // Check new interests
        if (!remoteDataInfo.isEmpty()) {
            updateInterests(peerId, msg.getDataInfo());
        }

        // Get map without empty data info
        Map<String, DataInfo> filteredRemoteDataInfo = remoteDataInfo.entrySet()
                                                                     .stream()
                                                                     .filter(p -> !p.getValue().isEmpty())
                                                                     .collect(Collectors.toMap(Map.Entry::getKey,
                                                                                               Map.Entry::getValue));

        // Ignore identical filtered remote data info
        if (lastFilteredRemoteDataInfo.equals(filteredRemoteDataInfo)) {
            return;
        }

        // Set last filtered remote data info
        lastFilteredRemoteDataInfo = filteredRemoteDataInfo;

        // Update external remote data info
        externalRemoteDataInfo = filteredRemoteDataInfo.isEmpty() ?
                                 Optional.empty() :
                                 Optional.of(Tuple.of(peerId, Collections.unmodifiableMap(filteredRemoteDataInfo)));

        // Suggest leeching if there are new non empty data info
        if (!filteredRemoteDataInfo.isEmpty()) {
            leecher.leech();
        }

        logger.debug("Leecher " + leecher.getPeerId() + " collected " + remoteDataInfo + " from " + peerId);

        // HANDLER
        leecher.getPeerHandler().collected(leecher, peerId, remoteDataInfo);
    }
}
