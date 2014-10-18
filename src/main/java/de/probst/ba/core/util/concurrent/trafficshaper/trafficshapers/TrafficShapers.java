package de.probst.ba.core.util.concurrent.trafficshaper.trafficshapers;

import de.probst.ba.core.util.concurrent.LeakyBucket;
import de.probst.ba.core.util.concurrent.trafficshaper.TrafficShaper;
import de.probst.ba.core.util.concurrent.trafficshaper.trafficshapers.leastfirst.LeastFirstTrafficShaper;

import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * Created by chrisprobst on 14.09.14.
 */
public final class TrafficShapers {

    private TrafficShapers() {

    }

    public static <T> TrafficShaper<T> leastFirst(Optional<LeakyBucket> leakyBucket,
                                                  Function<Runnable, Future<?>> scheduleFunction,
                                                  long resetTrafficInterval) {
        return new LeastFirstTrafficShaper<>(leakyBucket, scheduleFunction, resetTrafficInterval);
    }
}
