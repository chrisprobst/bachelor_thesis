package de.probst.ba.core.media.database.databases;

import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.DataBaseReadChannel;
import de.probst.ba.core.media.database.DataBaseWriteChannel;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.database.DataInfoRegionLock;
import de.probst.ba.core.util.collections.Tuple;
import de.probst.ba.core.util.collections.Tuple2;

import java.io.IOException;
import java.nio.channels.Channel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 15.08.14.
 */
public abstract class AbstractDataBase implements DataBase {

    private final Map<String, DataInfo> dataInfo = new HashMap<>();
    private final DataInfoRegionLock dataInfoRegionLock = new DataInfoRegionLock();
    private final Map<DataInfo, AbstractDataBaseWriteChannel> writeChannels = new HashMap<>();
    private final Map<DataInfo, AbstractDataBaseReadChannel> readChannels = new HashMap<>();

    final synchronized void update(DataInfo updateDataInfo) {
        dataInfo.merge(updateDataInfo.getHash(), updateDataInfo, DataInfo::union);
    }

    final synchronized void unregisterChannel(Channel channel, DataInfo channelDataInfo) {
        if (channel instanceof AbstractDataBaseWriteChannel) {
            writeChannels.remove(channelDataInfo);
            dataInfoRegionLock.unlockWriteResource(channelDataInfo);
        } else if (channel instanceof AbstractDataBaseReadChannel) {
            readChannels.remove(channelDataInfo);
            dataInfoRegionLock.unlockReadResource(channelDataInfo);
        } else {
            throw new IllegalArgumentException("Unknown channel type: " + channel);
        }
    }

    protected final synchronized Map<DataInfo, AbstractDataBaseWriteChannel> unsafeGetWriteChannels() {
        return new HashMap<>(writeChannels);
    }

    protected final synchronized Map<DataInfo, AbstractDataBaseReadChannel> unsafeGetReadChannels() {
        return new HashMap<>(readChannels);
    }

    protected abstract AbstractDataBaseWriteChannel openWriteChannel(DataInfo writeDataInfo) throws IOException;

    protected abstract AbstractDataBaseReadChannel openReadChannel(DataInfo readDataInfo) throws IOException;

