package de.probst.ba.core.net.peer.state;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.transfer.Transfer;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.PeerId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 01.09.14.
 */
public final class LeecherDataInfoState extends DataInfoState {

    // The estimated data info
    private final Map<String, DataInfo> estimatedDataInfo;

    // The estimated missing remote data info
    private final Map<PeerId, Map<String, DataInfo>> estimatedMissingRemoteDataInfo;

    // All known remote data info
    private final Map<PeerId, Map<String, DataInfo>> remoteDataInfo;

    // All pending downloads
    private final Map<PeerId, Transfer> downloads;

    public LeecherDataInfoState(Leecher leecher,
                                Map<String, DataInfo> dataInfo,
                                Map<PeerId, Map<String, DataInfo>> remoteDataInfo,
                                Map<PeerId, Transfer> downloads) {
        super(leecher, dataInfo);
        Objects.requireNonNull(downloads);
        Objects.requireNonNull(remoteDataInfo);
        this.downloads = downloads;
        this.remoteDataInfo = remoteDataInfo;

        // Calc the estimated data info
        estimatedDataInfo = createEstimatedDataInfo();

        // Calc the estimated missing remote data info
        estimatedMissingRemoteDataInfo = createEstimatedMissingRemoteDataInfo();
    }

    private Map<String, DataInfo> createEstimatedDataInfo() {
        // Our own data info (create a copy for manipulation)
        Map<String, DataInfo> dataInfo = new HashMap<>(getDataInfo());

        // Make a union of all pending data info to get an estimation
        // of all data info which will be available in the future
        Map<String, DataInfo> flatEstimatedDataInfo =
                getDownloads().values()
                              .stream()
                              .map(Transfer::getDataInfo)
                              .collect(Collectors.groupingBy(DataInfo::getHash))
                              .entrySet()
                              .stream()
                              .collect(Collectors.toMap(Map.Entry::getKey,
                                                        p -> p.getValue()
                                                              .stream()
                                                              .reduce(DataInfo::union)
                                                              .get()));

        // Merge with already available data info
        flatEstimatedDataInfo.forEach((k, v) -> dataInfo.merge(k, v, DataInfo::union));

        return Collections.unmodifiableMap(dataInfo);
    }

    private Map<PeerId, Map<String, DataInfo>> createEstimatedMissingRemoteDataInfo() {
        return getRemoteDataInfo().entrySet()
                                  .stream()
                                  .collect(Collectors.toMap(Map.Entry::getKey, p -> {
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
                                  }))
                                  .entrySet()
                                  .stream()
                                  .filter(p -> !p.getValue().isEmpty())
                                  .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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

    @Override
    public Leecher getPeer() {
        return (Leecher) super.getPeer();
    }
}
