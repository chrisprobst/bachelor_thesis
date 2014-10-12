package de.probst.ba.core.util.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;

public final class LimitedReadableByteChannel implements ReadableByteChannel {

    private final ReadableByteChannel peer;
    private final boolean closePeer;
    private boolean closed;
    private long current = 0;
    private final long length;

    protected final void checkClosed() throws ClosedChannelException {
        if (closed) {
            throw new ClosedChannelException();
        }
    }

    public LimitedReadableByteChannel(ReadableByteChannel peer, long length, boolean closePeer) {
        Objects.requireNonNull(peer);
        this.peer = peer;
        this.length = length;
        this.closePeer = closePeer;
    }

    @Override
    public synchronized int read(ByteBuffer dst) throws IOException {
        checkClosed();
        if (current < length) {
            // Limit byte buffer
            dst.limit((int) (dst.position() + Math.min(dst.remaining(), length - current)));
            int read = peer.read(dst);
            if (read >= 0) {
                current += read;
                return read;
            }
        }
        return -1;
    }

    @Override
    public synchronized boolean isOpen() {
        return !closed;
    }

    @Override
    public synchronized void close() throws IOException {
        if (!closed) {
            closed = true;
            if (closePeer) {
                peer.close();
            }
        }
    }
}