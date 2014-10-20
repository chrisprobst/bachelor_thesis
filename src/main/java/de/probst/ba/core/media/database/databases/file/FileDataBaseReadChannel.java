package de.probst.ba.core.media.database.databases.file;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.database.databases.AbstractDataBase;
import de.probst.ba.core.media.database.databases.AbstractDataBaseReadChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;

public final class FileDataBaseReadChannel extends AbstractDataBaseReadChannel {

    private final FileChannel fileChannel;

    public FileDataBaseReadChannel(AbstractDataBase dataBase,
                                   DataInfo dataInfo,
                                   FileChannel fileChannel) {
        super(dataBase, dataInfo);
        Objects.requireNonNull(fileChannel);
        this.fileChannel = fileChannel;
    }

    @Override
    protected int doRead(ByteBuffer dst,
                         int chunkIndex,
                         long totalChunkOffset,
                         long relativeChunkOffset,
                         long chunkSize) throws IOException {

        return fileChannel.read(dst, totalChunkOffset + relativeChunkOffset);
    }
}