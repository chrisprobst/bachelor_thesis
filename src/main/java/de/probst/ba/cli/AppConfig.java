package de.probst.ba.cli;

/**
 * Created by chrisprobst on 19.09.14.
 */
public final class AppConfig {

    private AppConfig() {

    }

    private static final long statisticInterval = 1000;

    public static long getStatisticInterval() {
        return statisticInterval;
    }
}
