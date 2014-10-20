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

    @Override
    protected void doClose() throws IOException {
    }

    public AbstractDataBaseReadChannel(AbstractDataBase dataBase, DataInfo dataInfo) {
        super(dataBase, dataInfo);
    }

    @Override
    public synchronized DataBaseReadChannel position(long position) throws IOException {
        return (DataBaseReadChannel) super.position(position);
    }

    @Override
    public synchronized final long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
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
    public synchronized final int read(ByteBuffer dst) throws IOException {
        checkClosed();
        if (isCompleted()) {
            return -1;
        }

        // Setup byte buffer limit
        int newLimit = (int) Math.min(dst.remaining(), remaining());
        if (newLimit <= 0) {
            return 0;
        }

        ByteBuffer copy = (ByteBuffer) dst.duplicate().limit(dst.position() + newLimit);
        long position = position();

        // Calculate state
        int chunkIndex = getDataInfo().getTotalChunkIndex(position, false);
        long totalChunkOffset = getDataInfo().getTotalOffset(chunkIndex);
        long relativeChunkOffset = position - getDataInfo().getRelativeOffset(chunkIndex);
        long chunkSize = getDataInfo().getChunkSize(chunkIndex);

        // Do read and increase counter
        int read = doRead(copy, chunkIndex, totalChunkOffset, relativeChunkOffset, chunkSize);
        if (read == -1) {
            throw new EOFException("Unexpected EOF detected");
        }
        dst.position(dst.position() + read);
        position(position + read);
        return read;
    }
}
