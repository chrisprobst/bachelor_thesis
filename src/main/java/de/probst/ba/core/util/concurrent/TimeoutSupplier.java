package de.probst.ba.core.util.concurrent;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Created by chrisprobst on 16.09.14.
 */
public final class TimeoutSupplier<T> implements Supplier<T> {

    private final Supplier<T> supplier;
    private final Supplier<T> timeoutSupplier;
    private final long timeoutDelay;
    private long timeStamp = -1;

    public TimeoutSupplier(Supplier<T> supplier, Supplier<T> timeoutSupplier, long timeoutDelay) {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(timeoutSupplier);
        this.supplier = supplier;
        this.timeoutSupplier = timeoutSupplier;
        this.timeoutDelay = timeoutDelay;
    }

    @Override
    public synchronized T get() {
        long now = System.currentTimeMillis();
        if (timeStamp == -1 || now - timeStamp > timeoutDelay) {
            timeStamp = now;
            return timeoutSupplier.get();
        } else {
            return supplier.get();
        }
    }
}
