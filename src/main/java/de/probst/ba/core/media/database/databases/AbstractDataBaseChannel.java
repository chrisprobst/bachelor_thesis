package de.probst.ba.core.media.database.databases;

import de.probst.ba.core.media.database.DataBaseChannel;
import de.probst.ba.core.media.database.DataInfo;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Created by chrisprobst on 08.10.14.
 */
public abstract class AbstractDataBaseChannel implements DataBaseChannel {

    private final AbstractDataBase dataBase;
    private final DataInfo dataInfo;
    private final long total;
    private long position;
    private boolean closed;

    protected final void checkClosed() throws ClosedChannelException {
        if (closed) {
            throw new ClosedChannelException();
        }
    }

    protected abstract void doClose() throws IOException;

    public AbstractDataBaseChannel(AbstractDataBase dataBase, DataInfo dataInfo) {
        Objects.requireNonNull(dataBase);
        Objects.requireNonNull(dataInfo);
        this.dataBase = dataBase;
        this.dataInfo = dataInfo;
        total = dataInfo.getCompletedSize();
    }

    @Override
    public final AbstractDataBase getDataBase() {
        return dataBase;
    }

    @Override
    public final boolean isCumulative() {
        return false;
    }

    @Override
    public final DataInfo getDataInfo() {
        return dataInfo;
    }

    @Override
    public final List<DataInfo> getCumulativeDataInfo() {
        return Arrays.asList(dataInfo);
    }

    @Override
    public final long size() throws IOException {
        checkClosed();
        return total;
    }

    @Override
    public final synchronized long position() throws IOException {
        checkClosed();
        return position;
    }

    @Override
    public synchronized DataBaseChannel position(long position) throws IOException {
        checkClosed();
        if (position < 0) {
            throw new IllegalArgumentException("position < 0");
        } else if (position > total) {
            throw new IllegalArgumentException("position > total");
        }
        this.position = position;
        return this;
    }

    @Override
    public final synchronized boolean isOpen() {
        return !closed;
    }

    @Override
    public final synchronized void close() throws IOException {
        if (!closed) {
            try {
                doClose();
            } finally {
                closed = true;
                dataBase.unregisterChannel(this, dataInfo);
            }
        }
    }
}
