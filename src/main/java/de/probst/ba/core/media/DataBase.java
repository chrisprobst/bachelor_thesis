package de.probst.ba.core.media;

import de.probst.ba.core.net.Transfer;
import de.probst.ba.core.net.TransferManager;
import io.netty.buffer.ByteBuf;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * A data base manages data info and the content.
 * <p>
 * Created by chrisprobst on 13.08.14.
 */
public interface DataBase extends Closeable {

    /**
     * @return A snapshot of all registered
     * data info in this data base.
     */
    Map<String, DataInfo> getDataInfo();

    /**
     * Add a new data info interest.
     * This means that you have to add
     * a data info first before processing
     * any buffers.
     * <p>
     * The data info must be empty.
     *
     * @param dataInfo
     * @return True if the data info was not already
     * added, otherwise false.
     */
    boolean addInterest(DataInfo dataInfo);

    /**
     * @param hash
     * @return The data info with the given hash.
     */
    DataInfo get(String hash);

    /**
     * Removes the data info.
     * <p>
     * If the data info does not exist
     * nothing happens.
     *
     * @param hash
     * @throws IOException
     */
    void remove(String hash) throws IOException;

    /**
     * Depending on the download flag this method fills the given
     * buffer or reads from it.
     * <p>
     * Additionally this method marks the chunk as complete.
     * <p>
     * If download is false than this simply means that the data base
     * could close the underlying resources used for reading the data.
     * <p>
     * If download is true than this means that the chunk is complete.
     * This will be reflected by the according data info.
     * This method also verify any hash sum checks. If a check fails
     * this chunk will not be marked as completed.
     *
     * @param hash
     * @param chunkIndex
     * @param offset
     * @param byteBuf
     * @param length
     * @param download
     * @throws IOException
     */
    void processBufferAndComplete(String hash,
                                  int chunkIndex,
                                  long offset,
                                  ByteBuf byteBuf,
                                  int length,
                                  boolean download) throws IOException;

    /**
     * Depending on the download flag this method fills the given
     * buffer or reads from it.
     *
     * @param hash
     * @param chunkIndex
     * @param offset
     * @param byteBuf
     * @param length
     * @param download
     * @throws IOException
     */
    void processBuffer(String hash,
                       int chunkIndex,
                       long offset,
                       ByteBuf byteBuf,
                       int length,
                       boolean download) throws IOException;

    default TransferManager createDownloadTransferManager(Object remotePeerId,
                                                          DataInfo dataInfo) {
        return createTransferManager(remotePeerId, dataInfo, true);
    }

    default TransferManager createUploadTransferManager(Object remotePeerId,
                                                        DataInfo dataInfo) {
        return createTransferManager(remotePeerId, dataInfo, false);
    }

    default TransferManager createTransferManager(Object remotePeerId,
                                                  DataInfo dataInfo,
                                                  boolean download) {

        Objects.requireNonNull(remotePeerId);
        Objects.requireNonNull(dataInfo);

        DataInfo existingDataInfo = get(dataInfo.getHash());
        if (existingDataInfo == null) {
            throw new IllegalArgumentException(
                    "Data info does not exist");
        }

        if (!download && !existingDataInfo.contains(dataInfo)) {
            throw new IllegalArgumentException(
                    "!download && !existingDataInfo.contains(dataInfo)");
        }

        return new TransferManager(this,
                new Transfer(remotePeerId, dataInfo, download));
    }
}
