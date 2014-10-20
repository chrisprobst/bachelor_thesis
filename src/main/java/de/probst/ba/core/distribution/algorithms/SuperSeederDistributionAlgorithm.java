package de.probst.ba.core.distribution.algorithms;

import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.Transfer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by chrisprobst on 11.09.14.
 */
public final class SuperSeederDistributionAlgorithm implements SeederDistributionAlgorithm {

    private final Set<DataInfo> alreadyUploaded = new HashSet<>();

    public synchronized DataInfo removeAlreadyUploaded(DataInfo dataInfo) {
        Objects.requireNonNull(dataInfo);
        return alreadyUploaded.stream()
                              .filter(x -> dataInfo.getHash().equals(x.getHash()))
                              .reduce(DataInfo::union)
                              .map(dataInfo::subtract)
                              .orElse(dataInfo);
    }

    public synchronized Map<String, DataInfo> removeAlreadyUploaded(Map<String, DataInfo> dataInfo) {
        Objects.requireNonNull(dataInfo);

        if (dataInfo.isEmpty()) {
            return dataInfo;
        }

        Map<String, DataInfo> copy = new HashMap<>(dataInfo);
        alreadyUploaded.forEach(x -> {
            DataInfo extracted = copy.get(x.getHash()).subtract(x);
            if (extracted.isEmpty()) {
                copy.remove(extracted.getHash());
            } else {
                copy.put(extracted.getHash(), extracted);
            }
        });
        return copy;
    }

    public boolean isSuperSeederDistributionAlgorithm() {
        return true;
    }

    @Override
    public synchronized Map<String, DataInfo> transformUploadDataInfo(Seeder seeder,
                                                                      Map<String, DataInfo> dataInfo,
                                                                      PeerId remotePeerId) {
/*
        // All running uploads
        Set<DataInfo> uploading = seeder.getDataInfoState()
                                        .getUploads()
                                        .values()
                                        .stream()
                                        .map(Transfer::getDataInfo)
                                        .collect(Collectors.toSet());

        Map<String, DataInfo> reallyUploaded = alreadyUploaded.stream()
                                                              .filter(x -> !uploading.contains(x))
                                                              .collect(Collectors.groupingBy(DataInfo::getHash))
                                                              .values()
                                                              .stream()
                                                              .map(x -> x.stream().reduce(DataInfo::union).get())
                                                              .collect(Collectors.toMap(DataInfo::getHash,
                                                                                        Function.identity()));

        // Group by name
        Map<String, List<DataInfo>> grouped = dataInfo.values()
                                                      .stream()
                                                      .filter(x -> x.getName().isPresent())
                                                      .collect(Collectors.groupingBy(x -> x.getName().get()));
        Map<String, List<DataInfo>> groupedReallyUploaded = reallyUploaded.values()
                                                                          .stream()
                                                                          .filter(x -> x.getName()
                                                                                        .isPresent())
                                                                          .collect(
                                                                                  Collectors.groupingBy(x -> x.getName()
                                                                                                              .get()));
        // Sort by id
        grouped.values().forEach(x -> x.sort(Comparator.comparing(DataInfo::getId)));
        groupedReallyUploaded.values().forEach(x -> x.sort(Comparator.comparing(DataInfo::getId).reversed()));


        for (String name : new ArrayList<>(grouped.keySet())) {
            List<DataInfo> removed = null;
            List<DataInfo> existing = grouped.get(name);
            List<DataInfo> uploaded = groupedReallyUploaded.get(name);
            if (uploaded == null) {
                grouped.put(name, existing.subList(0, 1));
                continue;
            }

            for (int i = 0; i < uploaded.size(); i++) {
                if (!uploaded.get(i).isCompleted()) {
                    removed = existing.subList(0, i + 1);
                    break;
                }
            }
            if (removed != null) {
                grouped.put(name, removed);
            }
        }

        return removeAlreadyUploaded(grouped.values()
                                            .stream()
                                            .flatMap(List::stream)
                                            .collect(Collectors.toMap(DataInfo::getHash, Function.identity())));*/
        return removeAlreadyUploaded(dataInfo);
    }

    @Override
    public synchronized boolean isUploadAllowed(Seeder seeder, Transfer transfer) {
        return alreadyUploaded.add(transfer.getDataInfo());
    }
}
