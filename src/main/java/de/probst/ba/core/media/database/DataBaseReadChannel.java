package de.probst.ba.core.media.database;

import java.io.IOException;
import java.nio.channels.ScatteringByteChannel;

/**
 * Created by chrisprobst on 08.10.14.
 */
public interface DataBaseReadChannel extends DataBaseChannel, ScatteringByteChannel {

    @Override
    DataBaseReadChannel position(long position) throws IOException;
}
