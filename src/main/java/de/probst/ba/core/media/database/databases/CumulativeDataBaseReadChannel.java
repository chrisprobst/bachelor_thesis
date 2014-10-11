package de.probst.ba.core.media.database.databases;

import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.DataBaseChannel;
import de.probst.ba.core.media.database.DataBaseReadChannel;
import de.probst.ba.core.media.database.DataInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 09.10.14.
 */
public final class CumulativeDataBaseReadChannel implements DataBaseReadChannel {

    private final DataBase dataBase;
    private final List<DataBaseReadChannel> readChannels;
    private int index;

    protected final void checkClosed() throws ClosedChannelException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
    }

    public CumulativeDataBaseReadChannel(DataBase dataBase, Collection<DataBaseReadChannel> readChannels) {
        Objects.requireNonNull(dataBase);
        Objects.requireNonNull(readChannels);
        if (readChannels.isEmpty()) {
            throw new IllegalArgumentException("readChannels.isEmpty()");
        }
        this.dataBase = dataBase;
        this.readChannels = new ArrayList<>(readChannels);
    }

    @Override
    public DataBase getDataBase() {
        return dataBase;
    }

    @Override
    public DataInfo getDataInfo() {
        return readChannels.get(0).getDataInfo();
    }

    @Override
    public boolean isCumulative() {
        return true;
    }

    @Override
    public List<DataInfo> getCumulativeDataInfo() {
        return readChannels.stream()
                           .map(DataBaseChannel::getCumulativeDataInfo)
                           .flatMap(List::stream)
                           .collect(Collectors.toList());
    }

    @Override
    public synchronized long size() throws IOException {
        checkClosed();
        long accumulator = 0;
        for (DataBaseReadChannel readChannel : readChannels) {
            accumulator += readChannel.size();
        }
        return accumulator;
    }

    @Override
    public synchronized long position() throws IOException {
        checkClosed();
        long accumulator = 0;
        for (int i = 0; i < index; i++) {
            accumulator += readChannels.get(i).position();
        }
        return accumulator + readChannels.get(index).position();
    }

    @Override
    public synchronized DataBaseReadChannel position(long position) throws IOException {
        if (position < 0) {
            throw new IllegalArgumentException("position < 0");
        } else if (position > size()) {
            throw new IllegalArgumentException("position > size()");
        }

        checkClosed();
        index = 0;
        long accumulator = 0;
        for (DataBaseReadChannel readChannel : readChannels) {
            long size = readChannel.size();

            if (position < accumulator + size) {
                position -= accumulator;
                readChannel.position(position);
                break;
            } else {
                readChannel.position(size);
                accumulator += size;
                index++;
            }
        }

        return this;
    }

    @Override
    public synchronized int read(ByteBuffer dst) throws IOException {
        checkClosed();
        DataBaseReadChannel readChannel = readChannels.get(index);
        int read = readChannel.read(dst);
        if (read == -1) {
            if (index >= readChannels.size() - 1) {
                return -1;
            } else {
                readChannels.get(++index).position(0);
                return read(dst);
            }
        }
        return read;
    }

    @Override
    public synchronized long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        checkClosed();
        long totalRead = 0;
        for (int i = 0; i < length; i++) {
            ByteBuffer byteBuffer = dsts[offset + i];

            int read = read(byteBuffer);
            if (read == -1) {
                if (totalRead > 0) {
                    return totalRead;
                } else {
                    return -1;
                }
            }

            totalRead += read;
            if (byteBuffer.hasRemaining()) {
                break;
            }
        }
        return totalRead;
    }

    @Override
    public long read(ByteBuffer[] dsts) throws IOException {
        return read(dsts, 0, dsts.length);
    }

    @Override
    public synchronized boolean isOpen() {
        return readChannels.stream().allMatch(Channel::isOpen);
    }

    @Override
    public synchronized void close() throws IOException {
        for (Channel channel : readChannels) {
            channel.close();
        }
    }
}
