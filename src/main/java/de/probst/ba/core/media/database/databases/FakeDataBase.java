package de.probst.ba.core.media.database.databases;

import de.probst.ba.core.media.database.DataInfo;
import io.netty.buffer.ByteBuf;

import java.io.IOException;

/**
 * Created by chrisprobst on 13.08.14.
 */
public final class FakeDataBase extends AbstractDataBase {

    @Override
    protected void doProcessBuffer(DataInfo dataInfo,
                                   int chunkIndex,
                                   long chunkSize,
                                   long offset,
                                   ByteBuf byteBuf,
                                   int length,
                                   boolean download) throws IOException {
        if (download) {
            byteBuf.readerIndex(byteBuf.readerIndex() + length);
        } else {
            if (offset == 0) {
                byteBuf.writeByte((byte) chunkIndex);
                byteBuf.writerIndex(byteBuf.writerIndex() + length - 1);
            } else {
                byteBuf.writerIndex(byteBuf.writerIndex() + length);
            }
        }
    }

    @Override
    public void flush() throws IOException {

    }
}
