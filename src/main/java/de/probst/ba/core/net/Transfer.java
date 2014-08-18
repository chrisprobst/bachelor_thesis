package de.probst.ba.core.net;

import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.peer.PeerId;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Represents a transfer.
 * <p>
 * Created by chrisprobst on 10.08.14.
 */
public final class Transfer implements Serializable {

    public static Transfer upload(PeerId remotePeerId,
                                  DataInfo dataInfo) {
        return new Transfer(remotePeerId, dataInfo, false);
    }

    public static Transfer download(PeerId remotePeerId,
                                    DataInfo dataInfo) {
        return new Transfer(remotePeerId, dataInfo, true);
    }

    // The remote peer id
    private final PeerId remotePeerId;

    // The data info which describes this transfer
    private final DataInfo dataInfo;

    // Tells whether or not this transfer
    // is a download or an upload
    private final boolean download;

    // The size of this transfer
    private final long size;

    // The completed size of this transfer
    private final long completedSize;

    private Transfer(PeerId remotePeerId,
                     DataInfo dataInfo,
                     boolean download,
                     long size,
                     long completedSize) {
        this.remotePeerId = remotePeerId;
        this.dataInfo = dataInfo;
        this.download = download;
        this.size = size;
        this.completedSize = completedSize;
    }

    public Transfer(PeerId remotePeerId,
                    DataInfo dataInfo,
                    boolean download) {
        this(remotePeerId, dataInfo, download, 0);
    }

    public Transfer(PeerId remotePeerId,
                    DataInfo dataInfo,
                    boolean download,
                    long completedSize) {

        Objects.requireNonNull(remotePeerId);
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

        this.remotePeerId = remotePeerId;
        this.dataInfo = dataInfo;
        this.download = download;
        this.completedSize = completedSize;
    }

    /**
     * @return True if this transfer
     * is an download, otherwise false.
     */
    public boolean isDownload() {
        return download;
    }

    /**
     * @return True if this transfer
     * is an upload, otherwise false.
     */
    public boolean isUpload() {
        return !isDownload();
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
                getRemotePeerId(),
                getDataInfo(),
                isDownload(),
                getSize(),
                getCompletedSize() + size);
    }

    /**
     * @return A stream of completed chunk indices
     * according to the completed size.
     */
    public IntStream getCompletedChunks() {
        List<Integer> completedChunks = new ArrayList<>();
        long cnt = getCompletedSize();
        for (int chunk : getDataInfo().getCompletedChunks().toArray()) {
            if ((cnt -= getDataInfo().getChunkSize(chunk)) >= 0) {
                completedChunks.add(chunk);
            }
        }
        return completedChunks
                .stream()
                .mapToInt(Integer::intValue);
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
    public PeerId getRemotePeerId() {
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

    @Override
    public String toString() {
        return "Transfer{" +
                "download=" + download +
                ", remotePeerId=" + remotePeerId +
                ", dataInfo=" + dataInfo +
                ", size=" + size +
                ", completedSize=" + completedSize +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Transfer transfer = (Transfer) o;

        if (completedSize != transfer.completedSize) return false;
        if (download != transfer.download) return false;
        if (size != transfer.size) return false;
        if (!dataInfo.equals(transfer.dataInfo)) return false;
        if (!remotePeerId.equals(transfer.remotePeerId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (download ? 1 : 0);
        result = 31 * result + remotePeerId.hashCode();
        result = 31 * result + dataInfo.hashCode();
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + (int) (completedSize ^ (completedSize >>> 32));
        return result;
    }
}
