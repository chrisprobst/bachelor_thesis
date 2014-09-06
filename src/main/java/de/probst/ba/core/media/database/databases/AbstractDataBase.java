package de.probst.ba.core.media.database.databases;

import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.util.collections.Tuple;
import de.probst.ba.core.util.collections.Tuple2;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Created by chrisprobst on 15.08.14.
 */
public abstract class AbstractDataBase implements DataBase {

    protected final Map<String, DataInfo> dataInfo = new HashMap<>();

    protected abstract void doProcessBuffer(DataInfo dataInfo,
                                            int chunkIndex,
                                            long chunkSize,
                                            long offset,
                                            ByteBuf byteBuf,
                                            int length,
                                            boolean download) throws IOException;

    protected void doComplete(DataInfo dataInfo, int chunkIndex, boolean download) throws IOException {
        if (download) {
            this.dataInfo.computeIfPresent(dataInfo.getHash(), (k, v) -> v.withChunk(chunkIndex));

            Map<String, DataInfo> newDataInfo = getDataInfo();
            listeners.values().forEach(c -> c.accept(newDataInfo));
        }
    }

    @Override
    public synchronized Map<String, DataInfo> getDataInfo() {
        return Collections.unmodifiableMap(new HashMap<>(dataInfo));
    }

    @Override
    public synchronized DataInfo get(String hash) {
        return dataInfo.get(hash);
    }

    private long tokens = 0;
    private final Map<Long, Consumer<Map<String, DataInfo>>> listeners = new HashMap<>();

    @Override
    public synchronized Tuple2<Long, Map<String, DataInfo>> subscribe(Consumer<Map<String, DataInfo>> consumer) {
        Objects.requireNonNull(consumer);
        long token = tokens++;
        listeners.put(token, consumer);
        return Tuple.of(token, getDataInfo());
    }

    @Override
    public synchronized void cancel(long token) {
        listeners.remove(token);
    }

    @Override
    public synchronized void processBuffer(DataInfo processDataInfo,
                                           int chunkIndex,
                                           long offset,
                                           ByteBuf byteBuf,
                                           int length,
                                           boolean download) throws IOException {

        Objects.requireNonNull(processDataInfo);
        Objects.requireNonNull(byteBuf);
        DataInfo dataInfo = this.dataInfo.get(processDataInfo.getHash());

        if (!download && dataInfo == null) {
            throw new IllegalArgumentException("Data info for uploading does not exist: " + processDataInfo);
        } else if (dataInfo == null) {
            dataInfo = processDataInfo.empty();
            this.dataInfo.put(dataInfo.getHash(), dataInfo);
        }

        if (download && dataInfo.isChunkCompleted(chunkIndex)) {
            throw new IllegalArgumentException("Chunk already completed: " + chunkIndex);
        }

        if (!download && !dataInfo.isChunkCompleted(chunkIndex)) {
            throw new IllegalArgumentException("Chunk not completed: " + chunkIndex);
        }

        long chunkSize = dataInfo.getChunkSize(chunkIndex);

        if (offset < 0 || offset >= chunkSize - 1) {
            throw new IllegalArgumentException("offset < 0 || offset >= chunkSize - 1");
        }

        if (length <= 0) {
            throw new IllegalArgumentException("length <= 0");
        }

        if (download && byteBuf.readableBytes() < length) {
            throw new IllegalArgumentException("download && byteBuf.readableBytes() < length");
        }

        if (!download && byteBuf.writableBytes() < length) {
            throw new IllegalArgumentException("!download && byteBuf.writableBytes() < length");
        }

        if (offset + length > chunkSize) {
            throw new IllegalArgumentException("offset + length > chunkSize");
        }

        // Do process the buffer
        doProcessBuffer(dataInfo, chunkIndex, chunkSize, offset, byteBuf, length, download);
    }

    @Override
    public synchronized void processBufferAndComplete(DataInfo dataInfo,
                                                      int chunkIndex,
                                                      long offset,
                                                      ByteBuf byteBuf,
                                                      int length,
                                                      boolean download) throws IOException {

        // Process buffer as usual
        processBuffer(dataInfo, chunkIndex, offset, byteBuf, length, download);

        // Complete the chunk
        doComplete(dataInfo, chunkIndex, download);
    }
}
