package de.probst.ba.core.net;

import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.peer.PeerId;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Immutable view of the networkstate.
 * <p>
 * Created by chrisprobst on 10.08.14.
 */
public final class NetworkState implements Serializable {

    // The local peer id
    private final PeerId localPeerId;

    // All pending uploads
    private final Map<PeerId, Transfer> uploads;

    // All pending downloads
    private final Map<PeerId, Transfer> downloads;

    // All already available local data info
    private final Map<String, DataInfo> dataInfo;

    // The estimated data info
    private final Map<String, DataInfo> estimatedDataInfo;

    // The estimated missing remote data info
    private final Map<PeerId, Map<String, DataInfo>> estimatedMissingRemoteDataInfo;

    // All known remote data info
    private final Map<PeerId, Map<String, DataInfo>> remoteDataInfo;

    // The upload rate
    private final long uploadRate;

    // The download rate
    private final long downloadRate;

    private Map<String, DataInfo> createEstimatedDataInfo() {
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

        return Collections.unmodifiableMap(dataInfo);
    }

    private Map<PeerId, Map<String, DataInfo>> createEstimatedMissingRemoteDataInfo() {
        return getRemoteDataInfo().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        p -> {
                            Map<String, DataInfo> remoteDataInfo = new HashMap<>(p.getValue());

                            for (String key : new ArrayList<>(remoteDataInfo.keySet())) {
                                DataInfo remote = remoteDataInfo.get(key);
                                DataInfo estimated = getEstimatedDataInfo().get(key);

                                if (estimated != null) {
                                    remote = remote.substract(estimated);
                                    if (remote.isEmpty()) {
                                        remoteDataInfo.remove(key);
                                    } else {
                                        remoteDataInfo.put(key, remote);
                                    }
                                }
                            }

                            return remoteDataInfo;
                        }
                )).entrySet().stream()
                .filter(p -> !p.getValue().isEmpty())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue));
    }

    /**
     * Creates a network state.
     *
     * @param localPeerId
     * @param dataInfo
     * @param remoteDataInfo
     * @param uploads
     * @param downloads
     * @param uploadRate
     * @param downloadRate
     */
    public NetworkState(PeerId localPeerId,
                        Map<String, DataInfo> dataInfo,
                        Map<PeerId, Map<String, DataInfo>> remoteDataInfo,
                        Map<PeerId, Transfer> uploads,
                        Map<PeerId, Transfer> downloads,
                        long uploadRate,
                        long downloadRate) {

        Objects.requireNonNull(localPeerId);
        Objects.requireNonNull(uploads);
        Objects.requireNonNull(downloads);
        Objects.requireNonNull(dataInfo);
        Objects.requireNonNull(remoteDataInfo);

        this.localPeerId = localPeerId;
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

        // Calc the estimated data info
        estimatedDataInfo = createEstimatedDataInfo();

        // Calc the estimated missing remote data info
        estimatedMissingRemoteDataInfo = createEstimatedMissingRemoteDataInfo();
    }

    /**
     * @return The lowest id of all uncompleted
     * data info.
     */
    public Optional<Long> getLowestUncompletedDataInfoId() {
        return getDataInfo().entrySet().stream()
                .filter(p -> !p.getValue().isCompleted())
                .sorted(Comparator.comparing(p -> p.getValue().getId()))
                .findFirst().map(p -> p.getValue().getId());
    }

    /**
     * @return A combined view which takes pending
     * downloads and already available data info into account.
     */
    public Map<String, DataInfo> getEstimatedDataInfo() {
        return estimatedDataInfo;
    }

    /**
     * @return A combined view of all missing data info which takes pending
     * downloads and already available data info into account.
     */
    public Map<PeerId, Map<String, DataInfo>> getEstimatedMissingRemoteDataInfo() {
        return estimatedMissingRemoteDataInfo;
    }

    /**
     * @return The local peer id.
     */
    public PeerId getLocalPeerId() {
        return localPeerId;
    }

    /**
     * @return All pending uploads.
     */
    public Map<PeerId, Transfer> getUploads() {
        return uploads;
    }

    /**
     * @return All pending downloads.
     */
    public Map<PeerId, Transfer> getDownloads() {
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
    public Map<PeerId, Map<String, DataInfo>> getRemoteDataInfo() {
        return remoteDataInfo;
    }


    /**
     * @return The upload rate.
     */
    public long getUploadRate() {
        return uploadRate;
    }

    /**
     * @return The download rate.
     */
    public long getDownloadRate() {
        return downloadRate;
    }
}
