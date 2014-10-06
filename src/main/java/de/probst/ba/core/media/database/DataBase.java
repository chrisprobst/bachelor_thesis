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
     * Queries the data base for the given data.
     * <p>
     * The returned channel is a cumulative channel
     * of all chunks specified in the given data info.
     * <p>
     * Only existing data and completed chunks are allowed
     * to be part of the query.
     * <p>
     * The chunks affected by this query are locked for inserting,
     * while querying still works.
     *
     * @param dataInfo
     * @return The channel or empty, if one of the specified chunks
     * is locked for querying.
     * @throws IOException If an exception occurs or one of the specified
     *                     chunks does not exist.
     */
    Optional<ScatteringByteChannel> tryQuery(DataInfo dataInfo) throws IOException;

    /**
     * Inserts the data into the data base.
     * <p>
     * The returned channel is a cumulative channel
     * of all chunks specified in the given data info.
     * <p>
     * Only uncompleted chunks are allowed to be part of the query.
     * <p>
     * The chunks affected by this query are locked for inserting
     * and querying.
     *
     * @param dataInfo
     * @return The channel or empty, if one of the specified chunks
     * is locked for inserting.
     * @throws IOException If an exception occurs or one of the specified
     *                     chunks already exists.
     */
    Optional<GatheringByteChannel> tryInsert(DataInfo dataInfo) throws IOException;
}
