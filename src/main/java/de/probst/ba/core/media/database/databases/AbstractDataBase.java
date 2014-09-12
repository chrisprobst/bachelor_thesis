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
    private long tokens = 0;
    private long stamps = 0;
    private final Map<Long, Consumer<Tuple2<Long, Map<String, DataInfo>>>> listeners = new HashMap<>();

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
        }
    }

    @Override
    public synchronized Tuple2<Long, Map<String, DataInfo>> getDataInfoWithStamp() {
        return Tuple.of(stamps++, dataInfo);
    }

    @Override
    public synchronized Map<String, DataInfo> getDataInfo() {
        Map<String, DataInfo> copy = new HashMap<>(dataInfo);
        dataInfo.forEach((k, v) -> {
            if (v.isEmpty()) {
                copy.remove(k);
            }
        });
        return Collections.unmodifiableMap(copy);
    }

    @Override
    public void update(DataInfo dataInfo) {
        Tuple2<Long, Map<String, DataInfo>> newDataInfo = null;
        synchronized (this) {
            DataInfo old = this.dataInfo.put(dataInfo.getHash(), dataInfo);
            if (!dataInfo.equals(old)) {
                newDataInfo = getDataInfoWithStamp();
            }
        }
        if (newDataInfo != null) {
            Tuple2<Long, Map<String, DataInfo>> finalDataInfo = newDataInfo;
            listeners.values().forEach(c -> c.accept(finalDataInfo));
        }
    }

    @Override
    public synchronized DataInfo get(String hash) {
        return dataInfo.get(hash);
    }

    @Override
    public long subscribe(Consumer<Tuple2<Long, Map<String, DataInfo>>> consumer) {
        Objects.requireNonNull(consumer);
        Tuple2<Long, Map<String, DataInfo>> newDataInfo;
        long token;
        synchronized (this) {
            token = tokens++;
            listeners.put(token, consumer);
            newDataInfo = getDataInfoWithStamp();
        }
        consumer.accept(newDataInfo);
        return token;
    }

    @Override
    public synchronized boolean cancel(long token) {
        return listeners.remove(token) != null;
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
    public void processBufferAndComplete(DataInfo dataInfo,
                                         int chunkIndex,
                                         long offset,
                                         ByteBuf byteBuf,
                                         int length,
                                         boolean download) throws IOException {

        Tuple2<Long, Map<String, DataInfo>> newDataInfo;
        synchronized (this) {
            // Process buffer as usual
            processBuffer(dataInfo, chunkIndex, offset, byteBuf, length, download);

            // Complete the chunk
            doComplete(dataInfo, chunkIndex, download);
            newDataInfo = getDataInfoWithStamp();
        }

        listeners.values().forEach(c -> c.accept(newDataInfo));
    }
}
