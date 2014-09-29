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

    private static volatile int maxConnectionsPerLeecher = 0;
    private static volatile int uploadBufferSize = 8192;
    private static final long messageQueueResetDelay = 2500;
    private static final long discoveryExchangeDelay = 1000;
    private static final long announceDelay = 200;
    private static volatile boolean useCodec;

    public static void setMaxConnectionsPerLeecher(int maxConnectionsPerLeecher) {
        if (maxConnectionsPerLeecher < 0) {
            throw new IllegalArgumentException("maxConnectionsPerLeecher < 0");
        }
        NettyConfig.maxConnectionsPerLeecher = maxConnectionsPerLeecher;
    }

    public static int getMaxConnectionsPerLeecher() {
        return maxConnectionsPerLeecher;
    }

    public static int getUploadBufferSize() {
        return uploadBufferSize;
    }

    public static void setUploadBufferSize(int uploadBufferSize) {
        if (uploadBufferSize <= 0) {
            throw new IllegalArgumentException("uploadBufferSize <= 0");
        }
        int powerOf2 = 1, nextPowerOf2 = powerOf2;
        while ((nextPowerOf2 <<= 1) < uploadBufferSize) {
            powerOf2 = nextPowerOf2;
        }
        NettyConfig.uploadBufferSize = powerOf2;
    }

    public static long getMessageQueueResetDelay() {
        return messageQueueResetDelay;
    }

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
