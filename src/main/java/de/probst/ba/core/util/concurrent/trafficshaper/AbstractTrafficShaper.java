package de.probst.ba.core.util.concurrent.trafficshaper;

import de.probst.ba.core.util.concurrent.Task;

import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * Created by chrisprobst on 17.10.14.
 */
public abstract class AbstractTrafficShaper<T> implements TrafficShaper<T> {

    private final Task task;
    private final long startTotalTimeStamp = System.currentTimeMillis();
    private long totalTraffic;
    private long startCurrentTrafficTimeStamp = System.currentTimeMillis();
    private long currentTraffic;

    protected abstract void shapeTraffic();

    protected synchronized final void increaseTraffic(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount < 0");
        }
        totalTraffic += amount;
        currentTraffic += amount;
    }

    public AbstractTrafficShaper(Function<Runnable, Future<?>> scheduleFunction) {
        task = new Task(x -> shapeTraffic(), scheduleFunction);
    }

    @Override
    public synchronized final long getTotalTrafficRate() {
        double seconds = (System.currentTimeMillis() - startTotalTimeStamp) / 1000.0;
        return seconds > 0 ? (long) (totalTraffic / seconds) : 0;
    }

    @Override
    public synchronized final long getCurrentTrafficRate() {
        long now = System.currentTimeMillis();
        double seconds = (now - startCurrentTrafficTimeStamp) / 1000.0;
        long rate = seconds > 0 ? (long) (currentTraffic / seconds) : 0;
        startCurrentTrafficTimeStamp = now;
        currentTraffic = 0;
        return rate;
    }

    @Override
    public synchronized final long getTotalTraffic() {
        return totalTraffic;
    }

    @Override
    public final void run() {
        task.run();
    }

    @Override
    public final void cancel() {
        task.cancel();
    }
}
