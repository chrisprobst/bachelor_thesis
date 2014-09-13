package de.probst.ba.core.net.peer.peers.netty.handlers.datainfo;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.AbstractLeecher;
import de.probst.ba.core.net.peer.PeerId;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 11.08.14.
 */
public final class CollectDataInfoHandler extends SimpleChannelInboundHandler<DataInfoMessage> {

    public static Map<PeerId, Map<String, DataInfo>> collectRemoteDataInfo(ChannelGroup channelGroup) {
        return Collections.unmodifiableMap(channelGroup.stream()
                                                       .map(CollectDataInfoHandler::get)
                                                       .map(CollectDataInfoHandler::getRemoteDataInfo)
                                                       .filter(Optional::isPresent)
                                                       .map(Optional::get)
                                                       .collect(Collectors.toMap(Tuple::first, Tuple::second)));
    }

    public static CollectDataInfoHandler get(Channel remotePeer) {
        return remotePeer.pipeline().get(CollectDataInfoHandler.class);
    }

    private final Logger logger = LoggerFactory.getLogger(CollectDataInfoHandler.class);
    private final AbstractLeecher leecher;

    private Map<String, DataInfo> lastRemoteDataInfo = Collections.emptyMap();
    private Map<String, DataInfo> lastNonEmptyRemoteDataInfo = Collections.emptyMap();
    private volatile Optional<Tuple2<PeerId, Map<String, DataInfo>>> externalRemoteDataInfo = Optional.empty();

    private Optional<Tuple2<PeerId, Map<String, DataInfo>>> getRemoteDataInfo() {
        return externalRemoteDataInfo;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, DataInfoMessage msg) throws Exception {
        PeerId peerId = new PeerId(ctx.channel().remoteAddress(), ctx.channel().id());
        Map<String, DataInfo> remoteDataInfo = msg.getDataInfo();

        // Ignore identical remote data info
        if (remoteDataInfo.equals(lastRemoteDataInfo)) {
            return;
        }

        // Set last remote data info
        lastRemoteDataInfo = Collections.unmodifiableMap(remoteDataInfo);

        // Set external remote data info
        externalRemoteDataInfo = Optional.of(Tuple.of(peerId, lastRemoteDataInfo));

        // Create non empty remote data info
        Map<String, DataInfo> nonEmptyRemoteDataInfo = remoteDataInfo.entrySet()
                                                                     .stream()
                                                                     .filter(p -> !p.getValue().isEmpty())
                                                                     .collect(Collectors.toMap(Map.Entry::getKey,
                                                                                               Map.Entry::getValue));

        // Suggest leeching if there are new non empty data info
        if (!nonEmptyRemoteDataInfo.equals(lastNonEmptyRemoteDataInfo)) {
            leecher.leech();
        }

        // Set last non empty remote data info
        lastNonEmptyRemoteDataInfo = Collections.unmodifiableMap(nonEmptyRemoteDataInfo);

        logger.debug("Leecher " + leecher.getPeerId() + " collected " + remoteDataInfo + " from " + peerId);

        // HANDLER
        leecher.getPeerHandler().collected(leecher, peerId, remoteDataInfo);
    }

    public CollectDataInfoHandler(AbstractLeecher leecher) {
        Objects.requireNonNull(leecher);
        this.leecher = leecher;
    }

    /**
     * Removes the given remove data info from the remote data info.
     * This is useful if you requested an upload which was rejected.
     * This way you can stop the leecher from re-requesting the same
     * data info over and over again.
     * <p>
     * Please note that the seeder has to re-announce the rejected data info
     * later if the leecher should be able to request the data info again.
     * <p>
     * Also note that this method can and should only be invoked by this event loop.
     *
     * @param removeDataInfo
     */
    public void removeDataInfo(DataInfo removeDataInfo) {
        Objects.requireNonNull(removeDataInfo);
        if (!externalRemoteDataInfo.isPresent()) {
            return;
        }

        Map<String, DataInfo> remoteDataInfo = new HashMap<>(lastRemoteDataInfo);

        // Lookup up existing data info
        DataInfo existingDataInfo = remoteDataInfo.get(removeDataInfo.getHash());
        if (existingDataInfo != null) {
            // Subtract the remove data info from the existing data info and put back into map
            remoteDataInfo.put(existingDataInfo.getHash(), existingDataInfo.subtract(removeDataInfo));
        }

        // Update the last remote data info
        lastRemoteDataInfo = Collections.unmodifiableMap(remoteDataInfo);

        Map<String, DataInfo> nonEmptyRemoteDataInfo = remoteDataInfo.entrySet()
                                                                     .stream()
                                                                     .filter(p -> !p.getValue().isEmpty())
                                                                     .collect(Collectors.toMap(Map.Entry::getKey,
                                                                                               Map.Entry::getValue));

        // Update the last non empty remote data info
        lastNonEmptyRemoteDataInfo = Collections.unmodifiableMap(nonEmptyRemoteDataInfo);

        // Update the external remote data info
        externalRemoteDataInfo = Optional.of(Tuple.of(externalRemoteDataInfo.get().first(),
                                                      lastRemoteDataInfo));
    }
}
