package de.probst.ba.core.util.concurrent.trafficshaper;

import java.util.function.Consumer;

/**
 * Created by chrisprobst on 17.10.14.
 */
public interface MessageSink<T> extends Comparable<MessageSink<?>>, AutoCloseable {

    TrafficShaper<T> getTrafficShaper();

    MessageSink<T> sinkMessage(Consumer<? super T> dispatcher, T message, boolean metaData);

    @Override
    void close();
}
