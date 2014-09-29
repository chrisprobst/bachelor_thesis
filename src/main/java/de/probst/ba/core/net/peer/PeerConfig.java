package de.probst.ba.core.net.peer;

/**
 * Created by chrisprobst on 15.09.14.
 */
public final class PeerConfig {

    private PeerConfig() {
    }

    private static final long leakyBucketRefillInterval = 250;
    private static final double leakyBucketMaxTokensFactor = leakyBucketRefillInterval / 1000.0;
    private static final long minimalBandwidthStatisticStateCreationDelay = leakyBucketRefillInterval;

    public static long getLeakyBucketRefillInterval() {
        return leakyBucketRefillInterval;
    }

    public static double getLeakyBucketMaxTokensFactor() {
        return leakyBucketMaxTokensFactor;
    }

    public static long getMinimalBandwidthStatisticStateCreationDelay() {
        return minimalBandwidthStatisticStateCreationDelay;
    }
}
