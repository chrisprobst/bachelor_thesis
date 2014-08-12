package de.probst.ba.core.logic;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Created by chrisprobst on 03.08.14.
 */
public final class DataInfo implements Serializable {

    // The unique hash
    private final String hash;

    // The total size
    private final long size;

    // The number of chunks
    private final int chunkCount;

    // The usual chunk size
    // (Calculated with size and chunkCount)
    private final long chunkSize;

    // The size of the last chunk
    // (Premature optimization, i know)
    private final long lastChunkSize;

    // Here we store whether or not
    // a chunk is completed
    private final BitSet chunks;

    public DataInfo(String hash, long size, int chunkCount) {
        Objects.requireNonNull(hash);

        if (size < 0) {
            throw new IllegalArgumentException("size < 0");
        }

        if (chunkCount < 1) {
            throw new IllegalArgumentException("chunkCount < 1");
        }

        if (chunkCount > size) {
            throw new IllegalArgumentException("chunkCount > size");
        }

        this.hash = hash;
        this.size = size;
        this.chunkCount = chunkCount;
        chunks = new BitSet(chunkCount);

        // Calculate the usual chunk size
        chunkSize = size / chunkCount;

        // Calculate the last chunk size
        long remaining = size % chunkCount;
        lastChunkSize = remaining > 0 ? remaining : chunkSize;
    }

    /**
     * Creates a copy and flips the
     * completion status of all chunks.
     *
     * @return
     */
    public DataInfo flip() {
        DataInfo dataInfo = duplicate();
        dataInfo.chunks.flip(0, dataInfo.getChunkCount());
        return dataInfo;
    }

    /**
     * Creates a copy of this data info.
     *
     * @return
     */
    public DataInfo duplicate() {
        DataInfo dataInfo = empty();
        dataInfo.chunks.or(chunks);
        return dataInfo;
    }

    /**
     * Creates a copy and sets
     * all chunks to completed.
     *
     * @return
     */
    public DataInfo full() {
        DataInfo dataInfo = empty();
        dataInfo.chunks.set(0, dataInfo.getChunkCount(), true);
        return dataInfo;
    }

    /**
     * Creates an empty copy where
     * no chunk is completed.
     *
     * @return
     */
    public DataInfo empty() {
        return new DataInfo(
                getHash(),
                getSize(),
                getChunkCount());
    }

    /**
     * Creates a copy and sets the given chunk
     * to the given value.
     *
     * @param chunkIndex
     * @param value
     * @return
     */
    public DataInfo whereChunk(int chunkIndex, boolean value) {
        if (chunkIndex < 0 || chunkIndex >= getChunkCount()) {
            throw new IndexOutOfBoundsException(
                    "chunkIndex < 0 || chunkIndex >= getChunkCount()");
        }
        DataInfo copy = duplicate();
        copy.chunks.set(chunkIndex, value);
        return copy;
    }

    /**
     * Creates a copy and sets the given chunk
     * to true.
     *
     * @param chunkIndex
     * @return
     */
    public DataInfo withChunk(int chunkIndex) {
        return whereChunk(chunkIndex, true);
    }

    /**
     * Creates a copy and sets the given chunk
     * to false.
     *
     * @param chunkIndex
     * @return
     */
    public DataInfo withoutChunk(int chunkIndex) {
        return whereChunk(chunkIndex, false);
    }

    /**
     * @return A randomized copy.
     */
    public DataInfo randomize() {
        DataInfo dataInfo = empty();
        for (int i = 0; i < dataInfo.getChunkCount(); i++) {
            dataInfo.chunks.set(i, Math.random() >= 0.5);
        }
        return dataInfo;
    }

    /**
     * Checks whether or the other data info is
     * compatible with this compatible.
     *
     * @param other
     * @return
     */
    public boolean isCompatibleWith(DataInfo other) {
        Objects.requireNonNull(other);
        return other.getHash().equals(getHash()) &&
                other.getSize() == getSize() &&
                other.getChunkCount() == getChunkCount();
    }

