package de.probst.ba.core.media.database;

import de.probst.ba.core.media.database.databases.CumulativeDataBaseReadChannel;
import de.probst.ba.core.util.FunctionThatThrows;
import de.probst.ba.core.util.io.IOUtil;
import de.probst.ba.core.util.io.LimitedReadableByteChannel;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A data base manages data info and the content.
 * <p>
 * Created by chrisprobst on 13.08.14.
 */
public interface DataBase extends Closeable {

    /**
     * @return A snapshot of all existing
     * non-empty data info in this data base.
     */
    Map<String, DataInfo> getDataInfo();

    /**
     * @return A snapshot of all existing
     * non-empty data info in this data base plus data
     * info locked by write channels which eventually be available soon.
     */
    Map<String, DataInfo> getEstimatedDataInfo();

    /**
     * @param hash
     * @return The data info with the given hash.
     */
    DataInfo get(String hash);

    /**
     * Tries to open a database channel for reading by
     * searching for the exact same data info.
     * <p>
     * The returned channel is a cumulative channel
     * of all chunks specified in the given data info.
     * <p>
     * Only existing data and completed chunks are allowed
     * to be part of the query.
     * <p>
     * The chunks affected by this lookup are locked for reading,
     * so that no modifications can happen, while reading
     * in parallel is allowed.
     *
     * @param lookupDataInfo
     * @return The channel or empty, if one of the specified chunks
     * is locked for writing.
     * @throws IOException If an exception occurs or one of the specified
     *                     chunks does not exist.
     */
    Optional<DataBaseReadChannel> lookup(DataInfo lookupDataInfo) throws IOException;


    /**
     * Tries to open a database channel for writing.
     * <p>
     * The returned channel is a cumulative channel
     * of all chunks specified in the given data info.
     * <p>
     * Only uncompleted chunks are allowed to be part of the query.
     * <p>
     * The chunks affected by this query are exclusively locked for writing,
     * so no reads or writes can occur in parallel.
     *
     * @param dataInfo
     * @return The channel or empty, if one of the specified chunks
     * cannot be locked for writing.
     * @throws IOException If an exception occurs or one of the specified
     *                     chunks already exists.
     */
    Optional<DataBaseWriteChannel> insert(DataInfo dataInfo) throws IOException;

    /**
     * Tries to open database channels for reading by
     * searching for the exact same data info.
     * <p>
     * The returned channels are cumulative channels
     * of all chunks specified in the given data info.
     * <p>
     * Only existing data and completed chunks are allowed
     * to be part of the query.
     * <p>
     * The chunks affected by this lookup are locked for reading,
     * so that no modifications can happen, while reading
     * in parallel is allowed.
     *
     * @param lookupDataInfo
     * @return The channels or empty,
     * if one of the specified chunks is locked for writing.
     * @throws IOException If an exception occurs or one of the specified
     *                     chunks does not exist.
     */
    default Optional<List<DataBaseReadChannel>> lookupMany(List<DataInfo> lookupDataInfo) throws IOException {
        Objects.requireNonNull(lookupDataInfo);

        List<DataBaseReadChannel> founds = new ArrayList<>(lookupDataInfo.size());
        for (DataInfo dataInfo : lookupDataInfo) {
            Optional<DataBaseReadChannel> readChannel;

            try {
                readChannel = lookup(dataInfo);
            } catch (Exception e) {
                throw IOUtil.closeAllAndGetException(founds, e);
            }

            if (readChannel.isPresent()) {
                founds.add(readChannel.get());
            } else {
                IOUtil.closeAllAndThrow(founds);
                return Optional.empty();
            }
        }

        return Optional.of(founds);
    }

    /**
     * Tries to open a database channel for reading by
     * searching for any data info which fulfills the predicate.
     * <p>
     * The returned channel is a cumulative channel
     * of all chunks in the given data info.
     * <p>
     * The chunks affected by this search are locked for reading,
     * so that no modifications can happen, while reading
     * in parallel is allowed.
     *
     * @param predicate
     * @return The channel or empty,
     * if one of the specified chunks is locked for writing.
     * @throws IOException If an exception occurs or there is no data info
     *                     which fulfills the predicate.
     */
    default Optional<DataBaseReadChannel> findAny(Predicate<DataInfo> predicate) throws IOException {
        Objects.requireNonNull(predicate);

        Optional<DataInfo> foundDataInfo = getDataInfo().values().stream().filter(predicate).findAny();
        if (!foundDataInfo.isPresent()) {
            throw new DataLookupException();
        } else {
            return lookup(foundDataInfo.get());
        }
    }

