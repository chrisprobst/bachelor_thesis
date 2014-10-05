package de.probst.ba.core.media.database;

import de.probst.ba.core.net.peer.transfer.Transfer;
import de.probst.ba.core.net.peer.transfer.TransferManager;
import io.netty.buffer.ByteBuf;

import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Map;
import java.util.Objects;

/**
 * A data base manages data info and the content.
 * <p>
 * Created by chrisprobst on 13.08.14.
 */
public interface DataBase extends Flushable {

    /**
     * Flushes all changes to the data base.
     *
     * @throws IOException
     */
    void flush() throws IOException;

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

    void insert(DataInfo dataInfo, ReadableByteChannel readableByteChannel) throws IOException;

    default void insert(DataInfo dataInfo, InputStream inputStream) throws IOException {
        insert(dataInfo, Channels.newChannel(inputStream));
    }

    void query(DataInfo dataInfo, WritableByteChannel writableByteChannel) throws IOException;

    default void query(DataInfo dataInfo, OutputStream outputStream) throws IOException {
        query(dataInfo, Channels.newChannel(outputStream));
    }

    SeekableByteChannel[] unsafeQueryRawWithName(String name) throws IOException;

    SeekableByteChannel unsafeQueryRaw(String hash) throws IOException;

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
