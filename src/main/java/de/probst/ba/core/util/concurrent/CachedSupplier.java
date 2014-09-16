package de.probst.ba.core.util.concurrent;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Created by chrisprobst on 16.09.14.
 */
public final class CachedSupplier<T> implements Supplier<T> {

    private final Supplier<T> supplier;
    private final long refreshDelay;
    private T t;
    private long timeStamp = -1;

    public CachedSupplier(Supplier<T> supplier, long refreshDelay) {
        Objects.requireNonNull(supplier);
        this.supplier = supplier;
        this.refreshDelay = refreshDelay;
    }

    @Override
    public synchronized T get() {
        long now = System.currentTimeMillis();
        if ((timeStamp == -1 || t == null) || now - timeStamp > refreshDelay) {
            timeStamp = now;
            return t = supplier.get();
        } else {
            return t;
        }
    }
}
