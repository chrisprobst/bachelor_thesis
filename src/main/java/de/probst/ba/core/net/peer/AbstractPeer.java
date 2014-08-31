package de.probst.ba.core.net.peer;

import de.probst.ba.core.Config;
import de.probst.ba.core.diag.Diagnostic;
import de.probst.ba.core.logic.Brain;
import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.NetworkState;
import de.probst.ba.core.net.Transfer;
import de.probst.ba.core.util.concurrent.AtomicCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 17.08.14.
 */
public abstract class AbstractPeer implements Peer {

    private final Logger logger =
            LoggerFactory.getLogger(AbstractPeer.class);

    private final class BrainWorker implements Runnable {

        public void schedule() {
            getScheduler().schedule(this,
                    Config.getBrainDelay(),
                    Config.getDefaultTimeUnit());
        }

        public void execute() {
            getScheduler().execute(this);
        }

        @Override
        public synchronized void run() {
            try {
                // Get the active network state
                NetworkState networkState = getNetworkState();

                // Let the brain generate transfers
                Optional<List<Transfer>> transfers =
                        getBrain().process(networkState);

                // This is most likely a brain bug
                if (transfers == null) {
                    logger.warn("Brain returned null for optional list of transfers");
                    schedule();
                    return;
                }

                // The brain do not want to
                // download anything
                if (!transfers.isPresent() ||
                        transfers.get().isEmpty()) {
                    schedule();
                    return;
                }

                // Check that there are no duplicates
                // Note: Not necessary, race condition was fixed
                /*
                transfers.get().stream()
                        .map(Transfer::getDataInfo)
                        .collect(Collectors.groupingBy(DataInfo::getHash)).values().stream()
                        .filter(l -> l.size() > 1) // There cannot be an overlapping
                        .map(l -> l.stream().reduce(DataInfo::intersection))
                        .map(Optional::get)
                        .forEach(dataInfo -> {
                            if (!dataInfo.isEmpty()) {
                                throw new IllegalStateException(
                                        "Brain requested the same chunk from different peers: " + transfers.get());
                            }
                        });*/

                // Create a list of transfers with distinct remote peer ids
                // and request them to download
                transfers.get().stream()
                        .filter(t -> !networkState.getDownloads().containsKey(t.getRemotePeerId()))
                        .collect(Collectors.groupingBy(Transfer::getRemotePeerId))
                        .entrySet().stream()
                        .filter(p -> p.getValue().size() == 1)
                        .map(p -> p.getValue().get(0))
                        .forEach(AbstractPeer.this::requestDownload);

                // Rerun later
                schedule();
            } catch (Exception e) {
                logger.error("The brain is dead, shutting peer down", e);
                silentClose();
            }
        }
    }

    private final PeerId localPeerId;

    private final DataBase dataBase;

    private final Brain brain;

    private final Diagnostic diagnostic;

    private final BrainWorker brainWorker =
            new BrainWorker();

    private final CompletableFuture<?> initFuture =
            new CompletableFuture<>();

    private final AtomicCounter parallelUploads =
            new AtomicCounter();

    protected AtomicCounter getParallelUploads() {
        return parallelUploads;
    }

    protected void silentClose() {
        try {
            close();
        } catch (IOException e) {
            logger.error("The brain is dead, shutting peer down", e);
        }
    }

    protected abstract void requestDownload(Transfer transfer);

    protected abstract ScheduledExecutorService getScheduler();

    protected abstract Map<PeerId, Transfer> getUploads();

    protected abstract Map<PeerId, Transfer> getDownloads();

    protected abstract Map<PeerId, Map<String, DataInfo>> getRemoteDataInfo();

    protected abstract long getUploadRate();

    protected abstract long getDownloadRate();

    protected AbstractPeer(PeerId localPeerId,
                           DataBase dataBase,
                           Brain brain,
                           Diagnostic diagnostic) {

        Objects.requireNonNull(localPeerId);
        Objects.requireNonNull(dataBase);
        Objects.requireNonNull(brain);
        Objects.requireNonNull(diagnostic);

        // Save args
        this.localPeerId = localPeerId;
        this.dataBase = dataBase;
        this.brain = brain;
        this.diagnostic = diagnostic;

        // Register the brain worker for execution
        getInitFuture().thenRun(brainWorker::schedule);
    }

    // ************ INTERFACE METHODS

    @Override
    public PeerId getLocalPeerId() {
        return localPeerId;
    }

    @Override
    public CompletableFuture<?> getInitFuture() {
        return initFuture;
    }

    @Override
    public Brain getBrain() {
        return brain;
    }

    @Override
    public Diagnostic getDiagnostic() {
        return diagnostic;
    }

    @Override
    public NetworkState getNetworkState() {
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

        return new NetworkState(
                getLocalPeerId(),
                getDataBase().getDataInfo(),
                getRemoteDataInfo(),
                getUploads(),
                downloads,
                getUploadRate(),
                getDownloadRate());
    }

    @Override
    public void close() throws IOException {
        getDataBase().close();
    }

    @Override
    public DataBase getDataBase() {
        return dataBase;
    }
}
