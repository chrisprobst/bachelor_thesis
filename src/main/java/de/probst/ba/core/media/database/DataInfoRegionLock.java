package de.probst.ba.core.media.database;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by chrisprobst on 07.10.14.
 */
public final class DataInfoRegionLock {

    private final List<DataInfo> writeResources = new LinkedList<>();
    private final List<DataInfo> readResources = new LinkedList<>();

    private boolean overlapsAnyInCollection(Collection<DataInfo> dataInfoCollection, DataInfo dataInfo) {
        return dataInfoCollection.stream().filter(dataInfo::isCompatibleWith).anyMatch(dataInfo::overlaps);
    }

    public void lockWriteResource(DataInfo dataInfo) {
        if (!tryLockWriteResource(dataInfo)) {
            throw new IllegalStateException("!tryLockWriteResource(dataInfo)");
        }
    }

    public synchronized boolean tryLockWriteResource(DataInfo dataInfo) {
        if (overlapsAnyInCollection(writeResources, dataInfo) || overlapsAnyInCollection(readResources, dataInfo)) {
            return false;
        }
        writeResources.add(dataInfo);
        return true;
    }

    public synchronized boolean tryUnlockWriteResource(DataInfo dataInfo) {
        return writeResources.remove(dataInfo);
    }

    public void unlockWriteResource(DataInfo dataInfo) {
        if (!tryUnlockWriteResource(dataInfo)) {
            throw new IllegalStateException("!tryUnlockWriteResource(dataInfo)");
        }
    }

    public void lockReadResource(DataInfo dataInfo) {
        if (!tryLockReadResource(dataInfo)) {
            throw new IllegalStateException("!tryLockReadResource(dataInfo)");
        }
    }

    public synchronized boolean tryLockReadResource(DataInfo dataInfo) {
        if (overlapsAnyInCollection(writeResources, dataInfo)) {
            return false;
        }
        readResources.add(dataInfo);
        return true;
    }

    public synchronized boolean tryUnlockReadResource(DataInfo dataInfo) {
        return readResources.remove(dataInfo);
    }

    public void unlockReadResource(DataInfo dataInfo) {
        if (!tryUnlockReadResource(dataInfo)) {
            throw new IllegalStateException("!tryUnlockReadResource(dataInfo)");
        }
    }
}
