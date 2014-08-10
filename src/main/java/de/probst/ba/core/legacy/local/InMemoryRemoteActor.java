package de.probst.ba.core.legacy.local;

import de.probst.ba.core.legacy.AbstractRemoteActor;
import de.probst.ba.core.logic.DataInfo;
import de.probst.ba.core.legacy.LocalActor;
import de.probst.ba.core.legacy.RemoteActorCloud;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by chrisprobst on 09.08.14.
 */
public class InMemoryRemoteActor extends AbstractRemoteActor implements Runnable {

    /**
     * Represents an upload from the peer to this remote actor.
     */
    private class UploadTask {
        private final CompletableFuture<Void> future;
        private final Consumer<byte[]> consumer;
        private volatile long size;

        private UploadTask(CompletableFuture<Void> future, Consumer<byte[]> consumer, long size) {
            this.future = future;
            this.consumer = consumer;
            this.size = size;
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

        public Consumer<byte[]> getConsumer() {
            return consumer;
        }
    }

    // All uploads are stored here
    private final Queue<UploadTask> uploads = new ConcurrentLinkedQueue<>();

    // The local peer
    private final LocalActor peer;

    protected InMemoryRemoteActor(RemoteActorCloud remoteActorCloud,
                                  long id,
                                  long downloadRate,
                                  long uploadRate,
                                  LocalActor peer) {

        super(remoteActorCloud, id, downloadRate, uploadRate);
        this.peer = peer;
        getScheduler().execute(this);
    }

    public LocalActor getPeer() {
        return peer;
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

            // Fake consumption
            task.getConsumer().accept(null);

            // Add this to our total counter
            total += transferred;

            // Reschedule
            if (!task.isFinished()) {
                uploads.offer(task);
            }
        }

        if (total > 0) {
            System.out.println("Client " + getId() + " transferred " + total + " bytes in one second");
        }

        // Rerun this uploader in one second
        getScheduler().schedule(this, 1, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<Map<String, DataInfo>> getAllDataAsync() {
        System.out.println("Uploading data infos from: " + getId());
        return CompletableFuture.completedFuture(new HashMap<>(peer.getAllData()));
    }

    @Override
    public CompletableFuture<Void> getDataContentChunkAsync(String hash,
                                                            int chunkIndex,
                                                            Consumer<byte[]> consumer) {
        // Lookup the peer data
        DataInfo peerDataInfo = peer.getAllData().get(hash);

        // No file found
        if (peerDataInfo == null) {
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(new NoSuchElementException(hash));
            return failed;
        }

        // Index out of bounds
        if (peerDataInfo.getChunkCount() <= chunkIndex) {
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IndexOutOfBoundsException("Chunk index: " + chunkIndex));
            return failed;
        }

        // Create a new valid upload task
        UploadTask task = new UploadTask(
                new CompletableFuture<>(),
                consumer,
                peerDataInfo.getChunkSize(chunkIndex));

        // Add to uploads
        uploads.offer(task);

        // Offer the future
        return task.getFuture();
    }

    @Override
    public CompletableFuture<Void> getDataContentAsync(String hash,
                                                       Consumer<byte[]> consumer) {
        // Lookup the peer data
        DataInfo peerDataInfo = peer.getAllData().get(hash);

        // No file found
        if (peerDataInfo == null) {
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(new NoSuchElementException(hash));
            return failed;
        }

        // Create a new valid upload task
        UploadTask task = new UploadTask(
                new CompletableFuture<>(),
                consumer,
                peerDataInfo.getSize());

        // Add to uploads
        uploads.offer(task);

        // Offer the future
        return task.getFuture();
    }
}
