package de.probst.ba.core.util.concurrent;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by chrisprobst on 12.09.14.
 */
public class LeakyBucket {

    private final long maxTokens;
    private final long refillRate;
    private Queue<Runnable> runnables = new LinkedList<>();
    private long tokens;

    public LeakyBucket(long maxTokens, long refillRate) {
        this.maxTokens = maxTokens;
        this.refillRate = refillRate;
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
}
