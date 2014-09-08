package de.probst.ba.core.net.peer.peers.netty.handlers.traffic;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * Created by chrisprobst on 16.08.14.
 */
@ChannelHandler.Sharable
public final class WriteThrottle extends ChannelHandlerAdapter implements Runnable {

    private final Logger logger =
            LoggerFactory.getLogger(WriteThrottle.class);

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
            ctx.channel().eventLoop().execute(WriteThrottle.this);
        }
    }

    private final Queue<ThrottledWrite> writes = new LinkedList<>();
    private boolean running = false;
    private final long uploadRate;

    public WriteThrottle(long uploadRate) {
        this.uploadRate = uploadRate;
    }

    @Override
    public void run() {
        synchronized (this) {
            ThrottledWrite throttledWrite = writes.poll();
            if (throttledWrite == null) {
                running = false;
                return;
            }

            if (throttledWrite.timeToWait > 10) {
                throttledWrite.ctx.channel()
                                  .eventLoop()
                                  .schedule(throttledWrite::write, throttledWrite.timeToWait, TimeUnit.MILLISECONDS);
            } else {
                throttledWrite.write();
            }
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ByteBuf) {

            long amount = ((ByteBuf) msg).readableBytes();
            long timeToWait = (long) ((amount / (double) uploadRate) * 1000);
            synchronized (this) {
                writes.offer(new ThrottledWrite(timeToWait, msg, promise, ctx));
                if (!running) {
                    running = true;
                    ctx.channel().eventLoop().execute(this);
                }
            }
        } else {
            super.write(ctx, msg, promise);
        }
    }
}