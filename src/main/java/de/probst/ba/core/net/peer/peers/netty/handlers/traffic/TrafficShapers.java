package de.probst.ba.core.net.peer.peers.netty.handlers.traffic;

import de.probst.ba.core.util.concurrent.CancelableRunnable;
import de.probst.ba.core.util.concurrent.LeakyBucket;
import de.probst.ba.core.util.concurrent.Task;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by chrisprobst on 14.09.14.
 */
public final class TrafficShapers {

    private TrafficShapers() {

    }

    public static CancelableRunnable leastWrittenFirst(Function<Runnable, Future<?>> scheduleFunction,
                                                       LeakyBucket leakyBucket,
                                                       Supplier<Collection<MessageQueueHandler>> messageQueueHandlers) {
        return new Task(new LeastFirstTrafficShaper(leakyBucket,
                                                    messageQueueHandlers,
                                                    MessageQueueHandler.TOTAL_WRITTEN_COMPARATOR,
                                                    MessageQueueHandler::peekWriteEvent,
                                                    MessageQueueHandler::removeWriteEvent,
                                                    Optional.empty(),
                                                    Optional.empty()), scheduleFunction);
    }

    public static CancelableRunnable leastReadFirst(Function<Runnable, Future<?>> scheduleFunction,
                                                    LeakyBucket leakyBucket,
                                                    Supplier<Collection<MessageQueueHandler>> messageQueueHandlers) {
        return new Task(new LeastFirstTrafficShaper(leakyBucket,
                                                    messageQueueHandlers,
                                                    MessageQueueHandler.TOTAL_READ_COMPARATOR,
                                                    MessageQueueHandler::peekReadEvent,
                                                    MessageQueueHandler::removeReadEvent,
                                                    Optional.of(m -> m.getChannelHandlerContext()
                                                                      .channel()
                                                                      .config()
                                                                      .setAutoRead(true)),
                                                    Optional.of(m -> m.getChannelHandlerContext()
                                                                      .channel()
                                                                      .config()
                                                                      .setAutoRead(false))), scheduleFunction);
    }
}
