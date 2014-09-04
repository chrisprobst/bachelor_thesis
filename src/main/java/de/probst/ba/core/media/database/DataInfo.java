package de.probst.ba.core.media.database;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class can be used to describe arbitrary data
 * in terms of size, consistency and chunks.
 * <p>
 * Created by chrisprobst on 03.08.14.
 */
public final class DataInfo implements Serializable {

    // The id of this data info
    private final long id;

    // The total size
    private final long size;

    // Human readable name of this data
    private final String name;

    // Human readable description of this data
    private final String description;

    // The unique hash
    private final String hash;

    // The unique chunk hashes
    private final List<String> chunkHashes;

    // The usual chunk size
    // (Calculated with size and chunkCount)
    private final long chunkSize;

    // The size of the last chunk
    // (Premature optimization, i know)
    private final long lastChunkSize;

    // Here we store whether or not
    // a chunk is completed
    private final BitSet chunks;

    /**
     * Initialize a data info with a chunk creator.
     *
     * @param id
     * @param size
     * @param name
     * @param description
     * @param hash
     * @param chunkCount
     * @param chunkCreator
     */
    public DataInfo(long id,
                    long size,
                    Optional<String> name,
                    Optional<String> description,
                    String hash,
                    int chunkCount,
                    IntFunction<String> chunkCreator) {
        this(id,
             size,
             name,
             description,
             hash,
             IntStream.range(0, chunkCount).mapToObj(chunkCreator).collect(Collectors.toList()));
    }

    public DataInfo(long id,
                    long size,
                    Optional<String> name,
                    Optional<String> description,
                    String hash,
                    List<String> chunkHashes) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(description);
        Objects.requireNonNull(hash);
        Objects.requireNonNull(chunkHashes);
        chunkHashes.stream().forEach(Objects::requireNonNull);

        if (size < 0) {
            throw new IllegalArgumentException("size < 0");
        }

        if (chunkHashes.size() == 0) {
            throw new IllegalArgumentException("chunkHashes.size() == 0");
        }

        if (chunkHashes.size() > size) {
            throw new IllegalArgumentException("chunkHashes.size() > size");
        }

        this.id = id;
        this.size = size;
        this.name = name.orElse(null);
        this.description = description.orElse(null);
        this.hash = hash;
        this.chunkHashes = Collections.unmodifiableList(new ArrayList<>(chunkHashes));
        chunks = new BitSet(chunkHashes.size());

        // Calculate the usual chunk size
        chunkSize = size / chunkHashes.size();

        // Calculate the last chunk size
        long remaining = size % chunkHashes.size();
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
        return new DataInfo(getId(), getSize(), getName(), getDescription(), getHash(), getChunkHashes());
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
            throw new IndexOutOfBoundsException("chunkIndex < 0 || chunkIndex >= getChunkCount()");
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
     * Creates a copy and sets the given chunks
     * to true.
     *
     * @param indexStream
     * @return
     */
    public DataInfo withChunks(IntStream indexStream) {
        Objects.requireNonNull(indexStream);
        DataInfo copy = duplicate();
        indexStream.forEach(copy.chunks::set);
        return copy;
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
     * Creates an empty copy but keeps a random chunk.
     *
     * @return
     */
    public DataInfo withOneCompletedChunk() {
        if (isEmpty()) {
            throw new IllegalStateException("isEmpty()");
        }

        int[] completedChunks = getCompletedChunks().toArray();
        int chunkIndex = (int) Math.round(Math.random() * (completedChunks.length - 1));

        return empty().withChunk(completedChunks[chunkIndex]);
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
        return other.getSize() == getSize() && other.getHash().equals(getHash()) && other.getChunkHashes()
                                                                                         .equals(getChunkHashes());
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
     * Creates a new data info which has all completed
     * chunks of this minus the other data info.
     *
     * @param other
     * @return
     */
    public DataInfo substract(DataInfo other) {
        ensureCompatibility(other);

        DataInfo dataInfo = duplicate();
        dataInfo.chunks.andNot(other.chunks);
        return dataInfo;
    }

    /**
     * Creates a new data info which has all completed
     * chunks of this which are also completed in
     * the other data info.
     *
     * @param other
     * @return
     */
    public DataInfo intersection(DataInfo other) {
        ensureCompatibility(other);

        DataInfo dataInfo = duplicate();
        dataInfo.chunks.and(other.chunks);
        return dataInfo;
    }

    /**
     * @return The id.
     */
    public long getId() {
        return id;
    }

    /**
     * @return The size.
     */
    public long getSize() {
        return size;
    }

    /**
     * @return The name.
     */
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * @return The description.
     */
    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    /**
     * @return The hash.
     */
    public String getHash() {
        return hash;
    }

    /**
     * @return The chunk hashes.
     */
    public List<String> getChunkHashes() {
        return chunkHashes;
    }

    /**
     * @return The chunk count.
     */
    public int getChunkCount() {
        return chunkHashes.size();
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
            throw new IndexOutOfBoundsException("chunkIndex < 0 || chunkIndex >= getChunkCount()");
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
        return getCompletedChunks().mapToLong(this::getChunkSize).sum();
    }

    /**
     * @param chunkIndex
     * @return The offset according to the chunk index.
     */
    public long getOffset(int chunkIndex) {
        return IntStream.range(0, chunkIndex).mapToLong(this::getChunkSize).sum();
    }

    /**
     * @return The percentage of this data info.
     */
    public double getPercentage() {
        return getCompletedChunkCount() / (double) getChunkCount();
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
            throw new IndexOutOfBoundsException("chunkIndex < 0 || chunkIndex >= getChunkCount()");
        }

        return chunks.get(chunkIndex);
    }

    @Override
    public String toString() {
        return "DataInfo{" +
                "id=" + id +
                ", size=" + size +
                ", name=" + getName() +
                ", description=" + getDescription() +
                ", hash='" + hash + '\'' +
                ", chunkHashes=" + chunkHashes +
                ", chunks=" + chunks +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataInfo dataInfo = (DataInfo) o;

        if (id != dataInfo.id) return false;
        if (size != dataInfo.size) return false;
        if (!chunkHashes.equals(dataInfo.chunkHashes)) return false;
        if (!chunks.equals(dataInfo.chunks)) return false;
        if (description != null ? !description.equals(dataInfo.description) : dataInfo.description != null)
            return false;
        if (!hash.equals(dataInfo.hash)) return false;
        if (name != null ? !name.equals(dataInfo.name) : dataInfo.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + hash.hashCode();
        result = 31 * result + chunkHashes.hashCode();
        result = 31 * result + chunks.hashCode();
        return result;
    }
}
