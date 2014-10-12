package de.probst.ba.core.media.database.databases;

import de.probst.ba.core.media.database.DataBaseWriteChannel;
import de.probst.ba.core.media.database.DataInfo;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by chrisprobst on 06.10.14.
 */
public abstract class AbstractDataBaseWriteChannel extends AbstractDataBaseChannel implements DataBaseWriteChannel {

    protected abstract int doWrite(ByteBuffer src,
                                   int chunkIndex,
                                   long totalChunkOffset,
                                   long relativeChunkOffset,
                                   long chunkSize) throws IOException;

    public AbstractDataBaseWriteChannel(AbstractDataBase dataBase, DataInfo dataInfo) {
        super(dataBase, dataInfo);
    }

    @Override
    public synchronized DataBaseWriteChannel position(long position) throws IOException {
        return (DataBaseWriteChannel) super.position(position);
    }

    @Override
    public final synchronized long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        checkClosed();
        long totalWritten = 0;
        for (int i = 0; i < length; i++) {
            ByteBuffer byteBuffer = srcs[offset + i];
            totalWritten += write(byteBuffer);

            if (byteBuffer.hasRemaining()) {
                break;
            }
        }
        return totalWritten;
    }

    @Override
    public final long write(ByteBuffer[] srcs) throws IOException {
        return write(srcs, 0, srcs.length);
    }

    @Override
    public final int write(ByteBuffer src) throws IOException {
        int written;
        boolean isCompleted;
        synchronized (this) {
            checkClosed();
            long completed = position();
            long size = size();

            if (completed >= size) {
                throw new IOException("Database write channel full");
            }

            // Setup byte buffer limit
            int newLimit = (int) Math.min(src.remaining(), size - completed);
            if (newLimit <= 0) {
                return 0;
            }

            ByteBuffer copy = (ByteBuffer) src.duplicate().limit(src.position() + newLimit);

            // Calculate state
            int chunkIndex = getDataInfo().getTotalChunkIndex(completed, false);
            long totalChunkOffset = getDataInfo().getTotalOffset(chunkIndex);
            long relativeChunkOffset = completed - getDataInfo().getRelativeOffset(chunkIndex);
            long chunkSize = getDataInfo().getChunkSize(chunkIndex);

            // Do write and increase counter
            written = doWrite(copy, chunkIndex, totalChunkOffset, relativeChunkOffset, chunkSize);
            src.position(src.position() + written);
            position(completed += written);
            isCompleted = completed >= size;
        }

        // If we are completed, notify the database
        if (isCompleted) {
            getDataBase().merge(getDataInfo());
        }

        return written;
    }
}
