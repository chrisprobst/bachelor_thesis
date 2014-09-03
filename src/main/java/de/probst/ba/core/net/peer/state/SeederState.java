package de.probst.ba.core.net.peer.state;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.transfer.Transfer;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by chrisprobst on 01.09.14.
 */
public final class SeederState extends DataInfoState {

    // All pending uploads
    private final Map<PeerId, Transfer> uploads;

    public SeederState(Seeder seeder,
                       Map<String, DataInfo> dataInfo,
                       Map<PeerId, Transfer> uploads) {

        super(seeder, dataInfo);

        Objects.requireNonNull(uploads);

        this.uploads = Collections.unmodifiableMap(new HashMap<>(uploads));
    }

    /**
     * @return All pending uploads.
     */
    public Map<PeerId, Transfer> getUploads() {
        return uploads;
    }

    @Override
    public Seeder getPeer() {
        return (Seeder) super.getPeer();
    }
}
