package de.probst.ba.core.logic;

import de.probst.ba.core.Config;
import de.probst.ba.core.net.NetworkState;
import de.probst.ba.core.net.Transfer;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 17.08.14.
 */
public final class BrainWorker implements Runnable {

    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(BrainWorker.class);

    private final Body body;

    public BrainWorker(Body body) {
        this.body = body;
    }

    public void schedule() {
        getBody().getScheduler().schedule(this,
                Config.getBrainDelay(),
                Config.getBrainTimeUnit());
    }

    public void execute() {
        getBody().getScheduler().execute(this);
    }

    public Body getBody() {
        return body;
    }

    @Override
    public synchronized void run() {
        try {
            // Get the active network state
            NetworkState networkState = getBody().getNetworkState();

            // Let the brain generate transfers
            Optional<List<Transfer>> transfers =
                    getBody().getBrain().process(networkState);

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

            // Create a list of transfers with distinct remote peer ids
            // and request them to download
            transfers.get().stream()
                    .filter(t -> !networkState.getDownloads().containsKey(t.getRemotePeerId()))
                    .collect(Collectors.groupingBy(Transfer::getRemotePeerId))
                    .entrySet().stream()
                    .filter(p -> p.getValue().size() == 1)
                    .map(p -> p.getValue().get(0))
                    .forEach(getBody()::requestDownload);

            // Rerun later
            schedule();
        } catch (Exception e) {
            logger.error("The brain is dead, shutting peer down", e);
            getBody().brainDead(e);
        }
    }
}

