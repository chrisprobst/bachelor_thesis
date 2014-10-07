package de.probst.ba.core.media.database;

import java.io.Flushable;
import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.util.Map;
import java.util.Optional;

/**
 * A data base manages data info and the content.
 * <p>
 * Created by chrisprobst on 13.08.14.
 */
public interface DataBase extends Flushable {

    /**
     * @return A snapshot of all existing
     * data info in this data base.
     */
    Map<String, DataInfo> getDataInfo();

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
     * The chunks affected by this query are locked for reading,
     * so that no modifications can happen, while reading
     * in parallel is allowed.
     *
     * @param dataInfo
     * @return The channel or empty, if one of the specified chunks
     * is locked for writing.
     * @throws IOException If an exception occurs or one of the specified
     *                     chunks does not exist.
     */
    Optional<ScatteringByteChannel> tryOpenReadChannel(DataInfo dataInfo) throws IOException;

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
    Optional<GatheringByteChannel> tryOpenWriteChannel(DataInfo dataInfo) throws IOException;
}
