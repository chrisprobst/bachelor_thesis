package de.probst.ba.core.media.database.databases;

import de.probst.ba.core.media.database.DataBaseWriteChannel;
import de.probst.ba.core.media.database.DataInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * Created by chrisprobst on 06.10.14.
 */
public abstract class AbstractDataBaseWriteChannel extends AbstractDataBaseChannel implements DataBaseWriteChannel {

    private DataInfo mergedDataInfo;

    @Override
    protected void doClose() throws IOException {
        if (isCompleted()) {
            mergedDataInfo = getDataBase().merge(getDataInfo());
        }
    }

    protected abstract int doWrite(ByteBuffer src,
                                   int chunkIndex,
                                   long totalChunkOffset,
                                   long relativeChunkOffset,
                                   long chunkSize) throws IOException;

    public AbstractDataBaseWriteChannel(AbstractDataBase dataBase, DataInfo dataInfo) {
        super(dataBase, dataInfo);
    }

    @Override
    public synchronized Optional<DataInfo> getMergedDataInfo() {
        return Optional.ofNullable(mergedDataInfo);
    }

    @Override
    public synchronized DataBaseWriteChannel position(long position) throws IOException {
        return (DataBaseWriteChannel) super.position(position);
    }

    @Override
    public synchronized final long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
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
    public synchronized final int write(ByteBuffer src) throws IOException {
        checkClosed();
        if (isCompleted()) {
            throw new IOException("isCompleted()");
        }

        // Setup byte buffer limit
        int newLimit = (int) Math.min(src.remaining(), remaining());
        if (newLimit <= 0) {
            return 0;
        }

        ByteBuffer copy = (ByteBuffer) src.duplicate().limit(src.position() + newLimit);
        long position = position();

        // Calculate state
        int chunkIndex = getDataInfo().getTotalChunkIndex(position, false);
        long totalChunkOffset = getDataInfo().getTotalOffset(chunkIndex);
        long relativeChunkOffset = position - getDataInfo().getRelativeOffset(chunkIndex);
        long chunkSize = getDataInfo().getChunkSize(chunkIndex);

        // Do write and increase counter
        int written = doWrite(copy, chunkIndex, totalChunkOffset, relativeChunkOffset, chunkSize);
        src.position(src.position() + written);
        position(position + written);
        return written;
    }
}
