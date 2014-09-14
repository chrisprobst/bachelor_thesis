package de.probst.ba.core.net.peer.peers.netty.handlers.traffic;

import de.probst.ba.core.util.concurrent.AbstractTask;
import de.probst.ba.core.util.concurrent.LeakyBucket;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 13.09.14.
 */
public final class LeastFirstTrafficShaper extends AbstractTask {

    private final LeakyBucket leakyBucket;
    private final Supplier<Collection<MessageQueueHandler>> messageQueueHandlers;
    private final Comparator<MessageQueueHandler> messageQueueHandlerComparator;
    private final Function<MessageQueueHandler, AbstractMessageEvent> peekMessageFunction;
    private final Function<MessageQueueHandler, AbstractMessageEvent> removeMessageFunction;
    private final Optional<Consumer<MessageQueueHandler>> startConsumer;
    private final Optional<Consumer<MessageQueueHandler>> stopConsumer;

    @Override
    protected void process() {
        // Get all message queue handlers
        List<MessageQueueHandler> messageQueueHandlerList =
                messageQueueHandlers.get().stream().filter(handler -> handler != null).collect(Collectors.toList());

        // Initialize all handlers
        startConsumer.ifPresent(messageQueueHandlerList::forEach);

        // Pack into a priority queue
        PriorityQueue<MessageQueueHandler> messageQueueHandlerPriorityQueue =
                new PriorityQueue<>(messageQueueHandlerComparator);
        messageQueueHandlerPriorityQueue.addAll(messageQueueHandlerList);

        // Walk until empty
        while (!messageQueueHandlerPriorityQueue.isEmpty()) {
            MessageQueueHandler messageQueueHandler = messageQueueHandlerPriorityQueue.remove();

            // Check if there is a message
            AbstractMessageEvent abstractMessageEvent = peekMessageFunction.apply(messageQueueHandler);
            if (abstractMessageEvent == null) {
                continue;
            }

            // Check tokens
            if (leakyBucket.take(abstractMessageEvent.getMessageSize(), this::run)) {
                removeMessageFunction.apply(messageQueueHandler).dispatch();
            } else {
                // Make sure all handlers are stopped
                stopConsumer.ifPresent(messageQueueHandlerList::forEach);
                return;
            }

            // Put back into queue
            messageQueueHandlerPriorityQueue.add(messageQueueHandler);
        }
    }

    public LeastFirstTrafficShaper(Executor executor,
                                   LeakyBucket leakyBucket,
                                   Supplier<Collection<MessageQueueHandler>> messageQueueHandlers,
                                   Comparator<MessageQueueHandler> messageQueueHandlerComparator,
                                   Function<MessageQueueHandler, AbstractMessageEvent> peekMessageFunction,
                                   Function<MessageQueueHandler, AbstractMessageEvent> removeMessageFunction,
                                   Optional<Consumer<MessageQueueHandler>> startConsumer,
                                   Optional<Consumer<MessageQueueHandler>> stopConsumer) {
        super(executor);
        Objects.requireNonNull(leakyBucket);
        Objects.requireNonNull(messageQueueHandlers);
        Objects.requireNonNull(messageQueueHandlerComparator);
        Objects.requireNonNull(peekMessageFunction);
        Objects.requireNonNull(removeMessageFunction);
        Objects.requireNonNull(startConsumer);
        Objects.requireNonNull(stopConsumer);
        this.leakyBucket = leakyBucket;
        this.messageQueueHandlers = messageQueueHandlers;
        this.messageQueueHandlerComparator = messageQueueHandlerComparator;
        this.peekMessageFunction = peekMessageFunction;
        this.removeMessageFunction = removeMessageFunction;
        this.startConsumer = startConsumer;
        this.stopConsumer = stopConsumer;
    }
}

