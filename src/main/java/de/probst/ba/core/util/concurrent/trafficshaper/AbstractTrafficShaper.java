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

    // Used for total traffic
    private long totalTraffic;

    // Used for current traffic
    private long startCurrentTrafficTimeStamp = System.currentTimeMillis();
    private long currentTraffic;

    // Used for total meta traffic
    private long totalMetaTraffic;

    // Used for current meta traffic
    private long startCurrentMetaTrafficTimeStamp = System.currentTimeMillis();
    private long currentMetaTraffic;

    protected abstract void shapeTraffic();

    protected synchronized final void increaseTraffic(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount < 0");
        }
        totalTraffic += amount;
        currentTraffic += amount;
    }

    protected synchronized final void increaseMetaTraffic(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount < 0");
        }
        currentMetaTraffic += amount;
        totalMetaTraffic += amount;
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
    public synchronized final long getTotalMetaTrafficRate() {
        double seconds = (System.currentTimeMillis() - startTotalTimeStamp) / 1000.0;
        return seconds > 0 ? (long) (totalMetaTraffic / seconds) : 0;
    }

    @Override
    public long getAccumTotalTrafficRate() {
        return getTotalTrafficRate() + getTotalMetaTrafficRate();
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
    public synchronized final long getCurrentMetaTrafficRate() {
        long now = System.currentTimeMillis();
        double seconds = (now - startCurrentMetaTrafficTimeStamp) / 1000.0;
        long rate = seconds > 0 ? (long) (currentMetaTraffic / seconds) : 0;
        startCurrentMetaTrafficTimeStamp = now;
        currentMetaTraffic = 0;
        return rate;
    }

    @Override
    public long getAccumCurrentTrafficRate() {
        return getCurrentTrafficRate() + getCurrentMetaTrafficRate();
    }

    @Override
    public synchronized final long getTotalTraffic() {
        return totalTraffic;
    }

    @Override
    public synchronized final long getTotalMetaTraffic() {
        return totalMetaTraffic;
    }

    @Override
    public long getAccumTotalTraffic() {
        return getTotalTraffic() + getTotalMetaTraffic();
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
