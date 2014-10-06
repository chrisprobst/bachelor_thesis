package de.probst.ba.core.media.database.databases;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ScatteringByteChannel;

/**
 * Created by chrisprobst on 06.10.14.
 */
public final class ScatteringDataBaseChannel implements ScatteringByteChannel {

    private final long total;
    private long completed;
    private boolean closed;

    private void checkClosed() throws ClosedChannelException {
        if (closed) {
            throw new ClosedChannelException();
        }
    }

    public ScatteringDataBaseChannel(long total) {
        this.total = total;
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        throw new UnsupportedOperationException("Not implemeted yet");
    }

    @Override
    public long read(ByteBuffer[] dsts) throws IOException {
        throw new UnsupportedOperationException("Not implemeted yet");
    }

    @Override
    public synchronized int read(ByteBuffer dst) throws IOException {
        checkClosed();
        if (completed >= total) {
            return -1;
        }

        int amount = (int) Math.min(dst.remaining(), total - completed);
        dst.position(dst.position() + amount);
        completed += amount;
        return amount;
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
