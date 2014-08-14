package de.probst.ba.core.media;

import java.io.IOException;
import java.util.Map;

/**
 * A data base manages data info and the content.
 * <p>
 * You can specify the input and output chunk type.
 * <p>
 * While you can use byte[] or something like that
 * you can also use InputStream or any other object you like.
 * <p>
 * Created by chrisprobst on 13.08.14.
 */
public interface DataBase {

    /**
     * @return A snapshot of all registered
     * data info in this data base.
     */
    Map<String, DataInfo> getDataInfo();

    /**
     * Add a new data info.
     * <p>
     * The data info must be empty.
     *
     * @param dataInfo
     * @return
     */
    boolean add(DataInfo dataInfo);

    /**
     * Delete the data info and the data.
     *
     * @param hash
     */
    void delete(String hash);

    /**
     * This method stores a specific part of the
     * data and marks the chunk as completed.
     *
     * @param hash
     * @param chunkIndex
     * @param offset
     * @param length
     * @param buffer
     * @throws IOException
     */
    void storeBufferAndComplete(String hash,
                                int chunkIndex,
                                int offset,
                                int length,
                                byte[] buffer) throws IOException;

    /**
     * This method stores a specific part of the
     * data.
     *
     * @param hash
     * @param chunkIndex
     * @param offset
     * @param length
     * @param buffer
     * @throws IOException
     */
    void storeBuffer(String hash,
                     int chunkIndex,
                     int offset,
                     int length,
                     byte[] buffer) throws IOException;

    /**
     * This method loads a specific part of
     * the data into memory and returns it.
     *
     * @param hash
     * @param chunkIndex
     * @param offset
     * @param length
     * @return
     * @throws IOException
     */
    byte[] loadBuffer(String hash,
                      int chunkIndex,
                      int offset,
                      int length) throws IOException;
}
