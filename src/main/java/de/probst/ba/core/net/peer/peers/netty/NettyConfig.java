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
                                   int maxConnections) {
        setupConfig(smallestBandwidth, getUploadBufferSize(), maxConnections, 0, true);
    }

    public static void setupConfig(int smallestBandwidth,
                                   long chunkSize) {
        setupConfig(smallestBandwidth, chunkSize, 0, 0, true);
    }

    public static void setupConfig(int smallestBandwidth,
                                   long chunkSize,
                                   int maxConnections,
                                   double metaDataSizePercentage,
                                   boolean binaryCodec) {

        // Calculate a few values
        double refillRateInSeconds = PeerConfig.getLeakyBucketRefillInterval() / 1000.0;
        int calculatedBufferSize =
                (int) Math.round(smallestBandwidth * refillRateInSeconds * getLeakyBucketBufferFactor());
        int bufferSize = (int) Math.min(Math.min(chunkSize, calculatedBufferSize), getUploadBufferSize());

        // Set netty config
        setUploadBufferSize(bufferSize);
        setDefaultEstimatedMessageSize((long) Math.ceil(chunkSize * metaDataSizePercentage / 100));
        setUseCodec(binaryCodec);
        setMaxConnectionsPerLeecher(maxConnections);

        // Log netty values
        logger.info(">>> [ Netty Config ]");
        logger.info(">>> Meta data size:            " + getDefaultEstimatedMessageSize() + " bytes");
        logger.info(">>> Upload buffer size:        " + getUploadBufferSize() + " bytes");
        logger.info(">>> Using codec:               " + isUseCodec());
        logger.info(">>> Leecher connection limit:  " + getMaxConnectionsPerLeecher());
    }

    private NettyConfig() {
    }


    private static final int httpBufferSize = 8192;
    private static final long httpRetryDelay = 1000;
    private static final double leakyBucketBufferFactor = 0.25;
    private static volatile long defaultEstimatedMessageSize;
    private static volatile int maxConnectionsPerLeecher;
    private static volatile int uploadBufferSize = 8192;
    private static volatile boolean useCodec;
    private static final long messageQueueResetDelay = 2500;
    private static final long discoveryExchangeDelay = 1000;
    private static final long announceDelay = 200;

    public static int getHttpBufferSize() {
        return httpBufferSize;
    }

    public static long getHttpRetryDelay() {
        return httpRetryDelay;
    }

    public static double getLeakyBucketBufferFactor() {
        return leakyBucketBufferFactor;
    }

    public static long getDefaultEstimatedMessageSize() {
        return defaultEstimatedMessageSize;
    }

    public static void setDefaultEstimatedMessageSize(long defaultEstimatedMessageSize) {
        NettyConfig.defaultEstimatedMessageSize = defaultEstimatedMessageSize;
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
        NettyConfig.uploadBufferSize = uploadBufferSize;
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
