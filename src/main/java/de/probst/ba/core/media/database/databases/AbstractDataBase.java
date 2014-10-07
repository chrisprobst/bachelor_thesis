package de.probst.ba.core.media.database.databases;

import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.database.DataInfoRegionLock;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 15.08.14.
 */
public abstract class AbstractDataBase implements DataBase {

    private final Map<String, DataInfo> dataInfo = new HashMap<>();
    private volatile Map<String, DataInfo> nonEmptyDataInfoView = Collections.emptyMap();

    private final DataInfoRegionLock dataInfoRegionLock = new DataInfoRegionLock();
    private final Map<DataInfo, GatheringByteChannel> insertChannels = new HashMap<>();
    private final Map<DataInfo, ScatteringByteChannel> queryChannels = new HashMap<>();

    private synchronized void unsafeUpdate(DataInfo updateDataInfo) {
        dataInfo.merge(updateDataInfo.getHash(), updateDataInfo, DataInfo::union);
        nonEmptyDataInfoView = Collections.unmodifiableMap(dataInfo.entrySet()
                                                                   .stream()
                                                                   .filter(p -> !p.getValue().isEmpty())
                                                                   .collect(Collectors.toMap(Map.Entry::getKey,
                                                                                             Map.Entry::getValue)));
    }

    protected GatheringByteChannel openWriteChannel(DataInfo writeDataInfo) throws IOException {
        return new GatheringDataBaseChannel(() -> dataInfoRegionLock.unlockWriteResource(writeDataInfo),
                                            () -> unsafeUpdate(writeDataInfo),
                                            writeDataInfo.getCompletedSize());
    }

    protected ScatteringByteChannel openReadChannel(DataInfo readDataInfo) throws IOException {
        return new ScatteringDataBaseChannel(readDataInfo.getCompletedSize());
    }

    @Override
    public Map<String, DataInfo> getDataInfo() {
        return nonEmptyDataInfoView;
    }

    @Override
    public synchronized DataInfo get(String hash) {
        return dataInfo.get(hash);
    }

    @Override
    public synchronized Optional<GatheringByteChannel> tryOpenWriteChannel(DataInfo writeDataInfo) throws IOException {
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
            return Optional.of(openWriteChannel(writeDataInfo));
        } catch (IOException e) {
            dataInfoRegionLock.unlockWriteResource(writeDataInfo);
            throw e;
        }
    }

    @Override
    public synchronized Optional<ScatteringByteChannel> tryOpenReadChannel(DataInfo readDataInfo) throws IOException {
        DataInfo existingDataInfo = dataInfo.get(readDataInfo.getHash());
        if (existingDataInfo == null) {
            throw new IllegalArgumentException("existingDataInfo == null");
        } else if (!existingDataInfo.contains(readDataInfo)) {
            throw new IllegalArgumentException("!existingDataInfo.contains(readDataInfo)");
        } else if (!dataInfoRegionLock.tryLockReadResource(readDataInfo)) {
            return Optional.empty();
        } else {
            try {
                return Optional.of(openReadChannel(readDataInfo));
            } catch (IOException e) {
                dataInfoRegionLock.unlockReadResource(readDataInfo);
                throw e;
            }
        }
    }

    @Override
    public void flush() throws IOException {
    }
}
