package de.probst.ba.core.net.peer.peers.netty;

import de.probst.ba.core.net.peer.peers.netty.handlers.codec.SimpleCodec;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Created by chrisprobst on 15.09.14.
 */
public final class NettyConfig {
    private NettyConfig() {
    }

    private static final long discoveryExchangeDelay = 1000;
    private static final long announceDelay = 250;
    private static volatile boolean useCodec;

    public static long getDiscoveryExchangeDelay() {
        return discoveryExchangeDelay;
    }

    public static long getAnnounceDelay() {
        return announceDelay;
    }

    public static boolean isUseCodec() {
        return useCodec;
    }

    public static void setUseCodec(boolean useCodec) {
        NettyConfig.useCodec = useCodec;
    }


    public static Collection<ChannelHandler> getCodecPipeline() {
        if (useCodec) {
            return Arrays.asList(new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4),
                                 new LengthFieldPrepender(4),
                                 new SimpleCodec());
        } else {
            return Collections.emptyList();
        }
    }
}
