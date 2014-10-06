package de.probst.ba.core.util.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;

/**
 * Created by chrisprobst on 05.10.14.
 */
public final class SeekableByteBufferChannel implements SeekableByteChannel {

    private final ByteBuffer byteBuffer;
    private boolean closed = false;

    private void checkClosed() throws ClosedChannelException {
        if (closed) {
            throw new ClosedChannelException();
        }
    }

    public SeekableByteBufferChannel(ByteBuffer byteBuffer) {
        Objects.requireNonNull(byteBuffer);
        this.byteBuffer = byteBuffer;
    }

    @Override
    public synchronized int read(ByteBuffer dst) throws IOException {
        checkClosed();

        if (!byteBuffer.hasRemaining()) {
            return -1;
        }

        // Get old limit
        int oldLimit = byteBuffer.limit();

        // Calc buffer size
        int bufferSize = Math.min(byteBuffer.remaining(), dst.remaining());

        // Set new limit
        byteBuffer.limit(byteBuffer.position() + bufferSize);

        // Transfer bytes
        dst.put(byteBuffer);

        // Restore old limit
        byteBuffer.limit(oldLimit);

        return bufferSize;
    }

    @Override
    public synchronized int write(ByteBuffer src) throws IOException {
        checkClosed();

        if (!byteBuffer.hasRemaining()) {
            throw new IOException("!byteBuffer.hasRemaining()");
        }
        // Get old limit
        int oldLimit = src.limit();

        // Calc buffer size
        int bufferSize = Math.min(byteBuffer.remaining(), src.remaining());

        // Set new limit
        src.limit(src.position() + bufferSize);

        // Transfer bytes
        byteBuffer.put(src);

        // Restore old limit
        src.limit(oldLimit);

        return bufferSize;
    }

    @Override
    public synchronized long position() throws IOException {
        checkClosed();
        return byteBuffer.position();
    }

    @Override
    public synchronized SeekableByteChannel position(long newPosition) throws IOException {
        checkClosed();
        byteBuffer.position((int) newPosition);
        return this;
    }

    @Override
    public synchronized long size() throws IOException {
        checkClosed();
        return byteBuffer.limit();
    }

    @Override
    public synchronized SeekableByteChannel truncate(long size) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public synchronized boolean isOpen() {
        return !closed;
    }

    @Override
    public synchronized void close() throws IOException {
        closed = true;
    }
}
