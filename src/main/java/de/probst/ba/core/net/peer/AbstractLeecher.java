package de.probst.ba.core.net.peer;

import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.handler.LeecherPeerHandler;
import de.probst.ba.core.net.peer.handler.LeecherPeerHandlerAdapter;
import de.probst.ba.core.net.peer.state.LeecherDataInfoState;
import de.probst.ba.core.util.concurrent.CancelableRunnable;
import de.probst.ba.core.util.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 01.09.14.
 */
public abstract class AbstractLeecher extends AbstractPeer implements Leecher {

    private final Logger logger = LoggerFactory.getLogger(AbstractLeecher.class);
    private final Task leecherDistributionAlgorithmWorkerTask;
    private final boolean autoConnect;

    private void runLeecherDistributionAlgorithm(CancelableRunnable cancelableRunnable) {
        try {
            // Let the algorithm generate transfers
            List<Transfer> transfers = getDistributionAlgorithm().requestDownloads(AbstractLeecher.this);

            // This is most likely a bug
            if (transfers == null) {
                logger.warn("Leecher " + getPeerId() +
                            " got null for the optional list of transfers from the algorithm");
                return;
            }

            // The algorithm do not want to
            // download anything
            if (transfers.isEmpty()) {
                return;
            }

            // Get downloads
            Map<PeerId, Transfer> downloads = getDownloads();

            // Here we check that we do not load the same
            // data info twice
            Set<DataInfo> requestedDataInfo = new HashSet<>();

            // Create a list of transfers with distinct remote peer ids
            // and request them to download
            transfers.stream()
                     .filter(t -> !downloads.containsKey(t.getRemotePeerId()))
                     .filter(t -> requestedDataInfo.add(t.getDataInfo()))
                     .collect(Collectors.groupingBy(Transfer::getRemotePeerId))
                     .entrySet()
                     .stream()
                     .filter(p -> p.getValue().size() == 1)
                     .map(p -> p.getValue().get(0))
                     .forEach(AbstractLeecher.this::requestDownload);

        } catch (Exception e) {
            logger.error("Leecher " + getPeerId() + " has a dead algorithm, shutting leecher down", e);
            silentClose();
        }
    }

    protected abstract void requestDownload(Transfer transfer);

    protected abstract Map<PeerId, Transfer> getDownloads();

    protected abstract Map<PeerId, Map<String, DataInfo>> getRemoteDataInfo();

    protected Runnable getLeechRunnable() {
        return leecherDistributionAlgorithmWorkerTask;
    }

    public AbstractLeecher(long maxUploadRate,
                           long maxDownloadRate,
                           Optional<PeerId> peerId,
                           DataBase dataBase,
                           LeecherDistributionAlgorithm leecherDistributionAlgorithm,
                           Optional<LeecherPeerHandler> leecherHandler,
                           ScheduledExecutorService leakyBucketRefillTaskScheduler,
                           boolean autoConnect,
                           ExecutorService algorithmExecutor) {
        super(maxUploadRate,
              maxDownloadRate,
              Optional.of(peerId.orElseGet(PeerId::new)),
              dataBase,
              leecherDistributionAlgorithm,
              Optional.of(leecherHandler.orElseGet(LeecherPeerHandlerAdapter::new)),
              leakyBucketRefillTaskScheduler);

        // Save args
        leecherDistributionAlgorithmWorkerTask =
                new Task(this::runLeecherDistributionAlgorithm, algorithmExecutor::submit);
        this.autoConnect = autoConnect;
    }

    @Override
    public LeecherPeerHandler getPeerHandler() {
        return (LeecherPeerHandler) super.getPeerHandler();
    }

    @Override
    public boolean isAutoConnect() {
        return autoConnect;
    }

    @Override
    public LeecherDistributionAlgorithm getDistributionAlgorithm() {
        return (LeecherDistributionAlgorithm) super.getDistributionAlgorithm();
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<Leecher> getInitFuture() {
        return (CompletableFuture<Leecher>) super.getInitFuture();
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<Leecher> getCloseFuture() {
        return (CompletableFuture<Leecher>) super.getCloseFuture();
    }

    @Override
    public LeecherDataInfoState getDataInfoState() {
        // IMPORTANT:
        // Collect downloads first, so that
        // there is no interleaving with the local data info
        // collected by the data base.
        //
        // It does not matter if there is a chunk which is completed
        // and also being downloaded.
        //
        // Otherwise this would be a race condition because
        // chunks could be totally lost:
        // -> Not in local data info AND not in downloads -> download same chunk twice -> error!
        Map<PeerId, Transfer> downloads = getDownloads();

        return new LeecherDataInfoState(this, getDataBase().getEstimatedDataInfo(), getRemoteDataInfo(), downloads);
    }
}
