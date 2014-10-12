package de.probst.ba.core.media.database.databases.fake;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.database.databases.AbstractDataBase;
import de.probst.ba.core.media.database.databases.AbstractDataBaseReadChannel;
import de.probst.ba.core.media.database.databases.AbstractDataBaseWriteChannel;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by chrisprobst on 13.08.14.
 */
public final class FakeDataBase extends AbstractDataBase {

    @Override
    protected void doClose() throws IOException {
    }

    @Override
    protected AbstractDataBaseWriteChannel openWriteChannel(DataInfo writeDataInfo) throws IOException {
        return new AbstractDataBaseWriteChannel(this, writeDataInfo) {
            @Override
            protected int doWrite(ByteBuffer src,
                                  int chunkIndex,
                                  long totalChunkOffset,
                                  long relativeChunkOffset,
                                  long chunkSize) throws IOException {
                int amount = src.remaining();
                src.position(src.position() + amount);
                return amount;
            }

            @Override
            protected void doClose() throws IOException {

            }
        };
    }

    @Override
    protected AbstractDataBaseReadChannel openReadChannel(DataInfo readDataInfo) throws IOException {
        return new AbstractDataBaseReadChannel(this, readDataInfo) {
            @Override
            protected int doRead(ByteBuffer dst,
                                 int chunkIndex,
                                 long totalChunkOffset,
                                 long relativeChunkOffset,
                                 long chunkSize) throws IOException {
                int amount = dst.remaining();
                dst.position(dst.position() + amount);
                return amount;
            }

            @Override
            protected void doClose() throws IOException {

            }
        };
    }

    public FakeDataBase(boolean allowOverwrite) {
        super(allowOverwrite);
    }
}
