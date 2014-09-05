package de.probst.ba.core;

import java.util.concurrent.TimeUnit;

/**
 * Created by chrisprobst on 12.08.14.
 */
public final class Config {

    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS;
    private static volatile long announceDelay = 100;

    private Config() {
    }

    public static int getDefaultCVSElementWidth() {
        return 30;
    }

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