    /**
     * Tries to open database channels for reading by
     * searching for all data info which fulfills the predicate.
     * <p>
     * The returned channels are cumulative channels
     * of all chunks in the given data info.
     * <p>
     * The chunks affected by this search are locked for reading,
     * so that no modifications can happen, while reading
     * in parallel is allowed.
     *
     * @param predicate
     * @return The channels or empty,
     * if one of the specified chunks is locked for writing.
     * @throws IOException If an exception occurs or there is no data info
     *                     which fulfills the predicate.
     */
    default Optional<List<DataBaseReadChannel>> findMany(Predicate<DataInfo> predicate)
            throws IOException {
        Objects.requireNonNull(predicate);

        List<DataInfo> foundDataInfo = getDataInfo().values().stream().filter(predicate).collect(Collectors.toList());
        if (foundDataInfo.isEmpty()) {
            throw new DataLookupException();
        } else {
            return lookupMany(foundDataInfo);
        }
    }


    /**
     * Tries to open a cumulative database channel for reading by
     * searching for all data info which fulfills the predicate and
     * sorts them according to their id. The first coherent data info
     * are then used to open the channel.
     * <p>
     * The returned channel is a cumulative channel
     * of all chunks in the given data info.
     * <p>
     * The chunks affected by this search are locked for reading,
     * so that no modifications can happen, while reading
     * in parallel is allowed.
     *
     * @param predicate
     * @return The channel or empty, if one of the
     * specified chunks is locked for writing.
     * @throws IOException If an exception occurs or there is no data info
     *                     which fulfills the predicate.
     */
    default Optional<DataBaseReadChannel> findIncremental(Predicate<DataInfo> predicate)
            throws IOException {
        Objects.requireNonNull(predicate);

        List<DataInfo> foundDataInfo = getDataInfo().values()
                                                    .stream()
                                                    .filter(predicate)
                                                    .sorted(Comparator.comparing(DataInfo::getId))
                                                    .collect(Collectors.toList());
        if (foundDataInfo.isEmpty()) {
            throw new DataLookupException();
        }

        // Collect all in-order data info
        List<DataInfo> inOrderDataInfo = new ArrayList<>();
        OptionalLong id = OptionalLong.empty();
        for (DataInfo dataInfo : foundDataInfo) {
            if (!id.isPresent() || id.getAsLong() + 1 == dataInfo.getId()) {
                id = OptionalLong.of(dataInfo.getId());
                inOrderDataInfo.add(dataInfo);
            } else {
                break;
            }
        }

        return lookupMany(inOrderDataInfo).map(dataInfo -> new CumulativeDataBaseReadChannel(this, dataInfo));
    }

    /**
     * Tries to open a database channel for writing and
     * copies as much bytes from the readable byte channel
     * to the writable byte channel as there are in
     * the given data info.
     * <p>
     * Only uncompleted chunks are allowed to be part of the query.
     * <p>
     * The chunks affected by this query are exclusively locked for writing,
     * so no reads or writes can occur in parallel.
     *
     * @param dataInfo
     * @param readableByteChannel
     * @param closeReadableByteChannel
     * @return True or false, if one of the specified chunks
     * cannot be locked for writing.
     * @throws IOException If an exception occurs or one of the specified
     *                     chunks already exists.
     */
    default boolean insertFromChannel(DataInfo dataInfo,
                                      ReadableByteChannel readableByteChannel,
                                      boolean closeReadableByteChannel) throws IOException {
        Objects.requireNonNull(dataInfo);
        Objects.requireNonNull(readableByteChannel);

        Optional<DataBaseWriteChannel> writeChannel = insert(dataInfo);
        if (writeChannel.isPresent()) {
            IOUtil.transfer(new LimitedReadableByteChannel(readableByteChannel,
                                                           dataInfo.getSize(),
                                                           closeReadableByteChannel), writeChannel.get());
            return true;
        } else {
            return false;
        }
    }

