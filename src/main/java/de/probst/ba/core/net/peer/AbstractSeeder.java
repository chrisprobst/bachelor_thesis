package de.probst.ba.core.net.peer;

import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.net.peer.handler.SeederPeerHandler;
import de.probst.ba.core.net.peer.handler.SeederPeerHandlerAdapter;
import de.probst.ba.core.net.peer.state.SeederDataInfoState;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by chrisprobst on 01.09.14.
 */
public abstract class AbstractSeeder extends AbstractPeer implements Seeder {

    protected abstract Map<PeerId, Transfer> getUploads();

    public AbstractSeeder(long maxUploadRate,
                          long maxDownloadRate,
                          DataBase dataBase,
                          SeederDistributionAlgorithm seederDistributionAlgorithm,
                          Optional<SeederPeerHandler> seederHandler,
                          ScheduledExecutorService leakyBucketRefillTaskScheduler) {
        super(maxUploadRate, maxDownloadRate, Optional.empty(),
              dataBase,
              seederDistributionAlgorithm,
              Optional.of(seederHandler.orElseGet(SeederPeerHandlerAdapter::new)),
              leakyBucketRefillTaskScheduler);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<Seeder> getInitFuture() {
        return (CompletableFuture<Seeder>) super.getInitFuture();
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<Seeder> getCloseFuture() {
        return (CompletableFuture<Seeder>) super.getCloseFuture();
    }


    @Override
    public SeederPeerHandler getPeerHandler() {
        return (SeederPeerHandler) super.getPeerHandler();
    }

    @Override
    public SeederDistributionAlgorithm getDistributionAlgorithm() {
        return (SeederDistributionAlgorithm) super.getDistributionAlgorithm();
    }

    @Override
    public SeederDataInfoState getDataInfoState() {
        return new SeederDataInfoState(this, getDataBase().getDataInfo(), getUploads());
    }
}
