package de.probst.ba.core.util.concurrent;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Created by chrisprobst on 14.09.14.
 */
public abstract class AbstractTask implements Runnable {

    private final Executor executor;
    private boolean scheduled;
    private boolean running;

    private void doRun() {
        synchronized (this) {
            if (running) {
                return;
            }

            scheduled = false;
            running = true;
        }

        try {
            process();
        } finally {
            synchronized (this) {
                running = false;
                if (scheduled) {
                    executor.execute(this::doRun);
                }
            }
        }
    }

    protected abstract void process();

    public AbstractTask(Executor executor) {
        Objects.requireNonNull(executor);
        this.executor = executor;
    }

    @Override
    public final synchronized void run() {
        if (running) {
            scheduled = true;
        } else if (scheduled) {
            return;
        } else {
            scheduled = true;
            executor.execute(this::doRun);
        }
    }
}
