package de.probst.ba.core.net.peer;

import de.probst.ba.core.logic.Brain;
import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.net.NetworkState;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Created by chrisprobst on 15.08.14.
 */
public interface Peer extends AutoCloseable {

    CompletableFuture<?> getInitFuture();

    Future<?> getCloseFuture();

    void connect(SocketAddress remoteSocketAddress);

    NetworkState getNetworkState();

    DataBase getDataBase();

    Brain getBrain();

    @Override
    void close();
}
