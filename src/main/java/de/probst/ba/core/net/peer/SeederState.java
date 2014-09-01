package de.probst.ba.core.net.peer;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.transfer.Transfer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by chrisprobst on 01.09.14.
 */
public final class SeederState extends PeerState {

    // All pending uploads
    private final Map<PeerId, Transfer> uploads;

    // The upload rate
    private final long uploadRate;

    public SeederState(PeerId peerId,
                       Map<String, DataInfo> dataInfo,
                       Map<PeerId, Transfer> uploads,
                       long uploadRate) {

        super(peerId, dataInfo);

        Objects.requireNonNull(uploads);

        this.uploads = Collections.unmodifiableMap(new HashMap<>(uploads));
        this.uploadRate = uploadRate;
    }

    /**
     * @return All pending uploads.
     */
    public Map<PeerId, Transfer> getUploads() {
        return uploads;
    }

    /**
     * @return The upload rate.
     */
    public long getUploadRate() {
        return uploadRate;
    }
}
