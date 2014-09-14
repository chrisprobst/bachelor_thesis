package de.probst.ba.core.net.peer.peers.netty.handlers.traffic;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.group.ChannelGroup;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 16.08.14.
 */
public final class MessageQueueHandler extends ChannelHandlerAdapter {

    public static final Comparator<MessageQueueHandler> TOTAL_READ_COMPARATOR =
            Comparator.comparing(MessageQueueHandler::getTotalRead);
    public static final Comparator<MessageQueueHandler> TOTAL_WRITTEN_COMPARATOR =
            Comparator.comparing(MessageQueueHandler::getTotalWritten);

    public static List<MessageQueueHandler> collect(ChannelGroup channelGroup) {
        return channelGroup.stream().map(MessageQueueHandler::get).collect(Collectors.toList());
    }

    public static MessageQueueHandler get(Channel remotePeer) {
        return remotePeer.pipeline().get(MessageQueueHandler.class);
    }

    private volatile ChannelHandlerContext ctx;
    private final Optional<Runnable> writeTask;
    private final Optional<Runnable> readTask;
    private final AtomicLong totalWritten = new AtomicLong();
    private final AtomicLong totalRead = new AtomicLong();
    private final Queue<WriteEvent> writeEventQueue = new ConcurrentLinkedQueue<>();
    private final Queue<ReadEvent> readEventQueue = new ConcurrentLinkedQueue<>();

    public MessageQueueHandler(Optional<Runnable> writeTask, Optional<Runnable> readTask) {
        Objects.requireNonNull(writeTask);
        Objects.requireNonNull(readTask);
        this.writeTask = writeTask;
        this.readTask = readTask;
    }

    public WriteEvent peekWriteEvent() {
        return writeEventQueue.peek();
    }

    public ReadEvent peekReadEvent() {
        return readEventQueue.peek();
    }

    public WriteEvent removeWriteEvent() {
        WriteEvent writeEvent = writeEventQueue.remove();
        totalWritten.getAndAdd(writeEvent.getMessageSize());
        return writeEvent;
    }

    public ReadEvent removeReadEvent() {
        ReadEvent readEvent = readEventQueue.remove();
        totalRead.getAndAdd(readEvent.getMessageSize());
        return readEvent;
    }

    public long getTotalWritten() {
        return totalWritten.get();
    }

    public long getTotalRead() {
        return totalRead.get();
    }

    public ChannelHandlerContext getChannelHandlerContext() {
        return ctx;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (writeTask.isPresent()) {
            writeEventQueue.add(new WriteEvent(ctx, msg, promise));
            writeTask.get().run();
        } else {
            super.write(ctx, msg, promise);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (readTask.isPresent()) {
            readEventQueue.add(new ReadEvent(ctx, msg));
            readTask.get().run();
        } else {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        super.channelActive(ctx);
    }
}