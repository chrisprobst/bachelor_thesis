package de.probst.ba.core.net.peer.peers.netty.handlers.throttle;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by chrisprobst on 16.08.14.
 */
@ChannelHandler.Sharable
public final class WriteThrottle extends ChannelHandlerAdapter implements Runnable {

    private final ExecutorService scheduler = Executors.newSingleThreadExecutor();

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

        public void write() {
            ctx.writeAndFlush(msg, promise);
        }
    }

    private final BlockingQueue<ThrottledWrite> writes = new LinkedBlockingQueue<>();
    private final long uploadRate;

    public WriteThrottle(long uploadRate) {
        this.uploadRate = uploadRate;
        scheduler.execute(this);
    }

    @Override
    public void run() {
        while (true) {
            try {
                ThrottledWrite throttledWrite = writes.take();
                Thread.sleep(throttledWrite.timeToWait);
                throttledWrite.write();
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(-77);
            }
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ByteBuf) {

            long amount = ((ByteBuf) msg).readableBytes();
            long timeToWait = (long) ((amount / (double) uploadRate) * 1000);
            writes.offer(new ThrottledWrite(timeToWait, msg, promise, ctx));
        } else {
            super.write(ctx, msg, promise);
        }
    }
}