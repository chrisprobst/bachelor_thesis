package de.probst.ba.core.logic;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Immutable view of the network state.
 * <p>
 * Created by chrisprobst on 10.08.14.
 */
public final class NetworkState implements Serializable {

    private final long peerId;
    private final Map<Long, Transfer> uploads;
    private final Map<Long, Transfer> downloads;
    private final Map<String, DataInfo> dataInfo;
    private final Map<Long, Map<String, DataInfo>> remoteDataInfo;

    public NetworkState(long peerId,
                        Map<Long, Transfer> uploads,
                        Map<Long, Transfer> downloads,
                        Map<String, DataInfo> dataInfo,
                        Map<Long, Map<String, DataInfo>> remoteDataInfo) {
        this.peerId = peerId;
        this.uploads = uploads;
        this.downloads = downloads;
        this.dataInfo = dataInfo;
        this.remoteDataInfo = remoteDataInfo;
    }

    public long getPeerId() {
        return peerId;
    }

    /**
     * @return A combined view which takes pending
     * downloads and already available data info into account.
     */
    public Map<String, DataInfo> getEstimatedDataInfo() {
        // Our own data info (create a copy for manipulation)
        Map<String, DataInfo> dataInfo = new HashMap<>(getDataInfo());

        // Group all pending data info by hash
        Map<String, List<DataInfo>> estimatedDataInfo = getDownloads().values().stream()
                .map(Transfer::getDataInfo)
                .collect(Collectors.groupingBy(DataInfo::getHash));

        // Make a union of all pending data info to get an estimation
        // of all data info which will be available in the future
        Map<String, DataInfo> flatEstimatedDataInfo = estimatedDataInfo.entrySet().stream()
                .collect(Collectors.toMap(
                        p -> p.getKey(),
                        p -> p.getValue().stream().reduce(DataInfo::union).get()));

        // Merge with already available data info
        flatEstimatedDataInfo.forEach((k, v) -> dataInfo.merge(k, v, DataInfo::union));

        return dataInfo;
    }

    /**
     * @return All pending uploads.
     */
    public Map<Long, Transfer> getUploads() {
        return uploads;
    }

    /**
     * @return All pending downloads.
     */
    public Map<Long, Transfer> getDownloads() {
        return downloads;
    }

    /**
     * @return All local data info already saved.
     */
    public Map<String, DataInfo> getDataInfo() {
        return dataInfo;
    }

    /**
     * @return All known remote data info.
     */
    public Map<Long, Map<String, DataInfo>> getRemoteDataInfo() {
        return remoteDataInfo;
    }
}
