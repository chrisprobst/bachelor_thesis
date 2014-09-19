package de.probst.ba.cli;

import de.probst.ba.core.net.peer.PeerConfig;

/**
 * Created by chrisprobst on 19.09.14.
 */
public final class AppConfig {

    private AppConfig() {

    }

    private static final long statisticInterval = PeerConfig.getMinimalBandwidthStatisticStateCreationDelay() * 2;

    public static long getStatisticInterval() {
        return statisticInterval;
    }
}
