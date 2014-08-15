package de.probst.ba.core.media.databases;

import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.media.DataInfo;
import io.netty.buffer.ByteBuf;

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
    public void processBuffer(String hash,
                              int chunkIndex,
                              long offset,
                              ByteBuf byteBuf,
                              int length,
                              boolean download) throws IOException {

        DataInfo dataInfo = this.dataInfo.get(hash);
        if (dataInfo == null) {
            String upOrDown = download ? "Download" : "Upload";
            throw new IOException("Data info " + upOrDown + " does not exist. Hash: " + hash);
        }

        if (download && dataInfo.isChunkCompleted(chunkIndex)) {
            throw new IOException("Chunk already available: " + chunkIndex);
        }

        if (!download && !dataInfo.isChunkCompleted(chunkIndex)) {
            throw new IOException("Chunk not available: " + chunkIndex);
        }

        long chunkSize = dataInfo.getChunkSize(chunkIndex);

        if (offset < 0 || offset >= chunkSize - 1) {
            throw new IOException("offset < 0 || offset >= chunkSize - 1");
        }

        if (length <= 0) {
            throw new IOException("length <= 0");
        }

        if (offset + length > chunkSize) {
            throw new IOException("offset + length > chunkSize");
        }

        // Fill the buffer
        if (download) {
            byteBuf.readerIndex(byteBuf.readerIndex() + length);
        } else {
            byteBuf.writeByte((byte) chunkIndex);
            byteBuf.writerIndex(byteBuf.writerIndex() + length - 1);
        }
    }

    @Override
    public void processBufferAndComplete(String hash,
                                         int chunkIndex,
                                         long offset,
                                         ByteBuf byteBuf,
                                         int length,
                                         boolean download) throws IOException {

        processBuffer(hash, chunkIndex, offset, byteBuf, length, download);
        if (download) {
            dataInfo.computeIfPresent(hash, (k, v) -> v.withChunk(chunkIndex));
        }
    }
}