    /**
     * Checks whether or not the other data info is
     * compatible and throws a runtime exception if
     * not.
     *
     * @param other
     */
    public void ensureCompatibility(DataInfo other) {
        if (!isCompatibleWith(other)) {
            throw new IllegalArgumentException("dataInfo not compatible");
        }
    }

    /**
     * Creates a new data info which has all completed
     * chunks of this and the other data info.
     *
     * @param other
     * @return
     */
    public DataInfo union(DataInfo other) {
        ensureCompatibility(other);

        DataInfo dataInfo = duplicate();
        dataInfo.chunks.or(other.chunks);
        return dataInfo;
    }

    /**
     * @return The hash.
     */
    public String getHash() {
        return hash;
    }

    /**
     * @return The chunk count.
     */
    public int getChunkCount() {
        return chunkCount;
    }

    /**
     * @return The size.
     */
    public long getSize() {
        return size;
    }

    /**
     * Calculates the chunk size using
     * the given chunk index.
     *
     * @param chunkIndex
     * @return
     */
    public long getChunkSize(int chunkIndex) {
        if (chunkIndex < 0 || chunkIndex >= getChunkCount()) {
            throw new IndexOutOfBoundsException(
                    "chunkIndex < 0 || chunkIndex >= getChunkCount()");
        }
        return chunkIndex < getChunkCount() - 1 ? chunkSize : lastChunkSize;
    }

    /**
     * @return A stream of indices which point to completed chunks.
     */
    public IntStream getCompletedChunks() {
        return chunks.stream();
    }

    /**
     * @return The missing size.
     */
    public long getMissingSize() {
        return getSize() - getCompletedSize();
    }

    /**
     * @return The completed size.
     */
    public long getCompletedSize() {
        return getCompletedChunks()
                .mapToLong(this::getChunkSize)
                .sum();
    }

    /**
     * Checks whether or not this data info
     * contains the other data info.
     * <p>
     * Two empty data info does not contain
     * each other.
     *
     * @param other
     * @return
     */
    public boolean contains(DataInfo other) {
        ensureCompatibility(other);

        // Clone and do a logical and operation
        BitSet clone = (BitSet) other.chunks.clone();
        clone.and(chunks);

        // If equal we contain the other completely
        return !clone.isEmpty() && clone.equals(other.chunks);
    }

    /**
     * @return The number of completed chunks.
     */
    public int getCompletedChunkCount() {
        return chunks.cardinality();
    }

    /**
     * @return The number of missing chunks.
     */
    public int getMissingChunkCount() {
        return getChunkCount() - getCompletedChunkCount();
    }

    /**
     * @return True if this data info has no completed chunks,
     * otherwise false.
     */
    public boolean isEmpty() {
        return chunks.isEmpty();
    }

    /**
     * @return True if this data info has no missing chunks,
     * otherwise false.
     */
    public boolean isCompleted() {
        return getMissingChunkCount() == 0;
    }

    /**
     * Checks whether or not the given chunk is completed.
     *
     * @param chunkIndex
     * @return
     */
    public boolean isChunkCompleted(int chunkIndex) {
        if (chunkIndex < 0 || chunkIndex >= getChunkCount()) {
            throw new IndexOutOfBoundsException(
                    "chunkIndex < 0 || chunkIndex >= getChunkCount()");
        }

        return chunks.get(chunkIndex);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataInfo dataInfo = (DataInfo) o;

        if (chunkCount != dataInfo.chunkCount) return false;
        if (size != dataInfo.size) return false;
        if (!chunks.equals(dataInfo.chunks)) return false;
        if (!hash.equals(dataInfo.hash)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = hash.hashCode();
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + chunkCount;
        result = 31 * result + chunks.hashCode();
        return result;
    }

    /**
     * Create a new transfer from this data info.
     *
     * @param remotePeerId
     * @return
     */
    public Transfer createTransfer(Object remotePeerId) {
        return new Transfer(remotePeerId, this);
    }
}
