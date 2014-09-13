package de.probst.ba.core.net.peer.peers.netty.handlers.traffic;

import de.probst.ba.core.util.concurrent.LeakyBucket;

import java.util.Collection;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 13.09.14.
 */
public final class RoundRobinTrafficShaper {

    private final LeakyBucket leakyBucket;
    private final ExecutorService executorService;
    private final Supplier<Collection<WriteRequestHandler>> writeRequestHandlers;
    private boolean scheduled;
    private boolean running;

    private PriorityQueue<WriteRequestHandler> getWriteRequestHandlers() {
        return new PriorityQueue<>(writeRequestHandlers.get()
                                                       .stream()
                                                       .filter(handler -> handler != null)
                                                       .collect(Collectors.toList()));
    }

    public RoundRobinTrafficShaper(LeakyBucket leakyBucket,
                                   ExecutorService executorService,
                                   Supplier<Collection<WriteRequestHandler>> writeRequestHandlers) {

        Objects.requireNonNull(leakyBucket);
        Objects.requireNonNull(executorService);
        Objects.requireNonNull(writeRequestHandlers);
        this.leakyBucket = leakyBucket;
        this.executorService = executorService;
        this.writeRequestHandlers = writeRequestHandlers;
    }

    public synchronized void execute() {
        if (running) {
            scheduled = true;
        } else if (scheduled) {
            return;
        } else {
            scheduled = true;
            executorService.submit(this::process);
        }
    }

    private void process() {
        synchronized (this) {
            if (running) {
                return;
            }

            scheduled = false;
            running = true;
        }

        try {
            PriorityQueue<WriteRequestHandler> writeRequestHandlers = getWriteRequestHandlers();
            for (; ; ) {
                WriteRequestHandler writeRequestHandler = writeRequestHandlers.poll();
                if (writeRequestHandler == null) {
                    return;
                }

                WriteRequest writeRequest = writeRequestHandler.peek();
                if (writeRequest == null) {
                    continue;
                }

                if (leakyBucket.take(writeRequest.getMessageSize(), this::execute)) {
                    writeRequestHandler.remove().writeAndFlush();
                } else {
                    return;
                }

                writeRequestHandlers.offer(writeRequestHandler);
            }
        } finally {
            synchronized (this) {
                running = false;
                if (scheduled) {
                    executorService.execute(this::process);
                }
            }
        }
    }
}

