package de.probst.ba.core.media.database.databases;

import de.probst.ba.core.media.database.DataInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by chrisprobst on 05.10.14.
 */
public final class InMemoryDataBase extends AbstractDataBase {

    private final Map<DataInfo, ByteBuf> data = new HashMap<>();

    @Override
    protected void doProcessBuffer(DataInfo dataInfo,
                                   int chunkIndex,
                                   long chunkSize,
                                   long offset,
                                   ByteBuf byteBuf,
                                   int length,
                                   boolean download) throws IOException {
        DataInfo full = dataInfo.full();

        if (download) {
            // Make sure there is enough space
            ByteBuf dataByteBuf = data.get(full);
            if (dataByteBuf == null) {
                dataByteBuf = Unpooled.buffer((int) full.getSize(), (int) full.getSize());
                data.put(full, dataByteBuf);
            }

            // Simply write the buffer at the specific place
            dataByteBuf.setBytes((int) (full.getOffset(chunkIndex) + offset), byteBuf, length);
        } else {
            // Cannot be null
            ByteBuf dataByteBuf = data.get(full);
            dataByteBuf.getBytes((int) (full.getOffset(chunkIndex) + offset), byteBuf, length);
        }
    }

    @Override
    public void flush() throws IOException {

    }
}
