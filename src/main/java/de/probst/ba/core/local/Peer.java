package de.probst.ba.core.local;

import de.probst.ba.core.logic.DataInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * Created by chrisprobst on 08.08.14.
 */
class Peer implements Runnable {

    private static final ScheduledExecutorService SCHEDULER =
            Executors.newScheduledThreadPool(8);

    /**
     * Represents an upload from the peer to this remote actor.
     */
    private class UploadTask {
        private final long destPeerId;
        private final CompletableFuture<Void> future;
        private final DataInfo dataInfo;
        private volatile long size;

        private UploadTask(long destPeerId, CompletableFuture<Void> future, DataInfo dataInfo) {
            this.destPeerId = destPeerId;
            this.future = future;
            this.dataInfo = dataInfo;
            size = dataInfo.getMissingSize();
        }

        public long getDestPeerId() {
            return destPeerId;
        }

        public CompletableFuture<Void> getFuture() {
            return future;
        }

        public long reduzeSize(long amount) {
            if (size <= amount) {
                amount = size;
                size = 0;
                future.complete(null);
            } else {
                size -= amount;
            }
            return amount;
        }

        public boolean isFinished() {
            return size <= 0;
        }

        public long getSize() {
            return size;
        }
    }

    private final long peerId;
    private final long downloadRate;
    private final long uploadRate;

    private final ConcurrentMap<String, DataInfo> dataInfoMap =
            new ConcurrentHashMap<>();

    // All uploads are stored here
    private final Queue<UploadTask> uploads = new ConcurrentLinkedQueue<>();

    Peer(long peerId,
         long downloadRate,
         long uploadRate) {

        if (downloadRate < uploadRate) {
            throw new IllegalArgumentException(
                    "downloadRate must be >= uploadRate");
        }

        this.peerId = peerId;
        this.downloadRate = downloadRate;
        this.uploadRate = uploadRate;

        SCHEDULER.execute(this);
    }

    public long getPeerId() {
        return peerId;
    }

    public long getUploadRate() {
        return uploadRate;
    }

    public long getDownloadRate() {
        return downloadRate;
    }


    /**
     * Scheduled every second!
     */
    @Override
    public void run() {
        /*
        This method runs every second so we can
        send as much data as our upload rate allow
        us to do.
         */
        long total = 0;
        long mtu = 1000;
        while (total < getUploadRate()) {
            UploadTask task = uploads.poll();

            // Wait a second if no task is there
            if (task == null) {
                break;
            }

            // A finished task .. just skip!
            if (task.isFinished()) {
                continue;
            }

            // The number of bytes transferred
            long transferred = task.reduzeSize(mtu);

            // Add this to our total counter
            total += transferred;

            // Reschedule
            if (!task.isFinished()) {
                uploads.offer(task);
            }
        }

        if (total > 0) {
            System.out.println("Client " + getPeerId() + " transferred " + total + " bytes in one second");
        }

        // Rerun this uploader in one second
        SCHEDULER.schedule(this, 1, TimeUnit.SECONDS);
    }

    public Map<String, DataInfo> getDataInfo() {
        return new HashMap<>(dataInfoMap);
    }

    public CompletableFuture<Void> upload(long destId, DataInfo dataInfo) {
        // Lookup the peer data
        DataInfo peerDataInfo = dataInfoMap.get(dataInfo.getHash());

        // No file found
        if (peerDataInfo == null) {
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(new NoSuchElementException(peerDataInfo.getHash()));
            return failed;
        }

        // Index out of bounds
        if (!peerDataInfo.contains(dataInfo)) {
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IndexOutOfBoundsException("Some chunks are not present"));
            return failed;
        }

        // Create a new valid upload task
        UploadTask task = new UploadTask(
                destId,
                new CompletableFuture<>(),
                dataInfo);

        // Add to uploads
        uploads.offer(task);

        // Offer the future
        return task.getFuture();
    }
}
