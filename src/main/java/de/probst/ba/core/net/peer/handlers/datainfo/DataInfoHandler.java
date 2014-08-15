package de.probst.ba.core.net.peer.handlers.datainfo;

import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.peer.handlers.datainfo.messages.DataInfoMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents a sharable data info handler which accumulates
 * all incoming data info messages and stores them according
 * to the channel id they come from.
 * <p>
 * In other words: Put this handler in all of your pipelines
 * and you have a consistent snapshot of all available remote
 * data info.
 * <p>
 * Created by chrisprobst on 11.08.14.
 */
public final class DataInfoHandler extends SimpleChannelInboundHandler<DataInfoMessage> {

    public static Map<Object, Map<String, DataInfo>> getRemoteDataInfo(ChannelGroup channelGroup) {
        return channelGroup.stream()
                .map(c -> new AbstractMap.SimpleEntry<>(
                        c, c.pipeline().get(DataInfoHandler.class).getRemoteDataInfo()))
                .filter(h -> h.getValue().isPresent())
                .collect(Collectors.toMap(
                        p -> p.getKey().id(),
                        p -> p.getValue().get()));
    }

    // All remote data info are stored here
    private volatile Optional<Map<String, DataInfo>> remoteDataInfo =
            Optional.empty();

    private boolean isDataInfoMessageValid(DataInfoMessage dataInfoMessage) {
        return dataInfoMessage != null &&
                dataInfoMessage.getDataInfo() != null &&
                !dataInfoMessage.getDataInfo().isEmpty();
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, DataInfoMessage msg) throws Exception {
        if (!isDataInfoMessageValid(msg)) {
            remoteDataInfo = Optional.empty();
        } else {
            remoteDataInfo = Optional.of(Collections.unmodifiableMap(
                    new HashMap<>(msg.getDataInfo())));
        }
    }

    public Optional<Map<String, DataInfo>> getRemoteDataInfo() {
        return remoteDataInfo;
    }
}
