package de.probst.ba.core.media.database.databases;

import de.probst.ba.core.media.database.DataBaseReadChannel;
import de.probst.ba.core.media.database.DataInfo;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by chrisprobst on 06.10.14.
 */
public abstract class AbstractDataBaseReadChannel extends AbstractDataBaseChannel implements DataBaseReadChannel {

    protected abstract int doRead(ByteBuffer dst,
                                  int chunkIndex,
                                  long totalChunkOffset,
                                  long relativeChunkOffset,
                                  long chunkSize) throws IOException;

    public AbstractDataBaseReadChannel(AbstractDataBase dataBase, DataInfo dataInfo) {
        super(dataBase, dataInfo);
    }

    @Override
    public synchronized DataBaseReadChannel position(long position) throws IOException {
        return (DataBaseReadChannel) super.position(position);
    }

    @Override
    public final synchronized long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        checkClosed();
        long totalRead = 0;
        for (int i = 0; i < length; i++) {
            ByteBuffer byteBuffer = dsts[offset + i];

            int read = read(byteBuffer);
            if (read == -1) {
                if (totalRead > 0) {
                    return totalRead;
                } else {
                    return -1;
                }
            }

            totalRead += read;
            if (byteBuffer.hasRemaining()) {
                break;
            }
        }
        return totalRead;
    }

    @Override
    public final long read(ByteBuffer[] dsts) throws IOException {
        return read(dsts, 0, dsts.length);
    }

    @Override
    public final synchronized int read(ByteBuffer dst) throws IOException {
        checkClosed();
        long completed = position();
        long size = size();

        if (completed >= size) {
            return -1;
        }

        // Setup byte buffer limit
        int newLimit = (int) Math.min(dst.remaining(), size - completed);
        ByteBuffer copy = (ByteBuffer) dst.duplicate().limit(dst.position() + newLimit);

        // Calculate state
        int chunkIndex = getDataInfo().getTotalChunkIndex(completed, false);
        long totalChunkOffset = getDataInfo().getTotalOffset(chunkIndex);
        long relativeChunkOffset = completed - getDataInfo().getRelativeOffset(chunkIndex);
        long chunkSize = getDataInfo().getChunkSize(chunkIndex);

        // Do read and increase counter
        int read = doRead(copy, chunkIndex, totalChunkOffset, relativeChunkOffset, chunkSize);
        if (read == -1) {
            throw new EOFException("Unexpected EOF detected");
        }
        dst.position(dst.position() + read);
        position(completed += read);

        // If we are completed, notify the database
        if (completed >= size) {
            getDataBase().update(getDataInfo());
        }

        return read;
    }
}
