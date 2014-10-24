package de.probst.ba.core.media.database;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import java.util.Optional;

/**
 * Created by chrisprobst on 08.10.14.
 */
public interface DataBaseWriteChannel extends DataBaseChannel, GatheringByteChannel {

    Optional<DataInfo> getMergedDataInfo();

    @Override
    DataBaseWriteChannel position(long position) throws IOException;
}
