package de.probst.ba.core.logic;

import java.util.concurrent.TimeUnit;

/**
 * Created by chrisprobst on 12.08.14.
 */
public final class Config {

    private Config() {
    }

    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS;

    public static long getRemoteDataInfoExpirationDelay() {
        return getDataInfoAnnounceDelay() * 2;
    }

    public static TimeUnit getRemoteDataInfoExpirationDelayTimeUnit() {
        return DEFAULT_TIME_UNIT;
    }

    public static long getDataInfoAnnounceDelay() {
        return 2000;
    }

    public static TimeUnit getDataInfoAnnounceTimeUnit() {
        return DEFAULT_TIME_UNIT;
    }
}
