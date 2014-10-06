package de.probst.ba.core.media.database.databases;

import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.DataInfo;

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

    private synchronized void unsafePut(DataInfo dataInfo) {
        this.dataInfo.merge(dataInfo.getHash(), dataInfo, DataInfo::union);
        createNonEmptyDataInfoView();
    }

    private void createNonEmptyDataInfoView() {
        nonEmptyDataInfoView = Collections.unmodifiableMap(dataInfo.entrySet()
                                                                   .stream()
                                                                   .filter(p -> !p.getValue().isEmpty())
                                                                   .collect(Collectors.toMap(Map.Entry::getKey,
                                                                                             Map.Entry::getValue)));
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
    public synchronized Optional<GatheringByteChannel> tryInsert(DataInfo dataInfo) throws IOException {
        DataInfo existingDataInfo = this.dataInfo.get(dataInfo.getHash());
        if (existingDataInfo == null) {
            this.dataInfo.put(dataInfo.getHash(), dataInfo.empty());
            return Optional.of(new GatheringDataBaseChannel(() -> unsafePut(dataInfo), dataInfo.getCompletedSize()));
        } else if (existingDataInfo.overlaps(dataInfo)) {
            throw new IllegalArgumentException("existingDataInfo.overlaps(dataInfo)");
        } else {
            return Optional.of(new GatheringDataBaseChannel(() -> unsafePut(dataInfo), dataInfo.getCompletedSize()));
        }
    }

    @Override
    public synchronized Optional<ScatteringByteChannel> tryQuery(DataInfo dataInfo) throws IOException {
        DataInfo existingDataInfo = this.dataInfo.get(dataInfo.getHash());
        if (existingDataInfo == null) {
            throw new IllegalArgumentException("existingDataInfo == null");
        } else if (!existingDataInfo.contains(dataInfo)) {
            throw new IllegalArgumentException("!existingDataInfo.contains(dataInfo)");
        } else {
            return Optional.of(new ScatteringDataBaseChannel(dataInfo.getCompletedSize()));
        }
    }

    @Override
    public void flush() throws IOException {
    }
}
