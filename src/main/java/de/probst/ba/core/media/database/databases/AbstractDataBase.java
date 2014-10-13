package de.probst.ba.core.media.database.databases;

import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.DataBaseReadChannel;
import de.probst.ba.core.media.database.DataBaseWriteChannel;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.database.DataInfoRegionRWLock;
import de.probst.ba.core.media.database.DataInsertException;
import de.probst.ba.core.media.database.DataLookupException;
import de.probst.ba.core.util.collections.Tuple;
import de.probst.ba.core.util.io.IOUtil;

import java.io.IOException;
import java.nio.channels.Channel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 15.08.14.
 */
public abstract class AbstractDataBase implements DataBase {

    private final Map<String, DataInfo> dataInfo = new HashMap<>();
    private final DataInfoRegionRWLock dataInfoRegionRWLock = new DataInfoRegionRWLock();
    private final Map<DataInfo, AbstractDataBaseWriteChannel> writeChannels = new HashMap<>();
    private final Map<DataInfo, AbstractDataBaseReadChannel> readChannels = new HashMap<>();
    private final boolean allowOverwrite;
    private boolean closed = false;

    synchronized final void merge(DataInfo mergeDataInfo) {
        dataInfo.merge(mergeDataInfo.getHash(), mergeDataInfo, DataInfo::union);
    }

    synchronized final void unregisterChannel(Channel channel, DataInfo channelDataInfo) {
        if (channel instanceof AbstractDataBaseWriteChannel) {
            writeChannels.remove(channelDataInfo);
            dataInfoRegionRWLock.unlockWriteResource(channelDataInfo);
        } else if (channel instanceof AbstractDataBaseReadChannel) {
            readChannels.remove(channelDataInfo);
            dataInfoRegionRWLock.unlockReadResource(channelDataInfo);
        } else {
            throw new IllegalArgumentException("Unknown channel type: " + channel);
        }
    }

    protected synchronized final Map<DataInfo, AbstractDataBaseWriteChannel> getWriteChannels() {
        return new HashMap<>(writeChannels);
    }

    protected synchronized final Map<DataInfo, AbstractDataBaseReadChannel> getReadChannels() {
        return new HashMap<>(readChannels);
    }

    protected abstract void doClose() throws IOException;

    protected abstract AbstractDataBaseWriteChannel openWriteChannel(DataInfo writeDataInfo) throws IOException;

    protected abstract AbstractDataBaseReadChannel openReadChannel(DataInfo readDataInfo) throws IOException;

    public AbstractDataBase(boolean allowOverwrite) {
        this.allowOverwrite = allowOverwrite;
    }

    @Override
    public final synchronized Map<String, DataInfo> getDataInfo() {
        return Collections.unmodifiableMap(dataInfo.entrySet()
                                                   .stream()
                                                   .filter(p -> !p.getValue().isEmpty())
                                                   .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    @Override
    public synchronized final Map<String, DataInfo> getEstimatedDataInfo() {
        // Create a map of all locked write regions
        Map<String, DataInfo> regions = dataInfoRegionRWLock.getLockedWriteRegions()
                                                            .stream()
                                                            .collect(Collectors.groupingBy(DataInfo::getHash))
                                                            .entrySet()
                                                            .stream()
                                                            .map(p -> Tuple.of(p.getKey(),
                                                                               p.getValue()
                                                                                .stream()
                                                                                .reduce(DataInfo::union)
                                                                                .get()))
                                                            .collect(Collectors.toMap(Tuple::first,
                                                                                      Tuple::second));

        // Get all non-empty data info and
        // merge with locked file regions
        for (DataInfo dataInfo : getDataInfo().values()) {
            regions.merge(dataInfo.getHash(), dataInfo, DataInfo::union);
        }

        return regions;
    }

    @Override
    public synchronized final DataInfo get(String hash) {
        Objects.requireNonNull(hash);
        return dataInfo.get(hash);
    }


    @Override
    public synchronized final Optional<DataBaseReadChannel> lookup(DataInfo readDataInfo) throws IOException {
        Objects.requireNonNull(readDataInfo);

        DataInfo existingDataInfo = dataInfo.get(readDataInfo.getHash());
        if (existingDataInfo == null) {
            throw new DataLookupException("existingDataInfo == null");
        } else if (!existingDataInfo.contains(readDataInfo)) {
            throw new DataLookupException("!existingDataInfo.contains(readDataInfo)");
        } else if (!dataInfoRegionRWLock.tryLockReadResource(readDataInfo)) {
            return Optional.empty();
        } else {
            try {
                // Open read channel and store into map
                AbstractDataBaseReadChannel readChannel = openReadChannel(readDataInfo);
                readChannels.put(readDataInfo, readChannel);
                return Optional.of(readChannel);
            } catch (IOException e) {
                dataInfoRegionRWLock.unlockReadResource(readDataInfo);
                throw e;
            }
        }
    }

    @Override
    public synchronized final Optional<DataBaseWriteChannel> insert(DataInfo writeDataInfo) throws IOException {
        Objects.requireNonNull(writeDataInfo);
        if (writeDataInfo.isEmpty()) {
            throw new DataInsertException("writeDataInfo.isEmpty()");
        }

        DataInfo existingDataInfo = dataInfo.get(writeDataInfo.getHash());
        if (existingDataInfo == null) {
            // The write data info does not exist, lets add it!
            dataInfo.put(writeDataInfo.getHash(), writeDataInfo.empty());
            dataInfoRegionRWLock.lockWriteResource(writeDataInfo);
        } else if (existingDataInfo.overlaps(writeDataInfo) && !allowOverwrite) {
            throw new DataInsertException("existingDataInfo.overlaps(writeDataInfo) && !allowOverwrite");
        } else if (!dataInfoRegionRWLock.tryLockWriteResource(writeDataInfo)) {
            return Optional.empty();
        }

        try {
            // Open write channel and store into map
            AbstractDataBaseWriteChannel writeChannel = openWriteChannel(writeDataInfo);
            writeChannels.put(writeDataInfo, writeChannel);
            return Optional.of(writeChannel);
        } catch (IOException e) {
            dataInfoRegionRWLock.unlockWriteResource(writeDataInfo);
            throw e;
        }
    }

    @Override
    public final void close() throws IOException {
        List<Channel> channels;
        synchronized (this) {
            if (!closed) {
                closed = true;
                channels = new ArrayList<>();
                channels.addAll(getReadChannels().values());
                channels.addAll(getWriteChannels().values());
            } else {
                return;
            }
        }
        try {
            IOUtil.closeAllAndThrow(channels);
        } finally {
            synchronized (this) {
                doClose();
            }
        }
    }
}
