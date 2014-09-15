package de.probst.ba.core.util.concurrent;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Created by chrisprobst on 12.09.14.
 */
public final class LeakyBucketRefiller implements Consumer<CancelableRunnable> {

    private final Set<LeakyBucket> leakyBuckets = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private volatile Instant timeStamp = Instant.now();

    public boolean add(LeakyBucket leakyBucket) {
        return leakyBuckets.add(leakyBucket);
    }

    public boolean remove(LeakyBucket leakyBucket) {
        return leakyBuckets.remove(leakyBucket);
    }

    @Override
    public void accept(CancelableRunnable cancelableRunnable) {
        Instant newTimeStamp = Instant.now();
        double seconds = Duration.between(timeStamp, newTimeStamp).toMillis() / 1000.0;
        timeStamp = newTimeStamp;
        leakyBuckets.forEach(leakyBucket -> leakyBucket.refill(seconds));

        cancelableRunnable.run();
    }
}
