package de.probst.ba.core.media.database;

import de.probst.ba.core.media.transfer.Transfer;
import de.probst.ba.core.media.transfer.TransferManager;
import de.probst.ba.core.util.collections.Tuple2;
import io.netty.buffer.ByteBuf;

import java.io.Flushable;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A data base manages data info and the content.
 * <p>
 * Created by chrisprobst on 13.08.14.
 */
public interface DataBase extends Flushable {

    /**
     * Flushes all changes to the file system.
     *
     * @throws IOException
     */
    void flush() throws IOException;

    /**
     * Add the given consumer to a list which gets
     * notified when the data base completes a new chunk.
     *
     * @param consumer
     * @return
     */
    Tuple2<Long, Map<String, DataInfo>> subscribe(Consumer<Map<String, DataInfo>> consumer);

    /**
     * Cancels a subscription.
     *
     * @param token
     * @return
     */
    boolean cancel(long token);

    /**
     * @return A snapshot of all existing
     * data info in this data base.
     */
    Map<String, DataInfo> getDataInfo();

    void update(DataInfo dataInfo);

    /**
     * @param hash
     * @return The data info with the given hash.
     */
    DataInfo get(String hash);

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
     * @param dataInfo
     * @param chunkIndex
     * @param offset
     * @param byteBuf
     * @param length
     * @param download
     * @throws IOException
     */
    void processBufferAndComplete(DataInfo dataInfo,
                                  int chunkIndex,
                                  long offset,
                                  ByteBuf byteBuf,
                                  int length,
                                  boolean download) throws IOException;

    /**
     * Depending on the download flag this method fills the given
     * buffer or reads from it.
     *
     * @param dataInfo
     * @param chunkIndex
     * @param offset
     * @param byteBuf
     * @param length
     * @param download
     * @throws IOException
     */
    void processBuffer(DataInfo dataInfo,
                       int chunkIndex,
                       long offset,
                       ByteBuf byteBuf,
                       int length,
                       boolean download) throws IOException;

    default TransferManager createTransferManager(Transfer transfer) {

        Objects.requireNonNull(transfer);

        DataInfo existingDataInfo = get(transfer.getDataInfo().getHash());
        if (transfer.isUpload() && existingDataInfo == null) {
            throw new IllegalArgumentException("transfer.isUpload() && existingDataInfo == null");
        }

        if (transfer.isUpload() && !existingDataInfo.contains(transfer.getDataInfo())) {
            throw new IllegalArgumentException("!download && !existingDataInfo.contains(transfer.getDataInfo())");
        }

        return new TransferManager(this, transfer);
    }
}
