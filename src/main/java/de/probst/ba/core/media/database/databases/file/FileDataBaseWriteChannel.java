package de.probst.ba.core.media.database.databases.file;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.database.databases.AbstractDataBase;
import de.probst.ba.core.media.database.databases.AbstractDataBaseWriteChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;

public final class FileDataBaseWriteChannel extends AbstractDataBaseWriteChannel {

    private final FileChannel fileChannel;

    public FileDataBaseWriteChannel(AbstractDataBase dataBase,
                                    DataInfo dataInfo,
                                    FileChannel fileChannel) {
        super(dataBase, dataInfo);
        Objects.requireNonNull(fileChannel);
        this.fileChannel = fileChannel;
    }

    @Override
    protected int doWrite(ByteBuffer src,
                          int chunkIndex,
                          long totalChunkOffset,
                          long relativeChunkOffset,
                          long chunkSize) throws IOException {

        return fileChannel.write(src, totalChunkOffset + relativeChunkOffset);
    }

    @Override
    protected void doClose() throws IOException {
        fileChannel.force(true);
    }
}