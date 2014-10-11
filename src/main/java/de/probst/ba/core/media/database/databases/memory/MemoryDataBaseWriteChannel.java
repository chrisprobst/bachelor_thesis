package de.probst.ba.core.media.database.databases.memory;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.database.databases.AbstractDataBase;
import de.probst.ba.core.media.database.databases.AbstractDataBaseWriteChannel;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

public final class MemoryDataBaseWriteChannel extends AbstractDataBaseWriteChannel {

    private final ByteBuf byteBuf;

    public MemoryDataBaseWriteChannel(AbstractDataBase dataBase,
                                      DataInfo dataInfo,
                                      ByteBuf byteBuf) {
        super(dataBase, dataInfo);
        Objects.requireNonNull(byteBuf);
        this.byteBuf = byteBuf;
    }

    @Override
    protected int doWrite(ByteBuffer src,
                          int chunkIndex,
                          long totalChunkOffset,
                          long relativeChunkOffset,
                          long chunkSize) throws IOException {

        int position = src.position();
        byteBuf.setBytes((int) (totalChunkOffset + relativeChunkOffset), src);
        return src.position() - position;
    }

    @Override
    protected void doClose() throws IOException {
    }
}
