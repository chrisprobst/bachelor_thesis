package de.probst.ba.core.net.peer;

import de.probst.ba.core.logic.Body;
import de.probst.ba.core.logic.Brain;
import de.probst.ba.core.logic.BrainWorker;
import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.NetworkState;
import de.probst.ba.core.net.Transfer;

import java.net.SocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Created by chrisprobst on 17.08.14.
 */
public abstract class AbstractPeer implements Peer {

    private final SocketAddress localAddress;

    private final DataBase dataBase;

    private final Brain brain;

    private final BrainWorker brainWorker =
            new BrainWorker(getBody());

    private final CompletableFuture<?> initFuture =
            new CompletableFuture<>();

    protected BrainWorker getBrainWorker() {
        return brainWorker;
    }

    protected SocketAddress getLocalAddress() {
        return localAddress;
    }

    protected abstract Map<Object, Transfer> getUploads();

    protected abstract Map<Object, Transfer> getDownloads();

    protected abstract Map<String, DataInfo> getDataInfo();

    protected abstract Map<Object, Map<String, DataInfo>> getRemoteDataInfo();

    protected abstract long getUploadRate();

    protected abstract long getDownloadRate();

    protected abstract Body getBody();

    protected AbstractPeer(SocketAddress localAddress,
                           DataBase dataBase,
                           Brain brain) {

        Objects.requireNonNull(localAddress);
        Objects.requireNonNull(dataBase);
        Objects.requireNonNull(brain);

        // Save args
        this.localAddress = localAddress;
        this.dataBase = dataBase;
        this.brain = brain;

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
    public NetworkState getNetworkState() {
        return new NetworkState(
                getLocalAddress(),
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
