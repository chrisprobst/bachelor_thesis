package de.probst.ba.core.util.concurrent.trafficshaper;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Created by chrisprobst on 14.09.14.
 */
public final class Message<T> {

    private final Consumer<? super T> dispatcher;
    private final T message;
    private volatile long remainingSize;

    Message(Consumer<? super T> dispatcher, T message) {
        Objects.requireNonNull(dispatcher);
        Objects.requireNonNull(message);
        this.dispatcher = dispatcher;
        this.message = message;
        remainingSize = MessageSizeEstimator.estimateMessageSize(message);
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
