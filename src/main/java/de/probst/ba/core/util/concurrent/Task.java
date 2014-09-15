package de.probst.ba.core.util.concurrent;

import java.util.Objects;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by chrisprobst on 14.09.14.
 */
public final class Task implements CancelableRunnable {

    private final Consumer<CancelableRunnable> consumer;
    private final Function<Runnable, Future<?>> scheduleFunction;
    private Future<?> future;
    private boolean scheduled;
    private boolean running;
    private boolean cancelled;

    private void doRun() {
        synchronized (this) {
            if (running) {
                return;
            }

            scheduled = false;
            running = true;
            future = null;
        }

        try {
            consumer.accept(this);
        } finally {
            synchronized (this) {
                if (cancelled) {
                    return;
                } else {
                    running = false;
                    if (scheduled) {
                        future = scheduleFunction.apply(this::doRun);
                    }
                }
            }
        }
    }

    public Task(Consumer<CancelableRunnable> consumer, Function<Runnable, Future<?>> scheduleFunction) {
        Objects.requireNonNull(consumer);
        Objects.requireNonNull(scheduleFunction);
        this.consumer = consumer;
        this.scheduleFunction = scheduleFunction;
    }

    @Override
    public synchronized void cancel() {
        cancelled = true;
        if (future != null) {
            future.cancel(false);
            future = null;
        }
    }

    @Override
    public synchronized void run() {
        if (cancelled) {
            return;
        } else if (running) {
            scheduled = true;
        } else if (scheduled) {
            return;
        } else {
            scheduled = true;
            future = scheduleFunction.apply(this::doRun);
        }
    }
}
