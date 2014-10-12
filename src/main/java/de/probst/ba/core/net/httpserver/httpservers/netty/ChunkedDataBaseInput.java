package de.probst.ba.core.net.httpserver.httpservers.netty;

import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.DataBaseReadChannel;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.peers.netty.NettyConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedInput;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.EOFException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Created by chrisprobst on 11.10.14.
 */
public final class ChunkedDataBaseInput
        implements ChunkedInput<ByteBuf>, GenericFutureListener<ChannelFuture>, Runnable {

    private final DataBase dataBase;
    private final Predicate<DataInfo> predicate;
    private final long offset;
    private final long length;
    private ChunkedWriteHandler chunkedWriteHandler;
    private DataBaseReadChannel readChannel;
    private Future<?> scheduleFuture;
    private long completed;
    private boolean registered;
    private boolean closed;

    private boolean configureReadChannel() throws Exception {
        // Lookup new read channel
        if (readChannel == null) {
            readChannel = dataBase.findIncremental(predicate).get();
        }

        // Validate opened channel
        if (readChannel != null && readChannel.isOpen() && readChannel.size() > offset + completed) {
            readChannel.position(offset + completed);
            return true;
        }

        // Clean up
        if (readChannel != null) {
            readChannel.close();
            readChannel = null;
        }

        return false;
    }

    public ChunkedDataBaseInput(DataBase dataBase,
                                Predicate<DataInfo> predicate,
                                long offset,
                                long length) {

        Objects.requireNonNull(dataBase);
        Objects.requireNonNull(predicate);

        if (offset < 0) {
            throw new IllegalArgumentException("offset < 0");
        } else if (length < 0) {
            throw new IllegalArgumentException("length < 0");
        }

        this.dataBase = dataBase;
        this.predicate = predicate;
        this.offset = offset;
        this.length = length;
    }

    @Override
    public boolean isEndOfInput() throws Exception {
        return progress() >= length();
    }

    @Override
    public void close() throws Exception {
        if (!closed) {
            closed = true;

            // Clean up future
            if (scheduleFuture != null) {
                scheduleFuture.cancel(false);
            }

            // Clean up channel
            if (readChannel != null) {
                readChannel.close();
                readChannel = null;
            }
        }
    }

    @Override
    public ByteBuf readChunk(ChannelHandlerContext ctx) throws Exception {
        // Set the chunked write handler
        if (chunkedWriteHandler == null) {
            chunkedWriteHandler = (ChunkedWriteHandler) ctx.handler();
        }

        // Make sure we register with the channel
        if (!registered) {
            registered = true;
            ctx.channel().closeFuture().addListener(this);
        }

        // Make sure the channel has enough data
        if (!configureReadChannel()) {
            scheduleFuture = ctx.executor().schedule(this, NettyConfig.getHttpRetryDelay(), TimeUnit.MILLISECONDS);
            return null;
        }

        // Read from channel and return chunk
        ByteBuf byteBuf = ctx.alloc().buffer();
        int read = byteBuf.writeBytes(readChannel, NettyConfig.getHttpBufferSize());
        if (read == -1) {
            throw new EOFException();
        }
        completed += read;
        return byteBuf;
    }

    @Override
    public long length() {
        return length;
    }

    @Override
    public long progress() {
        return completed;
    }

    @Override
    public void run() {
        // Resume transfer
        chunkedWriteHandler.resumeTransfer();

        // Clear the schedule future
        scheduleFuture = null;
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        // Cancel if still running
        if (scheduleFuture != null) {
            scheduleFuture.cancel(false);
        }
    }
}
