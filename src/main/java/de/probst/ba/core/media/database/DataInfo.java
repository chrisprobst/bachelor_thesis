package de.probst.ba.core.media.database;

import de.probst.ba.core.util.collections.Tuple;
import de.probst.ba.core.util.collections.Tuple2;
import de.probst.ba.core.util.io.LimitedReadableByteChannel;

import java.io.EOFException;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
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

    public static final String DEFAULT_HASH_ALGORITHM = "SHA1";
    public static final int DEFAULT_HASH_BUFFER_SIZE = 65535;

    public static Tuple2<Long, Long> calculatePartitionSizes(long size, int partitions) {
        if (size <= 0) {
            throw new IllegalArgumentException("size <= 0");
        }

        if (partitions <= 0) {
            throw new IllegalArgumentException("partitions <= 0");
        }

        if (partitions > size) {
            throw new IllegalArgumentException("partitions > size");
        }

        long partitionSize = size / partitions;
        return Tuple.of(partitionSize, size - partitionSize * (partitions - 1));
    }

    public static List<DataInfo> fromPartitionedChannel(int partitions,
                                                        long size,
                                                        Optional<String> name,
                                                        Optional<Object> description,
                                                        int chunkCount,
                                                        ReadableByteChannel readableByteChannel)
            throws IOException, NoSuchAlgorithmException {

        // Calculate partition sizes
        Tuple2<Long, Long> partitionSizes = calculatePartitionSizes(size, partitions);
        long partitionSize = partitionSizes.first();
        long lastPartitionSize = partitionSizes.second();

        if (chunkCount > partitionSize) {
            throw new IllegalArgumentException("chunkCount > partitionSize");
        }

        // Add data info for each part
        List<DataInfo> dataInfo = new ArrayList<>(partitions);
        for (int id = 0; id < partitions; id++) {
            long actualPartitionSize = id < partitions - 1 ? partitionSize : lastPartitionSize;
            dataInfo.add(fromChannel(id,
                                     actualPartitionSize,
                                     name,
                                     description,
                                     chunkCount,
                                     new LimitedReadableByteChannel(readableByteChannel, actualPartitionSize, false)));
        }
        return dataInfo;
    }

    public static DataInfo fromChannel(long id,
                                       long size,
                                       Optional<String> name,
                                       Optional<Object> description,
                                       int chunkCount,
                                       ReadableByteChannel readableByteChannel)
            throws NoSuchAlgorithmException, IOException {

        // A little helper function
        Function<byte[], String> hexify = byteArray -> {
            Formatter formatter = new Formatter();
            for (byte b : byteArray) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        };

        // All chunk hashes are stored here
        List<String> chunkHashes = new ArrayList<>(chunkCount);

        // Init hash
        MessageDigest hashDigest = MessageDigest.getInstance(DEFAULT_HASH_ALGORITHM);

        // Setup vars
        ByteBuffer byteBuffer = ByteBuffer.allocate(DEFAULT_HASH_BUFFER_SIZE);

        // Calculate chunk sizes
        Tuple2<Long, Long> chunkSizes = calculatePartitionSizes(size, chunkCount);
        long chunkSize = chunkSizes.first();
        long lastChunkSize = chunkSizes.second();

        for (int i = 0; i < chunkCount; i++) {
            // Init chunk hash
            MessageDigest chunkHashDigest = MessageDigest.getInstance(DEFAULT_HASH_ALGORITHM);

            // Calc the actual chunk size
            long actualChunkSize = i < chunkCount - 1 ? chunkSize : lastChunkSize;

            // Read in the actual chunk
            long completed = 0;
            while (completed < actualChunkSize) {
                // Set buffer size
                byteBuffer.clear().limit((int) Math.min(byteBuffer.capacity(), actualChunkSize - completed));

                // Read the data into memory
                int read = readableByteChannel.read(byteBuffer);
                if (read < 0) {
                    throw new EOFException();
                }
                byteBuffer.flip();

                // Update the hashes
                hashDigest.update(byteBuffer);
                byteBuffer.rewind();
                chunkHashDigest.update(byteBuffer);

                completed += read;
            }

            // Hexify chunk hash
            chunkHashes.add(hexify.apply(chunkHashDigest.digest()));
        }

        // Hexify hash
        String hash = hexify.apply(hashDigest.digest());

        // Create a new data info
        return new DataInfo(id, size, name, description, hash, chunkHashes).full();
    }

    public static DataInfo generate(long id,
                                    long size,
                                    Optional<String> name,
                                    Optional<Object> description,
                                    String hash,
                                    int chunkCount,
                                    IntFunction<String> chunkCreator) {
        return new DataInfo(id,
                            size,
                            name,
                            description,
                            hash,
                            IntStream.range(0, chunkCount).mapToObj(chunkCreator).collect(Collectors.toList()));
    }

    // The id of this data info
    private final long id;

    // The total size
    private final long size;

    // Human readable name of this data
    private final String name;

    // Human readable description of this data
    private final Object description;

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

    // Tells whether or not this data info
    // came from a producer
    private final boolean fromProducer;

    public DataInfo(long id,
                    long size,
                    Optional<String> name,
                    Optional<Object> description,
                    String hash,
                    List<String> chunkHashes) {
        this(id, size, name, description, hash, chunkHashes, false);
    }

    public DataInfo(long id,
                    long size,
                    Optional<String> name,
                    Optional<Object> description,
                    String hash,
                    List<String> chunkHashes,
                    boolean fromProducer) {
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
        this.fromProducer = fromProducer;
        chunks = new BitSet(chunkHashes.size());

        // Calculate the chunk sizes
        // Calculate chunk sizes
        Tuple2<Long, Long> chunkSizes = calculatePartitionSizes(size, chunkHashes.size());
        chunkSize = chunkSizes.first();
        lastChunkSize = chunkSizes.second();
    }

    /**
     * @return True if it is from a producer, otherwise false.
     */
    public boolean isFromProducer() {
        return fromProducer;
    }

    /**
     * @param fromProducer
     * @return
     */
    public DataInfo fromProducer(boolean fromProducer) {
        DataInfo copy = new DataInfo(getId(),
                                     getSize(),
                                     getName(),
                                     getDescription(),
                                     getHash(),
                                     getChunkHashes(),
                                     fromProducer);
        copy.chunks.or(chunks);
        return copy;
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
    public DataInfo subtract(DataInfo other) {
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
    public Optional<Object> getDescription() {
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
     * Calculate which chunks are completed according to the given
     * size and return them as a stream.
     *
     * @param size
     * @return
     */
    public IntStream calculateCompletedChunksForSize(long size) {
        List<Integer> completedChunks = new ArrayList<>();
        for (int chunk : getCompletedChunks().toArray()) {
            if ((size -= getChunkSize(chunk)) >= 0) {
                completedChunks.add(chunk);
            }
        }
        return completedChunks.stream().mapToInt(Integer::intValue);
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
     * @param totalChunkIndex
     * @return The total offset according to the total chunk index.
     */
    public long getTotalOffset(int totalChunkIndex) {
        return IntStream.range(0, totalChunkIndex).mapToLong(this::getChunkSize).sum();
    }

    /**
     * @param totalChunkIndex
     * @return The relative offset according to total chunk index.
     */
    public long getRelativeOffset(int totalChunkIndex) {
        if (totalChunkIndex < 0) {
            throw new IllegalArgumentException("totalChunkIndex < 0");
        } else if (totalChunkIndex >= getChunkCount()) {
            throw new IllegalArgumentException("totalChunkIndex >= getChunkCount()");
        } else if (!getCompletedChunks().filter(chunkIndex -> chunkIndex == totalChunkIndex).findFirst().isPresent()) {
            throw new IllegalArgumentException("!getCompletedChunks().filter(chunkIndex -> " +
                                               "chunkIndex == totalChunkIndex).findFirst().isPresent()");
        }
        return getCompletedChunks().filter(chunkIndex -> chunkIndex < totalChunkIndex)
                                   .mapToLong(this::getChunkSize)
                                   .sum();
    }

    /**
     * @param offset
     * @return The chunk index according to the offset.
     */
    public int getTotalChunkIndex(long offset, boolean totalOffset) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset < 0");
        }

        if (totalOffset) {
            if (offset >= getSize()) {
                throw new IllegalArgumentException("offset >= getSize()");
            }
            int chunkIndex;
            for (chunkIndex = 0; chunkIndex < getChunkCount(); chunkIndex++) {
                if ((offset -= getChunkSize(chunkIndex)) < 0) {
                    break;
                }
            }
            return chunkIndex;
        } else {
            if (offset >= getCompletedSize()) {
                throw new IllegalArgumentException("offset >= getCompletedSize()");
            }
            int[] completedChunks = getCompletedChunks().toArray();
            int chunkIndex = completedChunks[0];
            for (int i = 0; i < completedChunks.length; i++, chunkIndex = completedChunks[i]) {
                if ((offset -= getChunkSize(chunkIndex)) < 0) {
                    break;
                }
            }
            return chunkIndex;
        }
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
     * If one of the data info is empty, this method
     * returns false.
     *
     * @param other
     * @return
     */
    public boolean contains(DataInfo other) {
        return !isEmpty() && !other.isEmpty() && other.subtract(this).getCompletedChunkCount() == 0;
    }

    /**
     * Checks whether or not this data info
     * overlaps with the other data info.
     *
     * @param other
     * @return
     */
    public boolean overlaps(DataInfo other) {
        return intersection(other).getCompletedChunkCount() > 0;
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
               ", name='" + getName() + '\'' +
               ", description=" + getDescription() +
               ", hash='" + hash + '\'' +
               ", chunkHashes=" + chunkHashes +
               ", chunkSize=" + chunkSize +
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
