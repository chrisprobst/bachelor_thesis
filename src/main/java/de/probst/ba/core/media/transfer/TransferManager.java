package de.probst.ba.core.media.transfer;

import de.probst.ba.core.media.database.DataBase;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.util.Objects;
import java.util.PrimitiveIterator;

/**
 * This class can manage transfers, both upload
 * and download.
 * <p>
 * Created by chrisprobst on 15.08.14.
 */
public final class TransferManager {

    // The data base
    private final DataBase dataBase;

    // Iterates all missing chunks
    private final PrimitiveIterator.OfInt missingChunks;

    // The transfer
    // Must be volatile because we could access
    // this field from other threads
    private volatile Transfer transfer;

    // Status variables for every chunk
    private int chunkIndex;
    private long chunkSize;
    private long offset;

    public TransferManager(DataBase dataBase, Transfer transfer) {
        Objects.requireNonNull(dataBase);
        Objects.requireNonNull(transfer);

        if (transfer.getDataInfo().isEmpty()) {
            throw new IllegalArgumentException("Transfer has no chunks");
        }

        this.dataBase = dataBase;
        this.transfer = transfer;

        // Create an iterator for all missing chunks
        missingChunks = transfer.getDataInfo().getCompletedChunks().iterator();

        // Setup the next chunk transfer
        // for the first time
        setupNextChunkTransfer();
    }

    /**
     * Setup all internal variables for the next chunk.
     *
     * @return True if there are missing chunks,
     * otherwise false.
     */
    private boolean setupNextChunkTransfer() {
        // We have no more chunks to transfer
        if (!missingChunks.hasNext()) {
            return false;
        }

        // Get the next missing chunk index
        chunkIndex = missingChunks.next();
        chunkSize = getTransfer().getDataInfo().getChunkSize(chunkIndex);
        offset = 0;

        return true;
    }

    /**
     * @return True if the transfer is completed,
     * otherwise false.
     */
    public boolean isCompleted() {
        return getTransfer().isCompleted();
    }

    /**
     * Process all or only some parts of the
     * given byte buffer using the data base.
     * <p>
     * Depending on the transfer type the given
     * byte buffer will be filled or consumed.
     *
     * @param byteBuf
     * @return True if there is more work to do,
     * otherwise false.
     * @throws IOException
     */
    public synchronized boolean process(ByteBuf byteBuf) throws IOException {
        // The transfer is already finished
        if (isCompleted()) {
            return false;
        }

        // Is download or upload ?
        boolean isDownload = getTransfer().isDownload();

        // Calculate the buffer length
        int remaining = isDownload ? byteBuf.readableBytes() : byteBuf.writableBytes();
        int bufferLength = (int) Math.min(remaining, chunkSize - offset);

        // Advance the transfer
        transfer = transfer.advance(bufferLength);

        // Do we have finished the chunk
        boolean chunkCompleted = offset + bufferLength == chunkSize;

        if (chunkCompleted) {
            // We can complete the chunk
            dataBase.processBufferAndComplete(getTransfer().getDataInfo(),
                                              chunkIndex,
                                              offset,
                                              byteBuf,
                                              bufferLength,
                                              isDownload);

        } else {
            // Fill the chunk
            dataBase.processBuffer(getTransfer().getDataInfo(),
                                   chunkIndex,
                                   offset,
                                   byteBuf,
                                   bufferLength,
                                   isDownload);
        }

        // Increase
        offset += bufferLength;

        return !chunkCompleted || setupNextChunkTransfer();
    }

    /**
     * Thread-safe.
     *
     * @return The transfer.
     */
    public Transfer getTransfer() {
        return transfer;
    }

    @Override
    public String toString() {
        return "TransferManager{" +
               "transfer=" + transfer +
               ", chunkIndex=" + chunkIndex +
               ", chunkSize=" + chunkSize +
               ", offset=" + offset +
               '}';
    }
}
