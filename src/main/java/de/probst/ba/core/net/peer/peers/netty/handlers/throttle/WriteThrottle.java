package de.probst.ba.core.net.peer.peers.netty.handlers.throttle;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * Created by chrisprobst on 16.08.14.
 */
public class WriteThrottle extends ChannelHandlerAdapter implements Runnable {

    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(WriteThrottle.class);

    private class ThrottledWrite {
        public long timeToWait;
        public Object msg;
        public ChannelPromise promise;
        public ChannelHandlerContext ctx;

        private ThrottledWrite(long timeToWait, Object msg, ChannelPromise promise, ChannelHandlerContext ctx) {
            this.timeToWait = timeToWait;
            this.msg = msg;
            this.promise = promise;
            this.ctx = ctx;
        }

        public ThrottledWrite schedule() {
            if (scheduled) {
                return this;
            }
            scheduled = true;
            ctx.channel().eventLoop().schedule(
                    WriteThrottle.this, writes.isEmpty() ? timeToWait : writes.peek().timeToWait, TimeUnit.MILLISECONDS);

            return this;
        }

        public void write() {
            ctx.writeAndFlush(msg, promise);
        }
    }

    private Queue<ThrottledWrite> writes = new LinkedList<>();
    private boolean scheduled = false;

    private final long uploadRate;

    public WriteThrottle(long uploadRate) {
        this.uploadRate = uploadRate;
    }

    @Override
    public void run() {
        scheduled = false;

        writes.poll().write();
        if (!writes.isEmpty()) {
            writes.peek().schedule();
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ByteBuf) {
            long amount = ((ByteBuf) msg).readableBytes();
            long timeToWait = (long) ((amount / (double) uploadRate) * 1000);
            writes.offer(new ThrottledWrite(timeToWait, msg, promise, ctx).schedule());
        } else {
            super.write(ctx, msg, promise);
        }
    }
}