package de.probst.ba.core.util.concurrent.trafficshaper.trafficshapers.leastfirst;

import de.probst.ba.core.util.concurrent.LeakyBucket;
import de.probst.ba.core.util.concurrent.trafficshaper.AbstractTrafficShaper;
import de.probst.ba.core.util.concurrent.trafficshaper.Message;
import de.probst.ba.core.util.concurrent.trafficshaper.MessageQueueSink;
import de.probst.ba.core.util.concurrent.trafficshaper.MessageSink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * Created by chrisprobst on 13.09.14.
 */
public final class LeastFirstTrafficShaper<T> extends AbstractTrafficShaper<T> {

    private final Queue<MessageQueueSink<? extends T>> messageQueueSinks = new ConcurrentLinkedQueue<>();
    private final Optional<LeakyBucket> leakyBucket;
    private final long resetTrafficInterval;
    private long lastResetTrafficTimeStamp = System.currentTimeMillis();

    @Override
    protected void shapeTraffic() {
        List<MessageQueueSink<? extends T>> messageQueueSinkList = new ArrayList<>(messageQueueSinks);

        // Reset traffic if necessary
        long now = System.currentTimeMillis();
        if (now - lastResetTrafficTimeStamp > resetTrafficInterval) {
            // Reset traffic
            messageQueueSinkList.forEach(MessageQueueSink::resetTraffic);
            messageQueueSinkList.forEach(MessageQueueSink::resetMetaTraffic);

            // Shuffle message queue sinks
            Collections.shuffle(messageQueueSinkList);

            lastResetTrafficTimeStamp = now;
        }

        // Resume all
        messageQueueSinkList.forEach(MessageQueueSink::resume);

        // Pack into a priority queue
        Queue<MessageQueueSink<? extends T>> priorityQueue = new PriorityQueue<>(messageQueueSinkList);

        // Walk until empty
        while (!priorityQueue.isEmpty()) {

            // Remove next
            MessageQueueSink<? extends T> messageQueueSink = priorityQueue.remove();

            // Check if there is a message
            Message<? extends T> message = messageQueueSink.peekMessage();
            if (message == null) {
                continue;
            }

            // Get remaining size and try to take the tokens from the bucket
            long remainingSize = message.getRemainingSize();
            long removedTokens = leakyBucket.map(x -> x.take(remainingSize, this)).orElse(remainingSize);

            // Decrease remaining size of the message
            message.decreaseRemainingSize(removedTokens);

            // Increase the (meta) traffic
            if (message.isMetaData()) {
                messageQueueSink.increaseMetaTraffic(removedTokens);
                increaseMetaTraffic(removedTokens);
            } else {
                messageQueueSink.increaseTraffic(removedTokens);
                increaseTraffic(removedTokens);
            }

            // Dispatch or pause all
            if (removedTokens >= remainingSize) {
                messageQueueSink.removeMessage().dispatch();
            } else {
                priorityQueue.forEach(MessageQueueSink::pause);
                return;
            }

            // Put back into queue
            priorityQueue.add(messageQueueSink);
        }
    }

    public LeastFirstTrafficShaper(Optional<LeakyBucket> leakyBucket,
                                   Function<Runnable, Future<?>> scheduleFunction,
                                   long resetTrafficInterval) {
        super(scheduleFunction);
        Objects.requireNonNull(leakyBucket);
        this.resetTrafficInterval = resetTrafficInterval;
        this.leakyBucket = leakyBucket;
    }

    @Override
    public MessageSink<T> createMessageSink(Optional<Runnable> pause, Optional<Runnable> resume) {
        MessageQueueSink<T> messageQueueSink = new MessageQueueSink<>(this, messageQueueSinks::remove, pause, resume);
        messageQueueSinks.add(messageQueueSink);
        return messageQueueSink;
    }
}

