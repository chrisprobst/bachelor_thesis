package de.probst.ba.core.media.database.databases.memory;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.database.databases.AbstractDataBase;
import de.probst.ba.core.media.database.databases.AbstractDataBaseReadChannel;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

public final class MemoryDataBaseReadChannel extends AbstractDataBaseReadChannel {

    private final ByteBuf byteBuf;

    public MemoryDataBaseReadChannel(AbstractDataBase dataBase,
                                     DataInfo dataInfo,
                                     ByteBuf byteBuf) {
        super(dataBase, dataInfo);
        Objects.requireNonNull(byteBuf);
        this.byteBuf = byteBuf;
    }

    @Override
    protected int doRead(ByteBuffer dst,
                         int chunkIndex,
                         long totalChunkOffset,
                         long relativeChunkOffset,
                         long chunkSize) throws IOException {

        int position = dst.position();
        byteBuf.getBytes((int) (totalChunkOffset + relativeChunkOffset), dst);
        return dst.position() - position;
    }
}