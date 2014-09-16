package de.probst.ba.core.util.concurrent;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Created by chrisprobst on 12.09.14.
 */
public final class LeakyBucketRefiller implements Consumer<CancelableRunnable> {

    private final Set<LeakyBucket> leakyBuckets = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private volatile long timeStamp = System.currentTimeMillis();

    public boolean add(LeakyBucket leakyBucket) {
        return leakyBuckets.add(leakyBucket);
    }

    public boolean remove(LeakyBucket leakyBucket) {
        return leakyBuckets.remove(leakyBucket);
    }

    @Override
    public void accept(CancelableRunnable cancelableRunnable) {
        long now = System.currentTimeMillis();
        double seconds = (now - timeStamp) / 1000.0;
        timeStamp = now;

        leakyBuckets.forEach(leakyBucket -> leakyBucket.refill(seconds));
        cancelableRunnable.run();
    }
}
