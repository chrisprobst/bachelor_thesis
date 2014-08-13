package de.probst.ba.core.media.databases;

import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.media.DataInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedInput;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 13.08.14.
 */
public final class DefaultDataBase implements DataBase<ChunkedInput<ByteBuf>> {

    private final ConcurrentMap<String, DataInfo> dataInfo =
            new ConcurrentHashMap<>();

    @Override
    public ConcurrentMap<String, DataInfo> getDataInfo() {
        return dataInfo;
    }

    @Override
    public List<ChunkedInput<ByteBuf>> getChunks(DataInfo dataInfo) {
        return dataInfo.getCompletedChunks()
                .mapToObj(i -> new DefaultChunkedInput(dataInfo.getChunkSize(i)))
                .collect(Collectors.toList());
    }

    private static class DefaultChunkedInput implements ChunkedInput<ByteBuf> {

        private final long size;
        private long finished = 0;

        private DefaultChunkedInput(long size) {
            this.size = size;
        }

        @Override
        public boolean isEndOfInput() throws Exception {
            return finished >= size;
        }

        @Override
        public void close() throws Exception {
        }

        @Override
        public ByteBuf readChunk(ChannelHandlerContext ctx) throws Exception {
            long rem = size - finished;
            int bufferSize = (int) Math.min(8192, rem);
            finished += rem;
            return Unpooled
                    .buffer(bufferSize)
                    .writerIndex(bufferSize - 4)
                    .writeInt(42);
        }
    }
}
