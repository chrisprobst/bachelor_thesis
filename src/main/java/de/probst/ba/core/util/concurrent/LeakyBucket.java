package de.probst.ba.core.util.concurrent;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by chrisprobst on 12.09.14.
 */
public final class LeakyBucket {

    private final long maxTokens;
    private final long refillRate;
    private Queue<Runnable> runnables = new LinkedList<>();
    private long tokens;

    public LeakyBucket(long maxTokens, long refillRate) {
        if (maxTokens < 1) {
            throw new IllegalArgumentException("maxTokens < 1");
        }

        if (refillRate < 0) {
            throw new IllegalArgumentException("refillRate < 0");
        }

        this.maxTokens = maxTokens;
        this.refillRate = refillRate;
        tokens = maxTokens;
    }

    public synchronized boolean isEmpty() {
        return tokens <= 0;
    }

    public synchronized long take(long tokens, Runnable runnable) {
        if (tokens < 0) {
            throw new IllegalArgumentException("tokens < 0");
        }

        if (tokens == 0) {
            return 0;
        }

        if (this.tokens < tokens) {
            long removedTokens = this.tokens;
            this.tokens = 0;
            runnables.add(runnable);
            return removedTokens;
        } else {
            this.tokens -= tokens;
            return tokens;
        }
    }

    public void refill(double seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("seconds < 0");
        }

        Queue<Runnable> oldRunnables;
        synchronized (this) {
            tokens = Math.min(maxTokens, Math.round(tokens + refillRate * seconds));
            oldRunnables = runnables;
            runnables = new LinkedList<>();
        }
        oldRunnables.forEach(Runnable::run);
    }
}
