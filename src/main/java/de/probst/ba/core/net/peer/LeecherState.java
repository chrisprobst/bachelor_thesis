package de.probst.ba.core.net.peer;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.transfer.Transfer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 01.09.14.
 */
public final class LeecherState extends PeerState {

    // The estimated data info
    private final Map<String, DataInfo> estimatedDataInfo;

    // The estimated missing remote data info
    private final Map<PeerId, Map<String, DataInfo>> estimatedMissingRemoteDataInfo;

    // All known remote data info
    private final Map<PeerId, Map<String, DataInfo>> remoteDataInfo;

    // All pending downloads
    private final Map<PeerId, Transfer> downloads;

    // The download rate
    private final long downloadRate;

    private Map<String, DataInfo> createEstimatedDataInfo() {
        // Our own data info (create a copy for manipulation)
        Map<String, DataInfo> dataInfo = new HashMap<>(getDataInfo());

        // Make a union of all pending data info to get an estimation
        // of all data info which will be available in the future
        Map<String, DataInfo> flatEstimatedDataInfo = getDownloads().values().stream()
                .map(Transfer::getDataInfo)
                .collect(Collectors.groupingBy(DataInfo::getHash)).entrySet().stream()
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

    public LeecherState(PeerId peerId,
                        Map<String, DataInfo> dataInfo,
                        Map<PeerId, Map<String, DataInfo>> remoteDataInfo,
                        Map<PeerId, Transfer> downloads,
                        long downloadRate) {
        super(peerId, dataInfo);

        Objects.requireNonNull(downloads);
        Objects.requireNonNull(remoteDataInfo);

        this.downloads = Collections.unmodifiableMap(new HashMap<>(downloads));
        this.remoteDataInfo = Collections.unmodifiableMap(remoteDataInfo.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        p -> Collections.unmodifiableMap(new HashMap<>(p.getValue())))));
        this.downloadRate = downloadRate;

        // Calc the estimated data info
        estimatedDataInfo = createEstimatedDataInfo();

        // Calc the estimated missing remote data info
        estimatedMissingRemoteDataInfo = createEstimatedMissingRemoteDataInfo();
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
     * @return All pending downloads.
     */
    public Map<PeerId, Transfer> getDownloads() {
        return downloads;
    }

    /**
     * @return All known remote data info.
     */
    public Map<PeerId, Map<String, DataInfo>> getRemoteDataInfo() {
        return remoteDataInfo;
    }

    /**
     * @return The download rate.
     */
    public long getDownloadRate() {
        return downloadRate;
    }
}
