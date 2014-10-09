package de.probst.ba.core.net.peer;

import de.probst.ba.core.distribution.DistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.net.peer.handler.PeerHandler;
import de.probst.ba.core.net.peer.handler.PeerHandlerAdapter;
import de.probst.ba.core.net.peer.state.BandwidthStatisticState;
import de.probst.ba.core.net.peer.state.DataInfoState;
import de.probst.ba.core.util.concurrent.CachedSupplier;
import de.probst.ba.core.util.concurrent.CancelableRunnable;
import de.probst.ba.core.util.concurrent.LeakyBucket;
import de.probst.ba.core.util.concurrent.LeakyBucketRefiller;
import de.probst.ba.core.util.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by chrisprobst on 17.08.14.
 */
public abstract class AbstractPeer implements Peer {

    private final Logger logger = LoggerFactory.getLogger(AbstractPeer.class);

    private final Optional<CancelableRunnable> leakyBucketRefillTask;

    private final Optional<LeakyBucket> leakyUploadBucket;

    private final Optional<LeakyBucket> leakyDownloadBucket;

    private final DataBase dataBase;

    private final DistributionAlgorithm distributionAlgorithm;

    private final PeerHandler peerHandler;

    private final CompletableFuture<? extends Peer> initFuture = new CompletableFuture<>();

    private final CompletableFuture<? extends Peer> closeFuture = new CompletableFuture<>();

    private final CachedSupplier<BandwidthStatisticState> cachedBandwidthStatisticState =
            new CachedSupplier<>(this::createBandwidthStatisticState,
                                 PeerConfig.getMinimalBandwidthStatisticStateCreationDelay());

    private volatile Optional<PeerId> peerId;

    protected Optional<LeakyBucket> getLeakyDownloadBucket() {
        return leakyDownloadBucket;
    }

    protected Optional<LeakyBucket> getLeakyUploadBucket() {
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

    protected abstract BandwidthStatisticState createBandwidthStatisticState();

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
        double factor = PeerConfig.getLeakyBucketMaxTokensFactor();
        long maxUploadTokens = (long) (maxUploadRate * factor);
        leakyUploadBucket = maxUploadRate > 0 ?
                            Optional.of(new LeakyBucket(maxUploadTokens, maxUploadRate)) :
                            Optional.empty();

        long maxDownloadTokens = (long) (maxDownloadRate * factor);
        leakyDownloadBucket = maxDownloadRate > 0 ?
                              Optional.of(new LeakyBucket(maxDownloadTokens, maxDownloadRate)) :
                              Optional.empty();

        if (leakyUploadBucket.isPresent() || leakyDownloadBucket.isPresent()) {
            // Setup runnable
            LeakyBucketRefiller leakyBucketRefiller = new LeakyBucketRefiller();
            leakyUploadBucket.ifPresent(leakyBucketRefiller::add);
            leakyDownloadBucket.ifPresent(leakyBucketRefiller::add);

            // Create task
            long interval = PeerConfig.getLeakyBucketRefillInterval();
            CancelableRunnable task =
                    new Task(leakyBucketRefiller,
                             runnable -> leakyBucketRefillTaskScheduler.schedule(runnable,
                                                                                 interval,
                                                                                 TimeUnit.MILLISECONDS));
            leakyBucketRefillTask = Optional.of(task);
            task.run();
        } else {
            leakyBucketRefillTask = Optional.empty();
        }
    }

    @Override
    public BandwidthStatisticState getBandwidthStatisticState() {
        return cachedBandwidthStatisticState.get();
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
            leakyBucketRefillTask.ifPresent(CancelableRunnable::cancel);
        } finally {
            getDataBase().close();
        }
    }

    @Override
    public DataBase getDataBase() {
        return dataBase;
    }
}
