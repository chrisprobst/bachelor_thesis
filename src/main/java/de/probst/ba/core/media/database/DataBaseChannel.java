package de.probst.ba.core.media.database;

import java.io.IOException;
import java.nio.channels.Channel;
import java.util.List;

/**
 * Created by chrisprobst on 08.10.14.
 */
public interface DataBaseChannel extends Channel {

    DataInfo getDataInfo();

    boolean isCumulative();

    DataBase getDataBase();

    List<DataInfo> getCumulativeDataInfo();

    long size() throws IOException;

    long position() throws IOException;

    default long remaining() throws IOException {
        return size() - position();
    }

    default boolean isCompleted() throws IOException {
        return remaining() == 0;
    }

    DataBaseChannel position(long position) throws IOException;
}
