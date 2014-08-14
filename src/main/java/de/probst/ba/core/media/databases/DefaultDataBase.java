package de.probst.ba.core.media.databases;

import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.media.DataInfo;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 13.08.14.
 */
public final class DefaultDataBase implements DataBase {

    private final Map<String, DataInfo> dataInfo =
            new HashMap<>();

    public DefaultDataBase() {
    }

    public DefaultDataBase(DataInfo... dataInfo) {
        this(Arrays.stream(dataInfo)
                .collect(Collectors.toMap(
                        DataInfo::getHash,
                        Function.identity())));
    }

    public DefaultDataBase(Map<String, DataInfo> initialDataInfo) {
        dataInfo.putAll(initialDataInfo);
    }

    @Override
    public Map<String, DataInfo> getDataInfo() {
        return new HashMap<>(dataInfo);
    }

    @Override
    public synchronized boolean add(DataInfo dataInfo) {
        if (!dataInfo.isEmpty()) {
            throw new IllegalArgumentException("!dataInfo.isEmpty()");
        }

        if (this.dataInfo.containsKey(dataInfo.getHash())) {
            return false;
        }

        this.dataInfo.put(dataInfo.getHash(), dataInfo);
        return true;
    }

    @Override
    public void delete(String hash) {
        this.dataInfo.remove(hash);
    }


    @Override
    public synchronized void storeBufferAndComplete(String hash,
                                                    int chunkIndex,
                                                    long offset,
                                                    byte[] buffer) throws IOException {
        storeBuffer(hash, chunkIndex, offset, buffer);
        dataInfo.computeIfPresent(hash, (k, v) -> v.withChunk(chunkIndex));
    }

    @Override
    public synchronized void storeBuffer(String hash,
                                         int chunkIndex,
                                         long offset,
                                         byte[] buffer) throws IOException {

        DataInfo dataInfo = this.dataInfo.get(hash);
        if (dataInfo == null) {
            throw new IOException("Data info does not exist. Hash: " + hash);
        }

        if (dataInfo.isChunkCompleted(chunkIndex)) {
            throw new IOException("Chunk already available: " + chunkIndex);
        }

        long chunkSize = dataInfo.getChunkSize(chunkIndex);

        if (offset < 0 || offset >= chunkSize - 1) {
            throw new IOException("offset < 0 || offset >= chunkSize - 1");
        }

        if (buffer.length > chunkSize) {
            throw new IOException("buffer.length > chunkSize");
        }

        if (offset + buffer.length > chunkSize) {
            throw new IOException("offset + buffer.length > chunkSize");
        }
    }

    @Override
    public synchronized byte[] loadBuffer(String hash,
                                          int chunkIndex,
                                          long offset,
                                          int length) throws IOException {

        DataInfo dataInfo = this.dataInfo.get(hash);
        if (dataInfo == null) {
            throw new IOException("Data info does not exist. Hash: " + hash);
        }

        if (!dataInfo.isChunkCompleted(chunkIndex)) {
            throw new IOException("Chunk not available: " + chunkIndex);
        }

        long chunkSize = dataInfo.getChunkSize(chunkIndex);

        if (offset < 0 || offset >= chunkSize - 1) {
            throw new IOException("offset < 0 || offset >= chunkSize - 1");
        }

        if (length <= 0 || length > chunkSize) {
            throw new IOException("length <= 0 || length > chunkSize");
        }

        if (offset + length > chunkSize) {
            throw new IOException("offset + length > chunkSize");
        }

        return new byte[length];
    }
}
