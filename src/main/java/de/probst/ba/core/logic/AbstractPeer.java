package de.probst.ba.core.logic;

import java.util.Map;

/**
 * Created by chrisprobst on 11.08.14.
 */
public abstract class AbstractPeer implements Peer {

    private final Object peerId;

    protected AbstractPeer(Object peerId) {
        this.peerId = peerId;
    }

    protected abstract long getDownloadRate();

    protected abstract long getUploadRate();

    protected abstract Map<Object, Transfer> getUploads();

    protected abstract Map<Object, Transfer> getDownloads();

    protected abstract Map<String, DataInfo> getDataInfo();

    protected abstract Map<Object, Map<String, DataInfo>> getRemoteDataInfo();

    @Override
    public PeerState getPeerState() {
        return new PeerState(
                getUploads(),
                getDownloads(),
                getDataInfo(),
                getRemoteDataInfo(),
                getDownloadRate(),
                getUploadRate());
    }

    @Override
    public Object getPeerId() {
        return peerId;
    }
}
