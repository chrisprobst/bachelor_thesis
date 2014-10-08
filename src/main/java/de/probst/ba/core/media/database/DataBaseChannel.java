package de.probst.ba.core.media.database;

import java.io.IOException;
import java.nio.channels.Channel;

/**
 * Created by chrisprobst on 08.10.14.
 */
public interface DataBaseChannel extends Channel {

    long size() throws IOException;

    long position() throws IOException;

    DataBaseChannel position(long position) throws IOException;
}
