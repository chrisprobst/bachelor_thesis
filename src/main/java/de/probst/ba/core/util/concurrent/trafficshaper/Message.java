package de.probst.ba.core.util.concurrent.trafficshaper;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Created by chrisprobst on 14.09.14.
 */
public final class Message<T> {

    private final Consumer<? super T> dispatcher;
    private final T message;
    private final boolean metaData;
    private volatile long remainingSize;

    Message(Consumer<? super T> dispatcher, T message, boolean metaData) {
        Objects.requireNonNull(dispatcher);
        Objects.requireNonNull(message);
        this.dispatcher = dispatcher;
        this.message = message;
        this.metaData = metaData;
        remainingSize = MessageSizeEstimator.estimateMessageSize(message);
    }

    public boolean isMetaData() {
        return metaData;
    }

    public void dispatch() {
        dispatcher.accept(message);
    }

    public long getRemainingSize() {
        return remainingSize;
    }

    public synchronized void decreaseRemainingSize(long amount) {
        remainingSize = Math.max(remainingSize - amount, 0);
    }
}
