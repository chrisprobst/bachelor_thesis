package de.probst.ba.core.net.peer;

import de.probst.ba.core.Config;
import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.transfer.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 01.09.14.
 */
public abstract class AbstractLeecher extends AbstractPeer implements Leecher {

    private final Logger logger =
            LoggerFactory.getLogger(AbstractLeecher.class);

    private final class LeecherDistributionAlgorithmWorker implements Runnable {

        public void schedule() {
            getScheduler().schedule(this,
                    Config.getLeecherDistributionAlgorithmDelay(),
                    Config.getDefaultTimeUnit());
        }

        public void execute() {
            getScheduler().execute(this);
        }

        @Override
        public synchronized void run() {
            try {
                // Get the active network state
                LeecherState state = getPeerState();

                // Let the algorithm generate transfers
                Optional<List<Transfer>> transfers =
                        getDistributionAlgorithm().requestDownloads(state);

                // This is most likely a bug
                if (transfers == null) {
                    logger.warn("Algorithm returned null for optional list of transfers");
                    schedule();
                    return;
                }

                // The algorithm do not want to
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
                                        "The algorithm requested the same chunk from different peers: " + transfers.get());
                            }
                        });*/

                // Create a list of transfers with distinct remote peer ids
                // and request them to download
                transfers.get().stream()
                        .filter(t -> !state.getDownloads().containsKey(t.getRemotePeerId()))
                        .collect(Collectors.groupingBy(Transfer::getRemotePeerId))
                        .entrySet().stream()
                        .filter(p -> p.getValue().size() == 1)
                        .map(p -> p.getValue().get(0))
                        .forEach(AbstractLeecher.this::requestDownload);

                // Rerun later
                schedule();
            } catch (Exception e) {
                logger.error("The algorithm is dead, shutting leecher down", e);
                silentClose();
            }
        }
    }

    private final LeecherDistributionAlgorithmWorker leecherDistributionAlgorithmWorker =
            new LeecherDistributionAlgorithmWorker();

    protected abstract ScheduledExecutorService getScheduler();

    protected abstract void requestDownload(Transfer transfer);

    protected abstract Map<PeerId, Transfer> getDownloads();

    protected abstract Map<PeerId, Map<String, DataInfo>> getRemoteDataInfo();

    protected abstract long getDownloadRate();

    protected AbstractLeecher(PeerId peerId,
                              DataBase dataBase,
                              LeecherDistributionAlgorithm distributionAlgorithm,
                              LeecherHandler peerHandler) {
        super(peerId, dataBase, distributionAlgorithm, peerHandler);

        // Register the algorithm worker for execution
        getInitFuture().thenRun(leecherDistributionAlgorithmWorker::schedule);
    }

    @Override
    public LeecherHandler getPeerHandler() {
        return (LeecherHandler) super.getPeerHandler();
    }

    @Override
    public LeecherDistributionAlgorithm getDistributionAlgorithm() {
        return (LeecherDistributionAlgorithm) super.getDistributionAlgorithm();
    }

    @Override
    public LeecherState getPeerState() {
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

        return new LeecherState(
                getPeerId(),
                getDataBase().getDataInfo(),
                getRemoteDataInfo(),
                downloads,
                getDownloadRate());
    }
}
