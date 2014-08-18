package de.probst.ba.core.util.concurrent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by chrisprobst on 18.08.14.
 */
public final class AtomicCounter {

    private final AtomicInteger atomicInteger =
            new AtomicInteger();

    public boolean tryIncrement(int maximum) {
        if (maximum < 1) {
            throw new IllegalArgumentException("maximum < 1");
        }

        for (int expected; (expected = atomicInteger.get()) < maximum; ) {
            if (atomicInteger.compareAndSet(expected, expected + 1)) {
                return true;
            }
        }
        return false;
    }

    public int get() {
        return atomicInteger.get();
    }

    public boolean tryDecrement() {
        for (int expected; (expected = atomicInteger.get()) > 0; ) {
            if (atomicInteger.compareAndSet(expected, expected - 1)) {
                return true;
            }
        }
        return false;
    }
}
