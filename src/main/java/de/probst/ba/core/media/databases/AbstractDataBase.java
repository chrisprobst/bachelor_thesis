package de.probst.ba.core.media.databases;

import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.media.DataInfo;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by chrisprobst on 15.08.14.
 */
public abstract class AbstractDataBase implements DataBase {

    protected final Map<String, DataInfo> dataInfo =
            new HashMap<>();

    protected abstract void doProcessBuffer(DataInfo dataInfo,
                                            int chunkIndex,
                                            long chunkSize,
                                            long offset,
                                            ByteBuf byteBuf,
                                            int length,
                                            boolean download) throws IOException;

    protected abstract void doComplete(DataInfo dataInfo,
                                       int chunkIndex,
                                       boolean download) throws IOException;

    @Override
    public Map<String, DataInfo> getDataInfo() {
        return new HashMap<>(dataInfo);
    }

    @Override
    public synchronized boolean addInterest(DataInfo dataInfo) {
        Objects.requireNonNull(dataInfo);

        if (!dataInfo.isEmpty()) {
            throw new IllegalArgumentException("!dataInfo.isEmpty()");
        }

        return this.dataInfo.putIfAbsent(dataInfo.getHash(), dataInfo) == null;
    }

    @Override
    public synchronized DataInfo get(String hash) {
        return dataInfo.get(hash);
    }

    @Override
    public synchronized void remove(String hash) throws IOException {
        Objects.requireNonNull(hash);

        dataInfo.remove(hash);
    }

    @Override
    public synchronized void processBuffer(String hash,
                                           int chunkIndex,
                                           long offset,
                                           ByteBuf byteBuf,
                                           int length,
                                           boolean download) throws IOException {

        Objects.requireNonNull(hash);
        Objects.requireNonNull(byteBuf);

        // Try to find the data info first
        DataInfo dataInfo = this.dataInfo.get(hash);

        if (dataInfo == null) {
            String upOrDown = download ? "downloading" : "uploading";
            throw new IllegalArgumentException("Data info for " + upOrDown +
                    " does not exist. Hash: " + hash);
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
            throw new IllegalArgumentException("" +
                    "download && byteBuf.readableBytes() < length");
        }

        if (!download && byteBuf.writableBytes() < length) {
            throw new IllegalArgumentException("" +
                    "!download && byteBuf.writableBytes() < length");
        }

        if (offset + length > chunkSize) {
            throw new IllegalArgumentException("offset + length > chunkSize");
        }

        // Do process the buffer
        doProcessBuffer(
                dataInfo,
                chunkIndex,
                chunkSize,
                offset,
                byteBuf,
                length,
                download);
    }

    @Override
    public synchronized void processBufferAndComplete(String hash,
                                                      int chunkIndex,
                                                      long offset,
                                                      ByteBuf byteBuf,
                                                      int length,
                                                      boolean download) throws IOException {

        // Process buffer as usual
        processBuffer(
                hash,
                chunkIndex,
                offset, byteBuf,
                length,
                download);

        // Complete the chunk
        doComplete(dataInfo.get(hash), chunkIndex, download);
    }
}
