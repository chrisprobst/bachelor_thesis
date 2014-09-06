package de.probst.ba.core.net.peer;

import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.transfer.Transfer;
import de.probst.ba.core.net.peer.handler.LeecherPeerAdapter;
import de.probst.ba.core.net.peer.handler.LeecherPeerHandler;
import de.probst.ba.core.net.peer.state.LeecherDataInfoState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 01.09.14.
 */
public abstract class AbstractLeecher extends AbstractPeer implements Leecher {

    private final Logger logger = LoggerFactory.getLogger(AbstractLeecher.class);
    private final LeecherDistributionAlgorithmWorker leecherDistributionAlgorithmWorker;

    protected AbstractLeecher(PeerId peerId,
                              DataBase dataBase,
                              LeecherDistributionAlgorithm leecherDistributionAlgorithm,
                              Optional<LeecherPeerHandler> leecherHandler,
                              Executor executor) {
        super(peerId, dataBase, leecherDistributionAlgorithm, leecherHandler.orElseGet(LeecherPeerAdapter::new));
        leecherDistributionAlgorithmWorker = new LeecherDistributionAlgorithmWorker(executor);
    }

    protected abstract void requestDownload(Transfer transfer);

    protected abstract Map<PeerId, Transfer> getDownloads();

    protected abstract Map<PeerId, Map<String, DataInfo>> getRemoteDataInfo();

    @Override
    public LeecherPeerHandler getPeerHandler() {
        return (LeecherPeerHandler) super.getPeerHandler();
    }

    @Override
    public LeecherDistributionAlgorithm getDistributionAlgorithm() {
        return (LeecherDistributionAlgorithm) super.getDistributionAlgorithm();
    }

    @Override
    public void leech() {
        leecherDistributionAlgorithmWorker.execute();
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

        return new LeecherDataInfoState(this, getDataBase().getDataInfo(), getRemoteDataInfo(), downloads);
    }

    private final class LeecherDistributionAlgorithmWorker implements Runnable {

        private final Executor executor;

        public LeecherDistributionAlgorithmWorker(Executor executor) {
            Objects.requireNonNull(executor);
            this.executor = executor;
        }

        public void execute() {
            executor.execute(this);
        }

        @Override
        public synchronized void run() {
            try {
                // Let the algorithm generate transfers
                List<Transfer> transfers = getDistributionAlgorithm().requestDownloads(AbstractLeecher.this);

                // This is most likely a bug
                if (transfers == null) {
                    logger.warn("Algorithm returned null for optional list of transfers");
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
                logger.error("The algorithm is dead, shutting leecher down", e);
                silentClose();
            }
        }
    }
}
