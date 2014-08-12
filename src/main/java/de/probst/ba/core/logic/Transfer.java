package de.probst.ba.core.logic;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a transfer.
 * <p>
 * Created by chrisprobst on 10.08.14.
 */
public final class Transfer implements Serializable {

    private static final AtomicLong TRANSFER_ID_GEN = new AtomicLong();

    // The transfer id
    private final long transferId;

    // The remote peer id
    private final Object remotePeerId;

    // The data info which describes this transfer
    private final DataInfo dataInfo;

    // The size of this transfer
    private final long size;

    // The completed size of this transfer
    private final long completedSize;

    private Transfer(long transferId,
                     Object remotePeerId,
                     DataInfo dataInfo,
                     long size,
                     long completedSize) {
        this.transferId = transferId;
        this.remotePeerId = remotePeerId;
        this.dataInfo = dataInfo;
        this.size = size;
        this.completedSize = completedSize;
    }

    public Transfer(Object remotePeerId,
                    DataInfo dataInfo) {
        this(remotePeerId, dataInfo, 0);
    }

    public Transfer(Object remotePeerId,
                    DataInfo dataInfo,
                    long completedSize) {

        Objects.requireNonNull(dataInfo);

        // Calc the size
        size = dataInfo.getCompletedSize();

        if (size <= 0) {
            throw new IllegalArgumentException("size <= 0");
        }

        if (completedSize > size) {
            throw new IllegalArgumentException("completedSize > size");
        }

        if (completedSize < 0) {
            throw new IllegalArgumentException("completedSize < 0");
        }

        transferId = TRANSFER_ID_GEN.getAndIncrement();
        this.remotePeerId = remotePeerId;
        this.dataInfo = dataInfo;
        this.completedSize = completedSize;
    }

    /**
     * Creates a new transfer which has a greater
     * completed size.
     *
     * @param size
     * @return
     */
    public Transfer advance(long size) {
        if (size > getRemainingSize()) {
            throw new IllegalArgumentException("size > getRemainingSize()");
        }

        if (size <= 0) {
            throw new IllegalArgumentException("size <= 0");
        }

        return new Transfer(
                getTransferId(),
                getRemotePeerId(),
                getDataInfo(),
                getSize(),
                getCompletedSize() + size);
    }

    /**
     * @return The id of this transfer.
     */
    public long getTransferId() {
        return transferId;
    }

    /**
     * @return Whether or not this transfer is completed.
     */
    public boolean isCompleted() {
        return getRemainingSize() == 0;
    }

    /**
     * @return The remote peer id.
     */
    public Object getRemotePeerId() {
        return remotePeerId;
    }

    /**
     * @return The data info which describes this transfer.
     */
    public DataInfo getDataInfo() {
        return dataInfo;
    }

    /**
     * @return The size of this transfer.
     */
    public long getSize() {
        return size;
    }

    /**
     * @return The completed size of this transfer.
     */
    public long getCompletedSize() {
        return completedSize;
    }

    /**
     * @return The remaining size of this transfer.
     */
    public long getRemainingSize() {
        return getSize() - getCompletedSize();
    }

    /**
     * @return The percentage of this transfer.
     */
    public double getPercentage() {
        return completedSize / (double) size;
    }
}
