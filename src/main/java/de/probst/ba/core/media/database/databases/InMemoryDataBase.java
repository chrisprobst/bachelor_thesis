package de.probst.ba.core.media.database.databases;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.util.io.SeekableByteBufferChannel;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
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
    public synchronized SeekableByteChannel[] unsafeQueryRawWithName(String name) throws IOException {
        return getDataInfo().values()
                            .stream()
                            .filter(dataInfo -> dataInfo.getName().isPresent())
                            .filter(dataInfo -> dataInfo.getName().get().equals(name))
                            .map(DataInfo::getHash)
                            .map(s -> {
                                try {
                                    return unsafeQueryRaw(s);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    return null;
                                }
                            })
                            .filter(c -> c != null)
                            .toArray(SeekableByteChannel[]::new);
    }

    @Override
    public synchronized SeekableByteChannel unsafeQueryRaw(String hash) throws IOException {
        DataInfo dataInfo = get(hash);
        if (!dataInfo.isCompleted()) {
            throw new IOException("!dataInfo.isCompleted()");
        }
        ByteBuf byteBuf = data.get(dataInfo).duplicate();
        return new SeekableByteBufferChannel(byteBuf.readerIndex(0).writerIndex(byteBuf.capacity()).nioBuffer());
    }

    @Override
    public void flush() throws IOException {

    }
}
