package de.probst.ba.core.util.concurrent;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

/**
 * Created by chrisprobst on 12.09.14.
 */
public class LeakyBucket implements AutoCloseable {

    private final LeakyBucketRefillTask leakyBucketRefillTask;
    private final long maxTokens;
    private final long refillRate;
    private Queue<Runnable> runnables = new LinkedList<>();
    private long tokens;

    public LeakyBucket(LeakyBucketRefillTask leakyBucketRefillTask, long maxTokens, long refillRate) {
        Objects.requireNonNull(leakyBucketRefillTask);
        this.leakyBucketRefillTask = leakyBucketRefillTask;
        this.maxTokens = maxTokens;
        this.refillRate = refillRate;
        this.leakyBucketRefillTask.add(this);
        tokens = maxTokens;
    }

    public synchronized boolean isEmpty() {
        return tokens <= 0;
    }

    public synchronized boolean take(long tokens, Runnable runnable) {
        if (this.tokens >= tokens) {
            this.tokens -= tokens;
            return true;
        } else {
            runnables.add(runnable);
            return false;
        }
    }

    public void refill(double seconds) {
        Queue<Runnable> oldRunnables;
        synchronized (this) {
            tokens = Math.min(maxTokens, Math.round(tokens + refillRate * seconds));
            oldRunnables = runnables;
            runnables = new LinkedList<>();
        }
        oldRunnables.forEach(Runnable::run);
    }

    @Override
    public void close() {
        this.leakyBucketRefillTask.remove(this);
    }
}
