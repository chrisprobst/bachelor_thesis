package de.probst.ba.core.net.peer;

import de.probst.ba.core.diag.Diagnostic;
import de.probst.ba.core.logic.Body;
import de.probst.ba.core.logic.Brain;
import de.probst.ba.core.logic.BrainWorker;
import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.NetworkState;
import de.probst.ba.core.net.Transfer;
import de.probst.ba.core.util.concurrent.AtomicCounter;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Created by chrisprobst on 17.08.14.
 */
public abstract class AbstractPeer implements Peer {

    private final PeerId localPeerId;

    private final DataBase dataBase;

    private final Brain brain;

    private final Diagnostic diagnostic;

    private final BrainWorker brainWorker =
            new BrainWorker(getBody());

    private final CompletableFuture<?> initFuture =
            new CompletableFuture<>();

    private final AtomicCounter parallelUploads =
            new AtomicCounter();

    protected AtomicCounter getParallelUploads() {
        return parallelUploads;
    }

    protected BrainWorker getBrainWorker() {
        return brainWorker;
    }

    protected PeerId getLocalPeerId() {
        return localPeerId;
    }

    protected abstract Map<PeerId, Transfer> getUploads();

    protected abstract Map<PeerId, Transfer> getDownloads();

    protected abstract Map<String, DataInfo> getDataInfo();

    protected abstract Map<PeerId, Map<String, DataInfo>> getRemoteDataInfo();

    protected abstract long getUploadRate();

    protected abstract long getDownloadRate();

    protected abstract Body getBody();

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
        return new NetworkState(
                getLocalPeerId(),
                getDataInfo(),
                getRemoteDataInfo(),
                getUploads(),
                getDownloads(),
                getUploadRate(),
                getDownloadRate());
    }

    @Override
    public DataBase getDataBase() {
        return dataBase;
    }
}
