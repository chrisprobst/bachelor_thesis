package de.probst.ba.core.net.peer.handlers.transfer.messages;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by chrisprobst on 14.08.14.
 */
public final class DownloadBufferMessage implements Serializable {

    private final int chunkIndex;
    private final int offset;
    private final int length;
    private final byte[] buffer;

    public DownloadBufferMessage(int chunkIndex, int offset, int length, byte[] buffer) {
        Objects.requireNonNull(buffer);
        this.chunkIndex = chunkIndex;
        this.offset = offset;
        this.length = length;
        this.buffer = buffer;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public byte[] getBuffer() {
        return buffer;
    }
}
