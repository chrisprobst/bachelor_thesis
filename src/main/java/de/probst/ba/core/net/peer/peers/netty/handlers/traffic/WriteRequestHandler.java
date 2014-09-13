package de.probst.ba.core.net.peer.peers.netty.handlers.traffic;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.group.ChannelGroup;

import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 16.08.14.
 */
public final class WriteRequestHandler extends ChannelHandlerAdapter implements Comparable<WriteRequestHandler> {

    public static List<WriteRequestHandler> collect(ChannelGroup channelGroup) {
        return channelGroup.stream().map(WriteRequestHandler::get).collect(Collectors.toList());
    }

    public static WriteRequestHandler get(Channel remotePeer) {
        return remotePeer.pipeline().get(WriteRequestHandler.class);
    }

    private final RoundRobinTrafficShaper roundRobinTrafficShaper;
    private final AtomicLong totalRemoved = new AtomicLong();
    private final Queue<WriteRequest> writeRequestQueue = new ConcurrentLinkedQueue<>();

    public WriteRequestHandler(RoundRobinTrafficShaper roundRobinTrafficShaper) {
        Objects.requireNonNull(roundRobinTrafficShaper);
        this.roundRobinTrafficShaper = roundRobinTrafficShaper;
    }

    public WriteRequest peek() {
        return writeRequestQueue.peek();
    }

    public WriteRequest remove() {
        WriteRequest writeRequest = writeRequestQueue.remove();
        totalRemoved.getAndAdd(writeRequest.getMessageSize());
        return writeRequest;
    }

    public long getTotalRemoved() {
        return totalRemoved.get();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        writeRequestQueue.add(new WriteRequest(ctx, msg, promise));
        roundRobinTrafficShaper.execute();
    }

    @Override
    public int compareTo(WriteRequestHandler o) {
        return Long.compare(getTotalRemoved(), o.getTotalRemoved());
    }
}