    /**
     * Tries to open as many database channels as
     * there are data info for writing.
     * <p>
     * The returned channels are cumulative channels
     * of all chunks specified in the given data info.
     * <p>
     * Only uncompleted chunks are allowed to be part of the query.
     * <p>
     * The chunks affected by this query are exclusively locked for writing,
     * so no reads or writes can occur in parallel.
     *
     * @param dataInfo
     * @return The channels or empty,
     * if one of the specified chunks cannot be locked for writing.
     * @throws IOException If an exception occurs or one of the specified
     *                     chunks already exists.
     */
    default Optional<List<DataBaseWriteChannel>> insertMany(List<DataInfo> dataInfo) throws IOException {
        Objects.requireNonNull(dataInfo);

        List<DataBaseWriteChannel> writeChannels = new ArrayList<>(dataInfo.size());
        for (DataInfo insertDataInfo : dataInfo) {
            Optional<DataBaseWriteChannel> writeChannel;
            try {
                writeChannel = insert(insertDataInfo);
            } catch (IOException e) {
                throw IOUtil.closeAllAndGetException(writeChannels, e);
            }

            if (!writeChannel.isPresent()) {
                IOUtil.closeAllAndThrow(writeChannels);
                return Optional.empty();
            }

            writeChannels.add(writeChannel.get());
        }
        return Optional.of(writeChannels);
    }

    /**
     * Tries to open database channels for writing and
     * copies as much bytes from the readable byte channel
     * to the writable byte channels as there are in
     * the given data info.
     * <p>
     * Only uncompleted chunks are allowed to be part of the query.
     * <p>
     * The chunks affected by this query are exclusively locked for writing,
     * so no reads or writes can occur in parallel.
     *
     * @param dataInfo
     * @param readableByteChannel
     * @param closeReadableByteChannel
     * @return True or false, if one of the specified chunks
     * cannot be locked for writing.
     * @throws IOException If an exception occurs or one of the specified
     *                     chunks already exists.
     */
    default boolean insertManyFromChannel(List<DataInfo> dataInfo,
                                          ReadableByteChannel readableByteChannel,
                                          boolean closeReadableByteChannel) throws IOException {
        Objects.requireNonNull(dataInfo);
        Objects.requireNonNull(readableByteChannel);

        if (closeReadableByteChannel) {
            try (ReadableByteChannel ref = readableByteChannel) {
                return insertManyFromChannels(dataInfo, x -> ref, false);
            }
        } else {
            return insertManyFromChannels(dataInfo, x -> readableByteChannel, false);
        }
    }

    /**
     * Tries to open database channels for writing and
     * copies as much bytes from all opened readable byte channels
     * to the writable byte channels as there are in
     * the given data info.
     * <p>
     * Only uncompleted chunks are allowed to be part of the query.
     * <p>
     * The chunks affected by this query are exclusively locked for writing,
     * so no reads or writes can occur in parallel.
     *
     * @param dataInfo
     * @param function
     * @param closeReadableByteChannels
     * @return True or false, if one of the specified chunks
     * cannot be locked for writing.
     * @throws IOException If an exception occurs or one of the specified
     *                     chunks already exists.
     */
    default boolean insertManyFromChannels(List<DataInfo> dataInfo,
                                           FunctionThatThrows<DataInfo, ReadableByteChannel, IOException> function,
                                           boolean closeReadableByteChannels) throws IOException {
        Objects.requireNonNull(dataInfo);
        Objects.requireNonNull(function);

        Optional<List<DataBaseWriteChannel>> writeChannels = insertMany(dataInfo);
        if (!writeChannels.isPresent()) {
            return false;
        }

        try {
            for (DataBaseWriteChannel writeChannel : writeChannels.get()) {
                IOUtil.transfer(new LimitedReadableByteChannel(function.apply(writeChannel.getDataInfo()),
                                                               writeChannel.getDataInfo().getSize(),
                                                               closeReadableByteChannels),
                                writeChannel);
            }
            return true;
        } catch (IOException e) {
            throw IOUtil.closeAllAndGetException(writeChannels.get(), e);
        }
    }
}
