package de.probst.ba.core.media.database;

import de.probst.ba.core.util.FunctionThatThrows;
import de.probst.ba.core.util.collections.Tuple2;
import de.probst.ba.core.util.io.IOUtil;
import de.probst.ba.core.util.io.LimitedReadableByteChannel;

import java.io.Flushable;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A data base manages data info and the content.
 * <p>
 * Created by chrisprobst on 13.08.14.
 */
public interface DataBase extends Flushable {

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
     * @param dataInfo
     * @return The channel or empty, if one of the specified chunks
     * is locked for writing.
     * @throws IOException If an exception occurs or one of the specified
     *                     chunks does not exist.
     */
    Optional<DataBaseReadChannel> lookup(DataInfo dataInfo) throws IOException;

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
     * <p>
     * The returned map is in order.
     *
     * @param dataInfo
     * @return The data info mapped to their channels or empty,
     * if one of the specified chunks is locked for writing.
     * @throws IOException If an exception occurs or one of the specified
     *                     chunks does not exist.
     */
    Optional<Map<DataInfo, DataBaseReadChannel>> lookupMany(List<DataInfo> dataInfo) throws IOException;

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
     * @return The data info mapped to its channel or empty,
     * if one of the specified chunks is locked for writing.
     * @throws IOException If an exception occurs or there is no data info
     *                     which fulfills the predicate.
     */
    Optional<Tuple2<DataInfo, DataBaseReadChannel>> findAny(Predicate<DataInfo> predicate) throws IOException;

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
     * @return The data info mapped to their channels or empty,
     * if one of the specified chunks is locked for writing.
     * @throws IOException If an exception occurs or there is no data info
     *                     which fulfills the predicate.
     */
    Optional<Map<DataInfo, DataBaseReadChannel>> findMany(Predicate<DataInfo> predicate) throws IOException;

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
    Optional<DataBaseReadChannel> findIncremental(Predicate<DataInfo> predicate) throws IOException;

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
     * <p>
     * The returned map is in order.
     *
     * @param dataInfo
     * @return The data info mapped to their channels or empty,
     * if one of the specified chunks cannot be locked for writing.
     * @throws IOException If an exception occurs or one of the specified
     *                     chunks already exists.
     */
    default Optional<Map<DataInfo, DataBaseWriteChannel>> insertMany(List<DataInfo> dataInfo) throws IOException {
        Objects.requireNonNull(dataInfo);

        Map<DataInfo, DataBaseWriteChannel> writeChannels = new LinkedHashMap<>();
        for (DataInfo insertDataInfo : dataInfo) {
            Optional<DataBaseWriteChannel> writeChannel;
            try {
                writeChannel = insert(insertDataInfo);
            } catch (IOException e) {
                for (Channel channel : writeChannels.values()) {
                    try {
                        channel.close();
                    } catch (IOException e1) {
                        e.addSuppressed(e1);
                    }
                }
                throw e;
            }
            if (!writeChannel.isPresent()) {
                IOException any = null;
                for (Channel channel : writeChannels.values()) {
                    try {
                        channel.close();
                    } catch (IOException e1) {
                        if (any == null) {
                            any = e1;
                        } else {
                            any.addSuppressed(e1);
                        }
                    }
                }
                if (any != null) {
                    throw any;
                }
                return Optional.empty();
            }

            writeChannels.put(insertDataInfo, writeChannel.get());
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
                return insertManyFromChannels(dataInfo, x -> readableByteChannel, false);
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

        Optional<Map<DataInfo, DataBaseWriteChannel>> writeChannels = null;
        try {
            writeChannels = insertMany(dataInfo);
            if (!writeChannels.isPresent()) {
                return false;
            } else {
                for (Map.Entry<DataInfo, DataBaseWriteChannel> entry : writeChannels.get().entrySet()) {
                    IOUtil.transfer(new LimitedReadableByteChannel(function.apply(entry.getKey()),
                                                                   entry.getKey().getSize(),
                                                                   closeReadableByteChannels), entry.getValue());
                }
            }
            return true;
        } catch (IOException e) {
            if (writeChannels != null) {
                for (Channel channel : writeChannels.get().values()) {
                    try {
                        channel.close();
                    } catch (IOException e1) {
                        e.addSuppressed(e1);
                    }
                }
            }
            throw e;
        }
    }
}
