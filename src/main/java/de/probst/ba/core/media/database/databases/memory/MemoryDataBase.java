package de.probst.ba.core.media.database.databases.memory;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.database.databases.AbstractDataBase;
import de.probst.ba.core.media.database.databases.AbstractDataBaseReadChannel;
import de.probst.ba.core.media.database.databases.AbstractDataBaseWriteChannel;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by chrisprobst on 05.10.14.
 */
public final class MemoryDataBase extends AbstractDataBase {

    private final Map<DataInfo, ByteBuf> data = new HashMap<>();

    @Override
    protected AbstractDataBaseWriteChannel openWriteChannel(DataInfo writeDataInfo) throws IOException {
        DataInfo full = writeDataInfo.full();
        ByteBuf byteBuf = data.get(full);
        if (byteBuf == null) {
            data.put(full, byteBuf = Unpooled.buffer((int) full.getSize(), (int) full.getSize()));
        }
        return new MemoryDataBaseWriteChannel(this, writeDataInfo, byteBuf);
    }

    @Override
    protected AbstractDataBaseReadChannel openReadChannel(DataInfo readDataInfo) throws IOException {
        return new MemoryDataBaseReadChannel(this, readDataInfo, data.get(readDataInfo.full()));
    }

    public MemoryDataBase(boolean allowOverwrite) {
        super(allowOverwrite);
    }
}