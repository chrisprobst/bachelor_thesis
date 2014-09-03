package de.probst.ba.core.net.peer;

import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.transfer.Transfer;
import de.probst.ba.core.net.peer.handler.SeederAdapter;
import de.probst.ba.core.net.peer.handler.SeederHandler;
import de.probst.ba.core.net.peer.state.SeederDiagnosticState;
import de.probst.ba.core.net.peer.state.SeederState;
import de.probst.ba.core.util.concurrent.AtomicCounter;

import java.util.Map;
import java.util.Optional;

/**
 * Created by chrisprobst on 01.09.14.
 */
public abstract class AbstractSeeder extends AbstractPeer implements Seeder {

    private final AtomicCounter parallelUploads =
            new AtomicCounter();

    protected AtomicCounter getParallelUploads() {
        return parallelUploads;
    }

    protected abstract Map<PeerId, Transfer> getUploads();

    protected abstract long getMaxUploadRate();

    protected abstract long getAverageUploadRate();

    protected abstract long getCurrentUploadRate();

    protected abstract long getTotalUploaded();

    protected AbstractSeeder(PeerId peerId,
                             DataBase dataBase,
                             SeederDistributionAlgorithm seederDistributionAlgorithm,
                             Optional<SeederHandler> seederHandler) {
        super(peerId, dataBase, seederDistributionAlgorithm, seederHandler.orElseGet(SeederAdapter::new));
    }

    @Override
    public SeederHandler getPeerHandler() {
        return (SeederHandler) super.getPeerHandler();
    }

    @Override
    public SeederDistributionAlgorithm getDistributionAlgorithm() {
        return (SeederDistributionAlgorithm) super.getDistributionAlgorithm();
    }

    @Override
    public SeederDiagnosticState getDiagnosticState() {
        return new SeederDiagnosticState(this,
                getMaxUploadRate(),
                getAverageUploadRate(),
                getCurrentUploadRate(),
                getTotalUploaded());
    }

    @Override
    public SeederState getDataInfoState() {
        return new SeederState(this,
                getDataBase().getDataInfo(),
                getUploads());
    }
}
