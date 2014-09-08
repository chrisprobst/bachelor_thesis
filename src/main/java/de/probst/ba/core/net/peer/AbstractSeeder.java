package de.probst.ba.core.net.peer;

import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.transfer.Transfer;
import de.probst.ba.core.net.peer.handler.SeederPeerAdapter;
import de.probst.ba.core.net.peer.handler.SeederPeerHandler;
import de.probst.ba.core.net.peer.state.SeederDataInfoState;
import de.probst.ba.core.util.concurrent.AtomicCounter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Created by chrisprobst on 01.09.14.
 */
public abstract class AbstractSeeder extends AbstractPeer implements Seeder {

    private final AtomicCounter parallelUploads = new AtomicCounter();

    protected AtomicCounter getParallelUploads() {
        return parallelUploads;
    }

    protected abstract Map<PeerId, Transfer> getUploads();

    public AbstractSeeder(DataBase dataBase,
                          SeederDistributionAlgorithm seederDistributionAlgorithm,
                          Optional<SeederPeerHandler> seederHandler) {
        super(Optional.empty(),
              dataBase,
              seederDistributionAlgorithm,
              Optional.of(seederHandler.orElseGet(SeederPeerAdapter::new)));
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
