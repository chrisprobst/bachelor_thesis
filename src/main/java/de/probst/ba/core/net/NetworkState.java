package de.probst.ba.core.net;

import de.probst.ba.core.media.DataInfo;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Immutable view of the networkstate.
 * <p>
 * Created by chrisprobst on 10.08.14.
 */
public final class NetworkState implements Serializable {

    // All pending uploads
    private final Map<Object, Transfer> uploads;

    // All pending downloads
    private final Map<Object, Transfer> downloads;

    // All already available local data info
    private final Map<String, DataInfo> dataInfo;

    // All known remote data info
    private final Map<Object, Map<String, DataInfo>> remoteDataInfo;

    // The download rate
    private final long downloadRate;

    // The upload rate
    private final long uploadRate;

    /**
     * Creates a network state.
     *
     * @param uploads
     * @param downloads
     * @param dataInfo
     * @param remoteDataInfo
     * @param uploadRate
     * @param downloadRate
     */
    public NetworkState(Map<Object, Transfer> uploads,
                        Map<Object, Transfer> downloads,
                        Map<String, DataInfo> dataInfo,
                        Map<Object, Map<String, DataInfo>> remoteDataInfo,
                        long uploadRate,
                        long downloadRate) {

        Objects.requireNonNull(uploads);
        Objects.requireNonNull(downloads);
        Objects.requireNonNull(dataInfo);
        Objects.requireNonNull(remoteDataInfo);

        this.uploads = Collections.unmodifiableMap(new HashMap<>(uploads));
        this.downloads = Collections.unmodifiableMap(new HashMap<>(downloads));
        this.dataInfo = Collections.unmodifiableMap(new HashMap<>(dataInfo));
        this.remoteDataInfo = Collections.unmodifiableMap(remoteDataInfo.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        p -> Collections.unmodifiableMap(
                                new HashMap<>(p.getValue())))));
        this.uploadRate = uploadRate;
        this.downloadRate = downloadRate;
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
                        Map.Entry::getKey,
                        p -> p.getValue().stream().reduce(DataInfo::union).get()));

        // Merge with already available data info
        flatEstimatedDataInfo.forEach((k, v) -> dataInfo.merge(k, v, DataInfo::union));

        return dataInfo;
    }



    /**
     * @return The download rate.
     */
    public long getDownloadRate() {
        return downloadRate;
    }

    /**
     * @return All pending uploads.
     */
    public Map<Object, Transfer> getUploads() {
        return uploads;
    }

    /**
     * @return All pending downloads.
     */
    public Map<Object, Transfer> getDownloads() {
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
    public Map<Object, Map<String, DataInfo>> getRemoteDataInfo() {
        return remoteDataInfo;
    }
}
