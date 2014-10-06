package de.probst.ba.core.media.database.databases.memory;

import de.probst.ba.core.media.database.databases.AbstractDataBase;

/**
 * Created by chrisprobst on 05.10.14.
 */
public final class MemoryDataBase extends AbstractDataBase {
/*
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
    public synchronized List<Tuple2<DataInfo, SeekableByteChannel>> unsafeQueryRawWithName(String name)
            throws IOException {

        List<Tuple2<DataInfo, SeekableByteChannel>> results = new LinkedList<>();
        for (DataInfo dataInfo : getDataInfo().values()) {
            if (dataInfo.getName().isPresent() && dataInfo.getName().get().equals(name)) {
                results.add(unsafeQueryRaw(dataInfo.getHash()));
            }
        }
        return results;
    }

    @Override
    public synchronized Tuple2<DataInfo, SeekableByteChannel> unsafeQueryRaw(String hash) throws IOException {
        DataInfo dataInfo = get(hash);
        if (!dataInfo.isCompleted()) {
            throw new IOException("!dataInfo.isCompleted()");
        }
        ByteBuf byteBuf = data.get(dataInfo).duplicate();
        return Tuple.of(dataInfo,
                        new ReadableByteBufferChannel(byteBuf.readerIndex(0)
                                                             .writerIndex(byteBuf.capacity())
                                                             .nioBuffer()));
    }

    @Override
    public void flush() throws IOException {

    }*/
}
