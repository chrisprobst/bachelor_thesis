package de.probst.ba.core.statistic;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by chrisprobst on 04.09.14.
 */
public abstract class ScheduledStatistic extends FileStatistic implements Runnable {

    private final ScheduledExecutorService scheduledExecutorService;
    private final long delay;

    private Future<?> schedulation;

    public ScheduledStatistic(Path csvPath,
                              ScheduledExecutorService scheduledExecutorService,
                              long delay) {
        super(csvPath);
        Objects.requireNonNull(scheduledExecutorService);
        this.scheduledExecutorService = scheduledExecutorService;
        this.delay = delay;
    }

    public synchronized void schedule() {
        if (schedulation != null) {
            throw new IllegalStateException("schedulation != null");
        }
        schedulation = scheduledExecutorService.schedule(
                this, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public synchronized void run() {
        if (schedulation == null) {
            return;
        }
        schedulation = null;
        doWriteStatistic();
        schedule();
    }

    public synchronized void cancel() {
        if (schedulation != null) {
            schedulation.cancel(false);
            schedulation = null;
        }
    }

    @Override
    public synchronized void close() throws IOException {
        cancel();
        super.close();
    }
}
