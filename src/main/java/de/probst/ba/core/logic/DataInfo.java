package de.probst.ba.core.logic;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Created by chrisprobst on 03.08.14.
 */
public final class DataInfo implements Serializable {

    private final String hash;
    private final long size;
    private final int chunkCount;
    private final long chunkSize;
    private final long lastChunkSize;
    private final BitSet chunks;

    public DataInfo(String hash, long size, int chunkCount) {
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

    public DataInfo flip() {
        DataInfo dataInfo = empty();
        dataInfo.chunks.flip(0, dataInfo.getChunkCount());
        return dataInfo;
    }

    public DataInfo duplicate() {
        DataInfo dataInfo = empty();
        dataInfo.chunks.or(chunks);
        return dataInfo;
    }

    public DataInfo full() {
        DataInfo dataInfo = empty();
        dataInfo.chunks.set(0, dataInfo.getChunkCount(), true);
        return dataInfo;
    }

    public DataInfo empty() {
        return new DataInfo(
                getHash(),
                getSize(),
                getChunkCount());
    }

    public boolean isCompatibleWith(DataInfo other) {
        Objects.requireNonNull(other);
        return other.getHash().equals(getHash()) &&
                other.getSize() == getSize() &&
                other.getChunkCount() == getChunkCount();
    }

    public void ensureCompatibility(DataInfo other) {
        if(!isCompatibleWith(other)) {
            throw new IllegalArgumentException("dataInfo not compatible");
        }
    }

    public DataInfo union(DataInfo other) {
        ensureCompatibility(other);

        DataInfo dataInfo = duplicate();
        dataInfo.chunks.or(other.chunks);
        return dataInfo;
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
        if (chunkIndex < 0 || chunkIndex >= getChunkCount()) {
            throw new IndexOutOfBoundsException("chunkIndex");
        }
        return chunkIndex < getChunkCount() - 1 ? chunkSize : lastChunkSize;
    }

    public long getMissingSize() {
        return getCompletedChunks()
                .mapToLong(this::getChunkSize)
                .sum();
    }

    public boolean contains(DataInfo other) {
        ensureCompatibility(other);

        // Clone and do a logical and operation
        BitSet clone = (BitSet) other.chunks.clone();
        clone.and(chunks);

        // If equal we contain the other completely
        return !clone.isEmpty() && clone.equals(other.chunks);
    }

    public IntStream getCompletedChunks() {
        return chunks.stream();
    }

    public int getCompletedChunkCount() {
        return chunks.cardinality();
    }

    public int getMissingChunkCount() {
        return getChunkCount() - getCompletedChunkCount();
    }

    public boolean isEmpty() {
        return chunks.isEmpty();
    }

    public boolean isCompleted() {
        return getCompletedChunkCount() == getChunkCount();
    }

    public boolean isChunkCompleted(int chunkIndex) {
        if (chunkIndex < 0 || chunkIndex >= getChunkCount()) {
            throw new IndexOutOfBoundsException("chunkIndex");
        }

        return chunks.get(chunkIndex);
    }

    public DataInfo setChunk(int chunkIndex, boolean value) {
        if (chunkIndex < 0 || chunkIndex >= getChunkCount()) {
            throw new IndexOutOfBoundsException("chunkIndex");
        }
        DataInfo copy = duplicate();
        copy.chunks.set(chunkIndex, value);
        return copy;
    }

    @Override
    public String toString() {
        return "DataInfo{" +
                "hash='" + hash + '\'' +
                ", size=" + size +
                ", chunkCount=" + chunkCount +
                ", chunks=" + chunks +
                '}';
    }
}
