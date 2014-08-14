package de.probst.ba.core.net.peer.handlers.datainfo;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.probst.ba.core.Config;
import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.peer.handlers.datainfo.messages.DataInfoMessage;
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
 * and you have a consistent snapshot of all available remote
 * data info.
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

    // The map view
    private final Map<Object, Map<String, DataInfo>> unmodifiableRemoteDataInfo =
            Collections.unmodifiableMap(remoteDataInfo.asMap());

    /**
     * Checks the message.
     *
     * @param dataInfoMessage
     * @return
     */
    private boolean isDataInfoMessageValid(DataInfoMessage dataInfoMessage) {
        return dataInfoMessage != null &&
                dataInfoMessage.getDataInfo() != null;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, DataInfoMessage msg) throws Exception {
        // Check message
        if (isDataInfoMessageValid(msg)) {

            if (msg.getDataInfo().isEmpty()) {
                // If the map is empty, we remove the mapping
                remoteDataInfo.invalidate(ctx.channel().id());
            } else {
                // Update the cache with the new map
                remoteDataInfo.put(
                        ctx.channel().id(),
                        Collections.unmodifiableMap(msg.getDataInfo()));
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        remoteDataInfo.invalidate(ctx.channel().id());
        super.channelInactive(ctx);
    }

    /**
     * @return An unmodifiable map with all remote data info.
     */
    public Map<Object, Map<String, DataInfo>> getRemoteDataInfo() {
        return unmodifiableRemoteDataInfo;
    }
}
