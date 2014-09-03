package de.probst.ba.core;

import java.util.concurrent.TimeUnit;

/**
 * Created by chrisprobst on 12.08.14.
 */
public final class Config {

    private Config() {
    }

    private static volatile long announceDelay = 75;

    public static int getDefaultCVSElementWidth() {
        return 30;
    }

    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS;

    public static TimeUnit getDefaultTimeUnit() {
        return DEFAULT_TIME_UNIT;
    }

    public static long getAnnounceDelay() {
        return announceDelay;
    }

    public static void setAnnounceDelay(long announceDelay) {
        Config.announceDelay = announceDelay;
    }
}
