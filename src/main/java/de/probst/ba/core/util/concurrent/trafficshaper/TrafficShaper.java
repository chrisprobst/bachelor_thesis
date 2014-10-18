package de.probst.ba.core.util.concurrent.trafficshaper;

import de.probst.ba.core.util.concurrent.CancelableRunnable;

import java.util.Optional;

/**
 * Created by chrisprobst on 17.10.14.
 */
public interface TrafficShaper<T> extends CancelableRunnable {

    long getTotalTrafficRate();

    long getCurrentTrafficRate();

    long getTotalTraffic();

    MessageSink<T> createMessageSink(Optional<Runnable> pause, Optional<Runnable> resume);
}
