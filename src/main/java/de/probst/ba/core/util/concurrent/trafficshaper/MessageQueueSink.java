package de.probst.ba.core.util.concurrent.trafficshaper;

import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Created by chrisprobst on 17.10.14.
 */
public final class MessageQueueSink<T> implements MessageSink<T> {

    private final TrafficShaper<T> trafficShaper;
    private final Consumer<MessageQueueSink<? extends T>> closeCallback;
    private final Optional<Runnable> pauseCallback;
    private final Optional<Runnable> resumeCallback;
    private final Queue<Message<? extends T>> messageQueue = new ConcurrentLinkedQueue<>();
    private final AtomicLong traffic = new AtomicLong();

    public MessageQueueSink(TrafficShaper<T> trafficShaper,
                            Consumer<MessageQueueSink<? extends T>> closeCallback,
                            Optional<Runnable> pauseCallback,
                            Optional<Runnable> resumeCallback) {
        Objects.requireNonNull(trafficShaper);
        Objects.requireNonNull(closeCallback);
        Objects.requireNonNull(pauseCallback);
        Objects.requireNonNull(resumeCallback);
        this.trafficShaper = trafficShaper;
        this.closeCallback = closeCallback;
        this.pauseCallback = pauseCallback;
        this.resumeCallback = resumeCallback;
    }

    public void pause() {
        pauseCallback.ifPresent(Runnable::run);
    }

    public void resume() {
        resumeCallback.ifPresent(Runnable::run);
    }

    public Message<? extends T> peekMessage() {
        return messageQueue.peek();
    }

    public Message<? extends T> removeMessage() {
        return messageQueue.remove();
    }

    public void resetTraffic() {
        traffic.set(0);
    }

    public void increaseTraffic(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount < 0");
        }
        traffic.getAndAdd(amount);
    }

    public long getTraffic() {
        return traffic.get();
    }

    @Override
    public TrafficShaper<T> getTrafficShaper() {
        return trafficShaper;
    }

    @Override
    public MessageSink<T> sinkMessage(Consumer<? super T> dispatcher, T message) {
        messageQueue.offer(new Message<>(dispatcher, message));
        return this;
    }

    @Override
    public void close() {
        closeCallback.accept(this);
    }

    @Override
    public int compareTo(MessageSink<?> o) {
        if (!(o instanceof MessageQueueSink)) {
            throw new IllegalArgumentException("!(o instanceof MessageQueueSink)");
        } else {
            return Long.compare(getTraffic(), ((MessageQueueSink) o).getTraffic());
        }
    }
}
