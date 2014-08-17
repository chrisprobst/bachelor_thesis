package de.probst.ba.core;

import java.util.concurrent.TimeUnit;

/**
 * Created by chrisprobst on 12.08.14.
 */
public final class Config {

    private Config() {
    }

    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS;

    public static long getDataInfoAnnounceDelay() {
        return 700;
    }

    public static TimeUnit getDataInfoAnnounceTimeUnit() {
        return DEFAULT_TIME_UNIT;
    }

    public static long getBrainDelay() {
        return 1400;
    }

    public static TimeUnit getBrainTimeUnit() {
        return DEFAULT_TIME_UNIT;
    }
}
