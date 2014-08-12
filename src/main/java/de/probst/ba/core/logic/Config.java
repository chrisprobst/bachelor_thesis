package de.probst.ba.core.logic;

import java.util.concurrent.TimeUnit;

/**
 * Created by chrisprobst on 12.08.14.
 */
public final class Config {

    private Config() {
    }

    public static long getRemoteDataInfoExpirationDelay() {
        return 5;
    }

    public static TimeUnit getRemoteDataInfoExpirationDelayTimeUnit() {
        return TimeUnit.SECONDS;
    }
}
