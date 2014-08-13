package de.probst.ba.core.net.peer.handlers;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.probst.ba.core.Config;
import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.peer.handlers.messages.DataInfoMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Collections;
import java.util.Map;

/**
 * Represents a sharable data info handler which accumulates
 * all incoming data info messages and stores them according
 * to the channel id they come from.
 * <p>
 * In other words: Put this handler in all of your pipelines
 * and you have a consistent snapshot of all available data info.
 * <p>
 * Created by chrisprobst on 11.08.14.
 */
@ChannelHandler.Sharable
public final class DataInfoHandler extends SimpleChannelInboundHandler<DataInfoMessage> {

    // All remote data info are stored here
    private final Cache<Object, Map<String, DataInfo>> remoteDataInfo = CacheBuilder.newBuilder()
            .expireAfterWrite(
                    Config.getRemoteDataInfoExpirationDelay(),
                    Config.getRemoteDataInfoExpirationDelayTimeUnit())
            .build();

    private final Map<Object, Map<String, DataInfo>> unmodifiableRemoteDataInfo =
            Collections.unmodifiableMap(remoteDataInfo.asMap());

    private boolean isDataInfoMessageValid(DataInfoMessage dataInfoMessage) {
        return dataInfoMessage != null &&
                dataInfoMessage.getDataInfo() != null &&
                !dataInfoMessage.getDataInfo().isEmpty();
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, DataInfoMessage msg) throws Exception {
        if (isDataInfoMessageValid(msg)) {
            remoteDataInfo.put(
                    ctx.channel().id(),
                    Collections.unmodifiableMap(msg.getDataInfo()));
        }
    }

    public Map<Object, Map<String, DataInfo>> getRemoteDataInfo() {
        return unmodifiableRemoteDataInfo;
    }
}