    @Override
    public final synchronized Map<String, DataInfo> getDataInfo() {
        return Collections.unmodifiableMap(dataInfo.entrySet()
                                                   .stream()
                                                   .filter(p -> !p.getValue().isEmpty())
                                                   .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    @Override
    public final synchronized Map<String, DataInfo> getEstimatedDataInfo() {
        // Create a map of all locked write regions
        Map<String, DataInfo> lockedWriteRegions = dataInfoRegionLock.getLockedWriteRegions()
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
            lockedWriteRegions.merge(dataInfo.getHash(), dataInfo, DataInfo::union);
        }

        return lockedWriteRegions;
    }

    @Override
    public final synchronized DataInfo get(String hash) {
        return dataInfo.get(hash);
    }

    @Override
    public final synchronized Optional<DataBaseWriteChannel> insert(DataInfo writeDataInfo) throws IOException {
        DataInfo existingDataInfo = dataInfo.get(writeDataInfo.getHash());
        if (existingDataInfo == null) {
            // The write data info does not exist, lets add it!
            dataInfo.put(writeDataInfo.getHash(), writeDataInfo.empty());
            dataInfoRegionLock.lockWriteResource(writeDataInfo);

        } else if (existingDataInfo.overlaps(writeDataInfo)) {
            throw new IllegalArgumentException("existingDataInfo.overlaps(writeDataInfo)");
        } else if (!dataInfoRegionLock.tryLockWriteResource(writeDataInfo)) {
            return Optional.empty();
        }

        // Return a channel for writing
        try {
            // Open write channel and store into map
            AbstractDataBaseWriteChannel writeChannel = openWriteChannel(writeDataInfo);
            writeChannels.put(writeDataInfo, writeChannel);
            return Optional.of(writeChannel);
        } catch (IOException e) {
            dataInfoRegionLock.unlockWriteResource(writeDataInfo);
            throw e;
        }
    }

    @Override
    public final synchronized Optional<DataBaseReadChannel> lookup(DataInfo readDataInfo) throws IOException {
        DataInfo existingDataInfo = dataInfo.get(readDataInfo.getHash());
        if (existingDataInfo == null) {
            throw new IllegalArgumentException("existingDataInfo == null");
        } else if (!existingDataInfo.contains(readDataInfo)) {
            throw new IllegalArgumentException("!existingDataInfo.contains(readDataInfo)");
        } else if (!dataInfoRegionLock.tryLockReadResource(readDataInfo)) {
            return Optional.empty();
        } else {
            try {
                // Open read channel and store into map
                AbstractDataBaseReadChannel readChannel = openReadChannel(readDataInfo);
                readChannels.put(readDataInfo, readChannel);
                return Optional.of(readChannel);
            } catch (IOException e) {
                dataInfoRegionLock.unlockReadResource(readDataInfo);
                throw e;
            }
        }
    }

    @Override
    public synchronized Optional<Map<DataInfo, DataBaseReadChannel>> lookupMany(List<DataInfo> lookupDataInfo)
            throws IOException {
        Map<DataInfo, DataBaseReadChannel> founds = new LinkedHashMap<>();
        for (DataInfo dataInfo : lookupDataInfo) {
            Optional<DataBaseReadChannel> readChannel;

            try {
                readChannel = lookup(dataInfo);
            } catch (IOException e) {
                for (Channel channel : founds.values()) {
                    try {
                        channel.close();
                    } catch (IOException e1) {
                        e.addSuppressed(e1);
                    }
                }
                throw e;
            }

            if (readChannel.isPresent()) {
                founds.put(dataInfo, readChannel.get());
            } else {
                IOException any = null;
                for (Channel channel : founds.values()) {
                    try {
                        channel.close();
                    } catch (IOException e1) {
                        if (any == null) {
                            any = e1;
                        } else {
                            any.addSuppressed(e1);
                        }
                    }
                }
                if (any != null) {
                    throw any;
                }
                return Optional.empty();
            }
        }

        return Optional.of(founds);
    }

    @Override
    public synchronized final Optional<Tuple2<DataInfo, DataBaseReadChannel>> findAny(Predicate<DataInfo> predicate)
            throws IOException {
        Optional<DataInfo> foundDataInfo = dataInfo.values().stream().filter(predicate).findAny();
        if (foundDataInfo.isPresent()) {
            Optional<DataBaseReadChannel> foundReadChannel = lookup(foundDataInfo.get());
            return foundReadChannel.map(channel -> Tuple.of(foundDataInfo.get(), channel));
        } else {
            throw new IOException("No data info found, which fulfills the predicate");
        }
    }

    @Override
    public synchronized Optional<Map<DataInfo, DataBaseReadChannel>> findMany(Predicate<DataInfo> predicate)
            throws IOException {
        List<DataInfo> foundDataInfo = dataInfo.values().stream().filter(predicate).collect(Collectors.toList());
        if (foundDataInfo.isEmpty()) {
            throw new IOException("No data info found, which fulfills the predicate");
        } else {
            return lookupMany(foundDataInfo);
        }
    }

    @Override
    public synchronized Optional<DataBaseReadChannel> findIncremental(Predicate<DataInfo> predicate)
            throws IOException {
        List<DataInfo> foundDataInfo =
                dataInfo.values().stream().filter(predicate).sorted(Comparator.comparing(DataInfo::getId)).collect(
                        Collectors.toList());
        if (foundDataInfo.isEmpty()) {
            throw new IOException("No data info found, which fulfills the predicate");
        }

        // Collect all in-order data info
        List<DataInfo> inOrderDataInfo = new ArrayList<>();
        OptionalLong id = OptionalLong.empty();
        for (DataInfo dataInfo : foundDataInfo) {
            if (!id.isPresent() || id.getAsLong() + 1 == dataInfo.getId()) {
                id = OptionalLong.of(dataInfo.getId());
                inOrderDataInfo.add(dataInfo);
            } else {
                break;
            }
        }

        return lookupMany(inOrderDataInfo).map(Map::values).map(CumulativeDataBaseReadChannel::new);
    }
}
