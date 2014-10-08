package de.probst.ba.core.media.database;

import de.probst.ba.core.util.collections.Tuple2;
import de.probst.ba.core.util.io.IOUtil;
import de.probst.ba.core.util.io.LimitedReadableByteChannel;

import java.io.Flushable;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
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
     * Tries to open a database channel for reading.
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

    Optional<Tuple2<DataInfo, DataBaseReadChannel>> findAny(Predicate<DataInfo> predicate) throws IOException;

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

    default boolean insertFromChannel(DataInfo dataInfo, ReadableByteChannel readableByteChannel)
            throws IOException {
        Objects.requireNonNull(dataInfo);
        Objects.requireNonNull(readableByteChannel);
        Optional<DataBaseWriteChannel> writeChannel = insert(dataInfo);
        if (writeChannel.isPresent()) {
            IOUtil.transfer(readableByteChannel, writeChannel.get());
            return true;
        } else {
            return false;
        }
    }

    default Optional<Map<DataInfo, DataBaseWriteChannel>> insertMany(Collection<DataInfo> dataInfo) throws IOException {
        Objects.requireNonNull(dataInfo);
        Map<DataInfo, DataBaseWriteChannel> writeChannels = new HashMap<>();
        for (DataInfo insertDataInfo : dataInfo) {
            Optional<DataBaseWriteChannel> writeChannel;
            try {
                writeChannel = insert(insertDataInfo);
            } catch (IOException e) {
                // Close remaining channels
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
                // Close remaining channels
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

    default boolean insertManyFromPartitionedChannel(Collection<DataInfo> dataInfo,
                                                     ReadableByteChannel readableByteChannel) throws IOException {
        return insertManyFromChannel(dataInfo,
                                     insertDataInfo -> new LimitedReadableByteChannel(readableByteChannel,
                                                                                      insertDataInfo.getSize(),
                                                                                      false));
    }

    default boolean insertManyFromChannel(Collection<DataInfo> dataInfo,
                                          Function<DataInfo, ReadableByteChannel> readableByteChannelFunction)
            throws IOException {
        Objects.requireNonNull(dataInfo);
        Objects.requireNonNull(readableByteChannelFunction);

        Optional<Map<DataInfo, DataBaseWriteChannel>> writeChannels = null;
        try {
            writeChannels = insertMany(dataInfo);
            if (!writeChannels.isPresent()) {
                return false;
            } else {
                for (Map.Entry<DataInfo, DataBaseWriteChannel> entry : writeChannels.get().entrySet()) {
                    IOUtil.transfer(readableByteChannelFunction.apply(entry.getKey()), entry.getValue());
                }
            }
            return true;
        } catch (IOException e) {
            if (writeChannels != null) {
                // Close remaining channels
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
