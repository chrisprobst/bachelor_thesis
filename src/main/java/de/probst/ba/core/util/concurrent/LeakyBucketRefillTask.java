package de.probst.ba.core.util.concurrent;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by chrisprobst on 12.09.14.
 */
public final class LeakyBucketRefillTask implements Runnable, AutoCloseable {

    private final Future<?> scheduleFuture;
    private final Set<LeakyBucket> leakyBuckets = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private volatile Instant timeStamp = Instant.now();

    public LeakyBucketRefillTask(ScheduledExecutorService scheduledExecutorService,
                                 long refillInterval) {
        scheduleFuture = scheduledExecutorService.scheduleAtFixedRate(this, 0, refillInterval, TimeUnit.MILLISECONDS);
    }

    public boolean add(LeakyBucket leakyBucket) {
        return leakyBuckets.add(leakyBucket);
    }

    public boolean remove(LeakyBucket leakyBucket) {
        return leakyBuckets.remove(leakyBucket);
    }

    @Override
    public void close() {
        scheduleFuture.cancel(false);
    }

    @Override
    public void run() {
        Instant newTimeStamp = Instant.now();
        double seconds = Duration.between(timeStamp, newTimeStamp).toMillis() / 1000.0;
        timeStamp = newTimeStamp;
        leakyBuckets.forEach(leakyBucket -> leakyBucket.refill(seconds));
    }
}
