package de.probst.ba.core.local;

import de.probst.ba.core.logic.DataInfo;
import de.probst.ba.core.logic.Network;
import de.probst.ba.core.logic.NetworkState;
import de.probst.ba.core.logic.Transfer;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 08.08.14.
 */
public class Peer implements Runnable, Network {

    private static final AtomicLong ID_GEN = new AtomicLong();

    private static final ConcurrentMap<Long, Peer> PEERS =
            new ConcurrentHashMap<>();

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

        public DataInfo getDataInfo() {
            return dataInfo;
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

    private volatile Map<Long, Map<String, DataInfo>> remoteDataInfoMap
            = new HashMap<>();

    private final Runnable remoteSyncTask = new Runnable() {
        @Override
        public void run() {

            remoteDataInfoMap = PEERS.entrySet().stream()
                    .collect(Collectors.toMap(
                            p -> p.getValue().getPeerId(),
                            p -> p.getValue().getDataInfo()
                    ));

            // Rerun later
            SCHEDULER.schedule(this, 500, TimeUnit.MILLISECONDS);
        }
    };

    private Map<Long, Map<String, DataInfo>> getRemoteDataInfoMap() {
        return new HashMap<>(remoteDataInfoMap);
    }

    private Map<Long, Transfer> getUpload() {
        return new HashSet<>(uploads).stream()
                .collect(Collectors.toMap(
                        p -> p.getDestPeerId(),
                        p -> new Transfer(p.getDestPeerId(), p.getDataInfo())
                ));
    }

    @Override
    public NetworkState getNetworkState() {
        return new NetworkState(peerId,
                getUpload(),
                null,
                getDataInfo(),
                getRemoteDataInfoMap()
        );
    }

    // All uploads are stored here
    private final Queue<UploadTask> uploads = new ConcurrentLinkedQueue<>();

    public Peer(long downloadRate,
                long uploadRate) {

        if (downloadRate < uploadRate) {
            throw new IllegalArgumentException(
                    "downloadRate must be >= uploadRate");
        }

        peerId = ID_GEN.getAndIncrement();
        this.downloadRate = downloadRate;
        this.uploadRate = uploadRate;

        SCHEDULER.execute(this);
        SCHEDULER.execute(remoteSyncTask);
    }

    public void register() {
        PEERS.put(peerId, this);
    }

    public void unregister() {
        PEERS.remove(peerId);
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
            UploadTask task = uploads.peek();

            // Wait a second if no task is there
            if (task == null) {
                break;
            }

            // A finished task .. just skip!
            if (task.isFinished()) {
                uploads.poll();
                continue;
            }

            // The number of bytes transferred
            long transferred = task.reduzeSize(mtu);

            // Add this to our total counter
            total += transferred;

            // Reschedule
            if (!task.isFinished()) {
                uploads.offer(task);
                uploads.poll();
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

    @Override
    public void download(long peerId, DataInfo dataInfo) {
        System.out.println("Requested download: " + dataInfo + " from peer " + peerId);
    }

    public CompletableFuture<Void> upload(long destId, DataInfo dataInfo) {
        // Lookup the peer data
        DataInfo peerDataInfo = dataInfoMap.get(dataInfo.getHash());

        // No file found
        if (peerDataInfo == null) {
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(new NoSuchElementException(dataInfo.getHash()));
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
