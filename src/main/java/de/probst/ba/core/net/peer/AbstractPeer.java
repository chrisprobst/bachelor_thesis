package de.probst.ba.core.net.peer;

import de.probst.ba.core.distribution.DistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.net.peer.handler.PeerHandler;
import de.probst.ba.core.net.peer.state.DataInfoState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Created by chrisprobst on 17.08.14.
 */
public abstract class AbstractPeer implements Peer {

    private final Logger logger = LoggerFactory.getLogger(AbstractPeer.class);

    private final PeerId peerId;

    private final DataBase dataBase;

    private final DistributionAlgorithm distributionAlgorithm;

    private final PeerHandler peerHandler;

    private final CompletableFuture<?> initFuture = new CompletableFuture<>();

    private final CompletableFuture<?> closeFuture = new CompletableFuture<>();

    protected void silentClose() {
        try {
            close();
        } catch (IOException e) {
            logger.error("Peer " + getPeerId() + " failed to silently close", e);
        }
    }

    public AbstractPeer(PeerId peerId,
                        DataBase dataBase,
                        DistributionAlgorithm distributionAlgorithm,
                        PeerHandler peerHandler) {

        Objects.requireNonNull(peerId);
        Objects.requireNonNull(dataBase);
        Objects.requireNonNull(distributionAlgorithm);
        Objects.requireNonNull(peerHandler);

        // Save args
        this.peerId = peerId;
        this.dataBase = dataBase;
        this.distributionAlgorithm = distributionAlgorithm;
        this.peerHandler = peerHandler;
    }


    @Override
    public PeerId getPeerId() {
        return peerId;
    }

    @Override
    public CompletableFuture<?> getInitFuture() {
        return initFuture;
    }

    @Override
    public CompletableFuture<?> getCloseFuture() {
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
        getDataBase().flush();
    }

    @Override
    public DataBase getDataBase() {
        return dataBase;
    }
}
