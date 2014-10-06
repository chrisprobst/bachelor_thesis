package de.probst.ba.core.util.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;

public final class LimitedReadableByteChannel implements ReadableByteChannel {

    private final ReadableByteChannel peer;
    private long current = 0;
    private final long length;

    public LimitedReadableByteChannel(ReadableByteChannel peer, long length) {
        Objects.requireNonNull(peer);
        this.peer = peer;
        this.length = length;
    }

    @Override
    public synchronized int read(ByteBuffer dst) throws IOException {
        if (current < length) {
            // Limit byte buffer
            dst.limit((int) (dst.position() + Math.min(dst.remaining(), length - current)));
            int read = peer.read(dst);
            current += read;
            return read;
        } else {
            return -1;
        }
    }

    @Override
    public boolean isOpen() {
        return peer.isOpen();
    }

    @Override
    public void close() throws IOException {
        peer.close();
    }
}