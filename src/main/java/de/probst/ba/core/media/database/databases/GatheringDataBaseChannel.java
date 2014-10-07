package de.probst.ba.core.media.database.databases;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.GatheringByteChannel;
import java.util.Objects;

/**
 * Created by chrisprobst on 06.10.14.
 */
public final class GatheringDataBaseChannel implements GatheringByteChannel {

    private final Runnable abortCallback;
    private final Runnable completedCallback;
    private final long total;
    private long completed;
    private boolean closed;

    private void checkClosed() throws ClosedChannelException {
        if (closed) {
            throw new ClosedChannelException();
        }
    }

    public GatheringDataBaseChannel(Runnable abortCallback, Runnable completedCallback, long total) {
        Objects.requireNonNull(abortCallback);
        Objects.requireNonNull(completedCallback);
        this.abortCallback = abortCallback;
        this.completedCallback = completedCallback;
        this.total = total;
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        throw new UnsupportedOperationException("Not implemeted yet");
    }

    @Override
    public long write(ByteBuffer[] srcs) throws IOException {
        throw new UnsupportedOperationException("Not implemeted yet");
    }

    @Override
    public synchronized int write(ByteBuffer src) throws IOException {
        checkClosed();
        if (completed >= total) {
            throw new IOException("Channel full");
        }
        int amount = (int) Math.min(src.remaining(), total - completed);
        src.position(src.position() + amount);
        completed += amount;
        if (completed >= total) {
            completedCallback.run();
            close();
        }
        return amount;
    }

    @Override
    public synchronized boolean isOpen() {
        return !closed;
    }

    @Override
    public synchronized void close() throws IOException {
        if (!closed) {
            closed = true;
            abortCallback.run();
        }
    }
}
