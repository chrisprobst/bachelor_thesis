package de.probst.ba.core.net.peer.peers.netty.handlers.datainfo;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.AbstractLeecher;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 11.08.14.
 */
public final class CollectHandler extends SimpleChannelInboundHandler<DataInfoMessage> {

    public static Map<PeerId, Map<String, DataInfo>> collectRemoteDataInfo(ChannelGroup channelGroup) {
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
    private final AbstractLeecher leecher;

    private Map<String, DataInfo> lastRemoteDataInfo = Collections.emptyMap();
    private Map<String, DataInfo> lastNonEmptyRemoteDataInfo = Collections.emptyMap();
    private Map<String, DataInfo> remoteDataInfo;

    private volatile Optional<Tuple2<PeerId, Map<String, DataInfo>>> externalRemoteDataInfo = Optional.empty();

    public CollectHandler(AbstractLeecher leecher) {
        Objects.requireNonNull(leecher);
        this.leecher = leecher;
    }

    private Optional<Tuple2<PeerId, Map<String, DataInfo>>> getRemoteDataInfo() {
        return externalRemoteDataInfo;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, DataInfoMessage msg) throws Exception {
        // An invalid message is kind of a bug or
        // shows that the remote peer can not be trusted
        if (msg == null || !msg.isValid()) {
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

        // Look for new data info if the key sets differ
        if (!lastRemoteDataInfo.keySet().equals(remoteDataInfo.keySet())) {
            leecher.lookFor(remoteDataInfo.values().stream().map(DataInfo::empty).collect(Collectors.toSet()));
        }

        // Set last remote data info
        lastRemoteDataInfo = remoteDataInfo;

        // Update external remote data info
        externalRemoteDataInfo = Optional.of(Tuple.of(peerId, Collections.unmodifiableMap(remoteDataInfo)));

        // Create non empty remote data info
        Map<String, DataInfo> nonEmptyRemoteDataInfo = remoteDataInfo.entrySet()
                                                                     .stream()
                                                                     .filter(p -> !p.getValue().isEmpty())
                                                                     .collect(Collectors.toMap(Map.Entry::getKey,
                                                                                               Map.Entry::getValue));

        // Suggest leeching if there are new non empty data info
        if (!lastNonEmptyRemoteDataInfo.equals(nonEmptyRemoteDataInfo)) {
            leecher.leech();
        }

        // Set last non empty remote data info
        lastNonEmptyRemoteDataInfo = nonEmptyRemoteDataInfo;

        logger.debug("Leecher " + leecher.getPeerId() + " collected " + remoteDataInfo + " from " + peerId);

        // HANDLER
        leecher.getPeerHandler().collected(leecher, peerId, remoteDataInfo);
    }
}
