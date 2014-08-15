package de.probst.ba.core.media;

import io.netty.buffer.ByteBuf;

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

    void processBufferAndComplete(String hash,
                                  int chunkIndex,
                                  long offset,
                                  ByteBuf byteBuf,
                                  int length,
                                  boolean download) throws IOException;

    void processBuffer(String hash,
                       int chunkIndex,
                       long offset,
                       ByteBuf byteBuf,
                       int length,
                       boolean download) throws IOException;
}
