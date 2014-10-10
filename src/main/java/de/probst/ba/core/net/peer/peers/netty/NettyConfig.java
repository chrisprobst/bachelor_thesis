package de.probst.ba.core.net.peer.peers.netty;

import de.probst.ba.core.net.peer.PeerConfig;
import de.probst.ba.core.net.peer.peers.netty.handlers.codec.SimpleCodec;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Created by chrisprobst on 15.09.14.
 */
public final class NettyConfig {

    private static final Logger logger = LoggerFactory.getLogger(NettyConfig.class);

    public static void setupConfig(int smallestBandwidth,
                                   long chunkSize,
                                   int maxConnections,
                                   double metaDataSizePercentage,
                                   boolean binaryCodec) {

        // Calculate a few values
        double refillRateInSeconds = PeerConfig.getLeakyBucketRefillInterval() / 1000.0;
        int calculatedBufferSize = (int) Math.round(smallestBandwidth * refillRateInSeconds * 0.5);
        int bufferSize = (int) Math.min(Math.min(chunkSize, calculatedBufferSize), NettyConfig.getUploadBufferSize());

        // Set netty config
        NettyConfig.setUploadBufferSize(bufferSize);
        NettyConfig.setDefaultMessageSize((long) (chunkSize * metaDataSizePercentage / 100));
        NettyConfig.setUseCodec(binaryCodec);
        NettyConfig.setMaxConnectionsPerLeecher(maxConnections);

        // Log netty values
        logger.info(">>> [ Netty Config ]");
        logger.info(">>> Meta data size:            " + getDefaultMessageSize() + " bytes");
        logger.info(">>> Upload buffer size:        " + NettyConfig.getUploadBufferSize() + " bytes (" +
                    "Nearest power of 2 of " + bufferSize + ")");
        logger.info(">>> Using codec:               " + NettyConfig.isUseCodec());
        logger.info(">>> Leecher connection limit:  " + NettyConfig.getMaxConnectionsPerLeecher());
    }

    private NettyConfig() {
    }

    private static volatile long defaultMessageSize;
    private static volatile int maxConnectionsPerLeecher = 0;
    private static volatile int uploadBufferSize = 8192;
    private static final long messageQueueResetDelay = 2500;
    private static final long discoveryExchangeDelay = 1000;
    private static final long announceDelay = 200;
    private static volatile boolean useCodec;

    public static long getDefaultMessageSize() {
        return defaultMessageSize;
    }

    public static void setDefaultMessageSize(long defaultMessageSize) {
        NettyConfig.defaultMessageSize = defaultMessageSize;
    }

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
        NettyConfig.uploadBufferSize = Integer.highestOneBit(uploadBufferSize - 1) << 1;
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
