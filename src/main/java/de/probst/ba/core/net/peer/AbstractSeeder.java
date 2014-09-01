package de.probst.ba.core.net.peer;

import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.transfer.Transfer;
import de.probst.ba.core.util.concurrent.AtomicCounter;

import java.util.Map;

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

    protected abstract long getUploadRate();

    protected AbstractSeeder(PeerId peerId,
                             DataBase dataBase,
                             SeederDistributionAlgorithm distributionAlgorithm,
                             SeederHandler peerHandler) {
        super(peerId, dataBase, distributionAlgorithm, peerHandler);
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
    public SeederState getPeerState() {
        return new SeederState(
                getPeerId(),
                getDataBase().getDataInfo(),
                getUploads(),
                getUploadRate());
    }
}
