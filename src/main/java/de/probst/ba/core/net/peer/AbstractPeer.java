package de.probst.ba.core.net.peer;

import de.probst.ba.core.distribution.DistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.net.peer.handler.PeerHandler;
import de.probst.ba.core.net.peer.handler.PeerHandlerAdapter;
import de.probst.ba.core.net.peer.state.DataInfoState;
import de.probst.ba.core.util.concurrent.LeakyBucket;
import de.probst.ba.core.util.concurrent.LeakyBucketRefillTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by chrisprobst on 17.08.14.
 */
public abstract class AbstractPeer implements Peer {

    public static final int LEAKY_BUCKET_REFILL_INTERVAL = 250;

    private final Logger logger = LoggerFactory.getLogger(AbstractPeer.class);

    private final Optional<LeakyBucketRefillTask> leakyBucketRefillTask;

    private final Optional<LeakyBucket> leakyUploadBucket;

    private final Optional<LeakyBucket> leakyDownloadBucket;

    private final DataBase dataBase;

    private final DistributionAlgorithm distributionAlgorithm;

    private final PeerHandler peerHandler;

    private final CompletableFuture<? extends Peer> initFuture = new CompletableFuture<>();

    private final CompletableFuture<? extends Peer> closeFuture = new CompletableFuture<>();

    private volatile Optional<PeerId> peerId;

    public Optional<LeakyBucket> getLeakyDownloadBucket() {
        return leakyDownloadBucket;
    }

    public Optional<LeakyBucket> getLeakyUploadBucket() {
        return leakyUploadBucket;
    }

    protected void setPeerId(Optional<PeerId> peerId) {
        Objects.requireNonNull(peerId);
        this.peerId = peerId;
    }

    protected void silentClose() {
        try {
            close();
        } catch (IOException e) {
            logger.error("Peer " + getPeerId() + " failed to silently close", e);
        }
    }

    public AbstractPeer(long maxUploadRate,
                        long maxDownloadRate,
                        Optional<PeerId> peerId,
                        DataBase dataBase,
                        DistributionAlgorithm distributionAlgorithm,
                        Optional<PeerHandler> peerHandler,
                        ScheduledExecutorService leakyBucketRefillTaskScheduler) {

        Objects.requireNonNull(peerId);
        Objects.requireNonNull(dataBase);
        Objects.requireNonNull(distributionAlgorithm);
        Objects.requireNonNull(peerHandler);
        Objects.requireNonNull(leakyBucketRefillTaskScheduler);

        // Save args
        this.peerId = peerId;
        this.dataBase = dataBase;
        this.distributionAlgorithm = distributionAlgorithm;
        this.peerHandler = peerHandler.orElseGet(PeerHandlerAdapter::new);

        // Leaky bucket
        leakyUploadBucket =
                maxUploadRate > 0 ? Optional.of(new LeakyBucket(maxUploadRate, maxUploadRate)) : Optional.empty();
        leakyDownloadBucket =
                maxDownloadRate > 0 ? Optional.of(new LeakyBucket(maxDownloadRate, maxDownloadRate)) : Optional.empty();

        if (leakyUploadBucket.isPresent() || leakyDownloadBucket.isPresent()) {
            leakyBucketRefillTask = Optional.of(new LeakyBucketRefillTask(leakyBucketRefillTaskScheduler,
                                                                          LEAKY_BUCKET_REFILL_INTERVAL));
            leakyUploadBucket.ifPresent(leakyBucketRefillTask.get()::add);
            leakyDownloadBucket.ifPresent(leakyBucketRefillTask.get()::add);
        } else {
            leakyBucketRefillTask = Optional.empty();
        }
    }

    @Override
    public PeerId getPeerId() {
        return peerId.get();
    }

    @Override
    public CompletableFuture<? extends Peer> getInitFuture() {
        return initFuture;
    }

    @Override
    public CompletableFuture<? extends Peer> getCloseFuture() {
        return closeFuture;
    }

    @Override
    public DistributionAlgorithm getDistributionAlgorithm() {
        return distributionAlgorithm;
    }

    @Override
    public PeerHandler getPeerHandler() {
        return peerHandler;
    }

    @Override
    public DataInfoState getDataInfoState() {
        return new DataInfoState(this, getDataBase().getDataInfo());
    }

    @Override
    public void close() throws IOException {
        try {
            leakyBucketRefillTask.ifPresent(LeakyBucketRefillTask::cancel);
        } finally {
            getDataBase().flush();
        }
    }

    @Override
    public DataBase getDataBase() {
        return dataBase;
    }
}
