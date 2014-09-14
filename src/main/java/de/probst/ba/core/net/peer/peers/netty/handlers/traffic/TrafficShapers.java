package de.probst.ba.core.net.peer.peers.netty.handlers.traffic;

import de.probst.ba.core.util.concurrent.LeakyBucket;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Created by chrisprobst on 14.09.14.
 */
public final class TrafficShapers {

    private TrafficShapers() {

    }

    public static Runnable leastWrittenFirst(Executor executor,
                                             LeakyBucket leakyBucket,
                                             Supplier<Collection<MessageQueueHandler>> messageQueueHandlers) {
        return new LeastFirstTrafficShaper(executor,
                                           leakyBucket,
                                           messageQueueHandlers,
                                           MessageQueueHandler.TOTAL_WRITTEN_COMPARATOR,
                                           MessageQueueHandler::peekWriteEvent,
                                           MessageQueueHandler::removeWriteEvent,
                                           Optional.empty(),
                                           Optional.empty());
    }

    public static Runnable leastReadFirst(Executor executor,
                                          LeakyBucket leakyBucket,
                                          Supplier<Collection<MessageQueueHandler>> messageQueueHandlers) {
        return new LeastFirstTrafficShaper(executor,
                                           leakyBucket,
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
                                                             .setAutoRead(false)));
    }
}
