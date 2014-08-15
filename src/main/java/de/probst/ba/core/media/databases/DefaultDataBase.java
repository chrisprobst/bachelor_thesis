package de.probst.ba.core.media.databases;

import de.probst.ba.core.media.DataInfo;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 13.08.14.
 */
public final class DefaultDataBase extends AbstractDataBase {

    public DefaultDataBase() {
    }

    public DefaultDataBase(DataInfo... dataInfo) {
        this(Arrays.stream(dataInfo)
                .collect(Collectors.toMap(
                        DataInfo::getHash,
                        Function.identity())));
    }

    public DefaultDataBase(Map<String, DataInfo> initialDataInfo) {
        dataInfo.putAll(initialDataInfo);
    }

    @Override
    protected void doProcessBuffer(DataInfo dataInfo,
                                   int chunkIndex,
                                   long chunkSize,
                                   long offset,
                                   ByteBuf byteBuf,
                                   int length,
                                   boolean download) throws IOException {
        if (download) {
            byteBuf.readerIndex(byteBuf.readerIndex() + length);
        } else {
            byteBuf.writeByte((byte) chunkIndex);
            byteBuf.writerIndex(byteBuf.writerIndex() + length - 1);
        }
    }

    @Override
    protected void doComplete(DataInfo dataInfo, int chunkIndex, boolean download) throws IOException {
        if (download) {
            this.dataInfo.computeIfPresent(dataInfo.getHash(), (k, v) -> v.withChunk(chunkIndex));
        }
    }

    @Override
    public void close() throws IOException {
        // Do nothing
    }
}
