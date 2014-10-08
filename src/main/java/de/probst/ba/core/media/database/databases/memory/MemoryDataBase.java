package de.probst.ba.core.media.database.databases.memory;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.database.databases.AbstractDataBase;
import de.probst.ba.core.media.database.databases.AbstractDataBaseReadChannel;
import de.probst.ba.core.media.database.databases.AbstractDataBaseWriteChannel;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by chrisprobst on 05.10.14.
 */
public final class MemoryDataBase extends AbstractDataBase {

    private final Map<DataInfo, ByteBuf> data = new HashMap<>();

    @Override
    protected AbstractDataBaseWriteChannel openWriteChannel(DataInfo writeDataInfo) throws IOException {
        return new AbstractDataBaseWriteChannel(this, writeDataInfo) {
            @Override
            protected int doWrite(ByteBuffer src,
                                  int chunkIndex,
                                  long totalChunkOffset,
                                  long relativeChunkOffset,
                                  long chunkSize) throws IOException {
                // Get full representation
                DataInfo full = getDataInfo().full();

                // Make sure there is enough space
                ByteBuf dataByteBuf = data.get(full);
                if (dataByteBuf == null) {
                    dataByteBuf = Unpooled.buffer((int) full.getSize(), (int) full.getSize());
                    data.put(full, dataByteBuf);
                }

                int position = src.position();

                // Simply write the buffer at the specific place
                dataByteBuf.setBytes((int) (totalChunkOffset + relativeChunkOffset), src);

                return src.position() - position;
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

                // Get full representation
                DataInfo full = getDataInfo().full();

                int position = dst.position();

                // Cannot be null
                ByteBuf dataByteBuf = data.get(full);
                dataByteBuf.getBytes((int) (totalChunkOffset + relativeChunkOffset), dst);

                return dst.position() - position;
            }

            @Override
            protected void doClose() throws IOException {

            }
        };
    }

    @Override
    public void flush() throws IOException {

    }
}