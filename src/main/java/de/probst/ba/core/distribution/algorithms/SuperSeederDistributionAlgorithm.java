package de.probst.ba.core.distribution.algorithms;

import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.transfer.TransferManager;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by chrisprobst on 11.09.14.
 */
public final class SuperSeederDistributionAlgorithm implements SeederDistributionAlgorithm {

    @Override
    public synchronized Map<String, DataInfo> transformUploadDataInfo(Seeder seeder,
                                                                      Map<String, DataInfo> dataInfo,
                                                                      PeerId remotePeerId) {
        if (dataInfo.isEmpty()) {
            return dataInfo;
        }

        if (!totallyEmpty) {
            Map<String, DataInfo> copy = new HashMap<>(dataInfo);
            alreadyUploaded.forEach(x -> {
                DataInfo extracted = copy.get(x.getHash()).subtract(x);
                if (extracted.isEmpty()) {
                    copy.remove(extracted.getHash());
                } else {
                    copy.put(extracted.getHash(), extracted);
                }
            });
            if (copy.isEmpty()) {
                totallyEmpty = true;
            }
            return copy;
        } else {
            return dataInfo;
        }
    }

    private boolean totallyEmpty = false;
    private final Set<DataInfo> alreadyUploaded = new HashSet<>();

    @Override
    public synchronized boolean isUploadAllowed(Seeder seeder, TransferManager transferManager) {
        if (totallyEmpty) {
            return true;
        }
        return alreadyUploaded.add(transferManager.getTransfer().getDataInfo());
    }
}
