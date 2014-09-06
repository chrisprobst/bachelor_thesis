package de.probst.ba.core.media.database.databases.file;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.database.databases.AbstractDataBase;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Created by chrisprobst on 31.08.14.
 */
public final class SingleFileDataBase extends AbstractDataBase {

    private final FileChannel fileChannel;

    public SingleFileDataBase(Path path, DataInfo singleDataInfo) throws IOException {
        Objects.requireNonNull(singleDataInfo);
        fileChannel =
                FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
        dataInfo.put(singleDataInfo.getHash(), singleDataInfo);
    }

    @Override
    protected void doProcessBuffer(DataInfo dataInfo,
                                   int chunkIndex,
                                   long chunkSize,
                                   long offset,
                                   ByteBuf byteBuf,
                                   int length,
                                   boolean download) throws IOException {

        long position = dataInfo.getOffset(chunkIndex) + offset;
        if (download) {
            // Write into extra buffer and read back
            ByteBuffer buf = ByteBuffer.allocate(length);
            byteBuf.readBytes(buf);
            buf.flip();
            fileChannel.write(buf, position);

        } else {
            // Read into extra buffer and write back
            ByteBuffer buf = ByteBuffer.allocate(length);
            fileChannel.read(buf, position);
            buf.flip();
            byteBuf.writeBytes(buf);
        }
    }

    @Override
    public void flush() throws IOException {
        fileChannel.close();
    }
}
