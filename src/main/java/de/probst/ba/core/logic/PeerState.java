package de.probst.ba.core.logic;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Immutable view of the peer state.
 * <p>
 * Created by chrisprobst on 10.08.14.
 */
public final class PeerState implements Serializable {

    // The id of this peer
    private final long peerId;

    // All pending uploads
    private final Map<Long, Transfer> uploads;

    // All pending downloads
    private final Map<Long, Transfer> downloads;

    // All already available local data info
    private final Map<String, DataInfo> dataInfo;

    // All known remote data info
    private final Map<Long, Map<String, DataInfo>> remoteDataInfo;

    public PeerState(long peerId,
                     Map<Long, Transfer> uploads,
                     Map<Long, Transfer> downloads,
                     Map<String, DataInfo> dataInfo,
                     Map<Long, Map<String, DataInfo>> remoteDataInfo) {
        Objects.requireNonNull(uploads);
        Objects.requireNonNull(downloads);
        Objects.requireNonNull(dataInfo);
        Objects.requireNonNull(remoteDataInfo);

        this.peerId = peerId;
        this.uploads = uploads;
        this.downloads = downloads;
        this.dataInfo = dataInfo;
        this.remoteDataInfo = remoteDataInfo;
    }

    /**
     * @return The peer id.
     */
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
     * @return All already available local data info.
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
