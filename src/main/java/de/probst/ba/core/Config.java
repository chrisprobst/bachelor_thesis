package de.probst.ba.core;

import java.util.concurrent.TimeUnit;

/**
 * Created by chrisprobst on 12.08.14.
 */
public final class Config {

    private Config() {
    }

    public static int getDefaultCVSElementWidth() {
        return 20;
    }

    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS;

    public static long getDataInfoAnnounceDelay() {
        return 500;
    }

    public static TimeUnit getDataInfoAnnounceTimeUnit() {
        return DEFAULT_TIME_UNIT;
    }

    public static long getBrainDelay() {
        return 50;
    }

    public static TimeUnit getBrainTimeUnit() {
        return DEFAULT_TIME_UNIT;
    }
}
