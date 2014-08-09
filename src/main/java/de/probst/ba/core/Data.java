package de.probst.ba.core;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Objects;

/**
 * Created by chrisprobst on 03.08.14.
 */
public class Data implements Serializable {

    private final String hash;
    private final long size;
    private final int chunkCount;
    private final long chunkSize;
    private final long lastChunkSize;
    private final BitSet chunks;

    public Data(String hash, long size, int chunkCount) {
        Objects.requireNonNull(hash);

        if (size < 0) {
            throw new IllegalArgumentException("size must be positive");
        }

        if (chunkCount < 0) {
            throw new IllegalArgumentException("chunkCount must be positive");
        }

        if (chunkCount > size) {
            throw new IllegalArgumentException("chunkCount cannot be greater than size");
        }

        this.hash = hash;
        this.size = size;
        this.chunkCount = chunkCount;

        chunkSize = size / chunkCount;
        long remaining = size % chunkCount;

        lastChunkSize = remaining > 0 ? remaining : chunkSize;
        chunks = new BitSet(chunkCount);
    }

    public Data flip() {
        Data data = empty();
        for (int i = 0; i < data.getChunkCount(); i++) {
            data.setChunk(i, !isChunkCompleted(i));
        }
        return data;
    }

    public Data duplicate() {
        Data data = empty();
        for (int i = 0; i < data.getChunkCount(); i++) {
            data.setChunk(i, isChunkCompleted(i));
        }
        return data;
    }

    public Data empty() {
        return new Data(
                getHash(),
                getSize(),
                getChunkCount());
    }

    public String getHash() {
        return hash;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public long getSize() {
        return size;
    }

    public long getChunkSize(int chunkIndex) {
        if (chunkIndex < 0 || chunkIndex >= chunkCount) {
            throw new IndexOutOfBoundsException("chunkIndex");
        }
        return chunkIndex < chunkCount - 1 ? chunkSize : lastChunkSize;
    }

    public boolean isChunkCompleted(int chunkIndex) {
        if (chunkIndex < 0 || chunkIndex >= chunkCount) {
            throw new IndexOutOfBoundsException("chunkIndex");
        }

        return chunks.get(chunkIndex);
    }

    public Data setChunk(int chunkIndex, boolean value) {
        if (chunkIndex < 0 || chunkIndex >= chunkCount) {
            throw new IndexOutOfBoundsException("chunkIndex");
        }

        chunks.set(chunkIndex, value);
        return this;
    }

    @Override
    public String toString() {
        return "Data{" +
                "hash='" + hash + '\'' +
                ", size=" + size +
                ", chunkCount=" + chunkCount +
                ", chunks=" + chunks +
                '}';
    }
}
