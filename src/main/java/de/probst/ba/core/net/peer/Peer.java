package de.probst.ba.core.net.peer;

import de.probst.ba.core.logic.Brain;
import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.NetworkState;
import de.probst.ba.core.net.Transfer;

import java.io.Closeable;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Created by chrisprobst on 15.08.14.
 */
public interface Peer extends Closeable {

    CompletableFuture<Void> getInitFuture();

    long getUploadRate();

    long getDownloadRate();

    Map<Object, Transfer> getUploads();

    Map<Object, Transfer> getDownloads();

    Map<String, DataInfo> getDataInfo();

    Map<Object, Map<String, DataInfo>> getRemoteDataInfo();

    SocketAddress getAddress();

    DataBase getDataBase();

    Brain getBrain();

    default NetworkState getNetworkState() {
        return new NetworkState(
                getUploads(),
                getDownloads(),
                getDataInfo(),
                getRemoteDataInfo(),
                getUploadRate(),
                getDownloadRate());
    }
}
