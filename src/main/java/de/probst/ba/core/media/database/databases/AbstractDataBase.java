package de.probst.ba.core.media.database.databases;

import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.DataInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 15.08.14.
 */
public abstract class AbstractDataBase implements DataBase {

    private final Map<String, DataInfo> dataInfo = new HashMap<>();
    private volatile Map<String, DataInfo> nonEmptyDataInfoView = Collections.emptyMap();

    private void createNonEmptyDataInfoView() {
        nonEmptyDataInfoView = Collections.unmodifiableMap(dataInfo.entrySet()
                                                                   .stream()
                                                                   .filter(p -> !p.getValue().isEmpty())
                                                                   .collect(Collectors.toMap(Map.Entry::getKey,
                                                                                             Map.Entry::getValue)));
    }

    protected abstract void doProcessBuffer(DataInfo dataInfo,
                                            int chunkIndex,
                                            long chunkSize,
                                            long offset,
                                            ByteBuf byteBuf,
                                            int length,
                                            boolean download) throws IOException;

    protected void doComplete(DataInfo dataInfo, int chunkIndex, boolean download) throws IOException {
        if (download) {
            this.dataInfo.computeIfPresent(dataInfo.getHash(), (k, v) -> v.withChunk(chunkIndex));
            createNonEmptyDataInfoView();
        }
    }

    @Override
    public Map<String, DataInfo> getDataInfo() {
        return nonEmptyDataInfoView;
    }

    @Override
    public synchronized DataInfo get(String hash) {
        return dataInfo.get(hash);
    }

    @Override
    public synchronized void processBuffer(DataInfo dataInfo,
                                           int chunkIndex,
                                           long offset,
                                           ByteBuf byteBuf,
                                           int length,
                                           boolean download) throws IOException {
        Objects.requireNonNull(dataInfo);
        Objects.requireNonNull(byteBuf);
        DataInfo existingDataInfo = this.dataInfo.get(dataInfo.getHash());

        if (existingDataInfo == null) {
            if (download) {
                existingDataInfo = dataInfo.empty();
                this.dataInfo.put(existingDataInfo.getHash(), existingDataInfo);
            } else {
                throw new IllegalArgumentException("Data info for uploading does not exist: " + dataInfo);
            }
        } else if (!existingDataInfo.isCompatibleWith(dataInfo)) {
            throw new IllegalArgumentException("!existingDataInfo.isCompatibleWith(dataInfo)");
        }

        if (download && existingDataInfo.isChunkCompleted(chunkIndex)) {
            throw new IllegalArgumentException("Chunk already completed: " + chunkIndex);
        }

        if (!download && !existingDataInfo.isChunkCompleted(chunkIndex)) {
            throw new IllegalArgumentException("Chunk not completed: " + chunkIndex);
        }

        long chunkSize = existingDataInfo.getChunkSize(chunkIndex);

        if (offset < 0 || offset >= chunkSize) {
            throw new IllegalArgumentException("offset < 0 || offset >= chunkSize");
        }

        if (length <= 0) {
            throw new IllegalArgumentException("length <= 0");
        }

        if (download && byteBuf.readableBytes() < length) {
            throw new IllegalArgumentException("download && byteBuf.readableBytes() < length");
        }

        if (!download && byteBuf.writableBytes() < length) {
            throw new IllegalArgumentException("!download && byteBuf.writableBytes() < length");
        }

        if (offset + length > chunkSize) {
            throw new IllegalArgumentException("offset + length > chunkSize");
        }

        // Do process the buffer
        doProcessBuffer(existingDataInfo, chunkIndex, chunkSize, offset, byteBuf, length, download);
    }

    @Override
    public synchronized void insert(DataInfo dataInfo, ReadableByteChannel readableByteChannel) throws IOException {
        Objects.requireNonNull(dataInfo);
        Objects.requireNonNull(readableByteChannel);
        DataInfo existingDataInfo = this.dataInfo.get(dataInfo.getHash());

        if (existingDataInfo != null && !existingDataInfo.isCompatibleWith(dataInfo)) {
            throw new IllegalArgumentException(
                    "existingDataInfo != null && !existingDataInfo.isCompatibleWith(dataInfo)");
        }

        for (int chunkIndex : dataInfo.getCompletedChunks().toArray()) {
            // Read one chunk into memory
            ByteBuffer byteBuffer = ByteBuffer.allocate((int) dataInfo.getChunkSize(chunkIndex));
            while (byteBuffer.hasRemaining()) {
                if (readableByteChannel.read(byteBuffer) < 0) {
                    throw new EOFException();
                }
            }
            byteBuffer.flip();

            // Store buffer in data base
            processBufferAndComplete(dataInfo,
                                     chunkIndex,
                                     0,
                                     Unpooled.wrappedBuffer(byteBuffer),
                                     byteBuffer.remaining(),
                                     true);
        }
    }

    @Override
    public synchronized void query(DataInfo dataInfo, WritableByteChannel writableByteChannel) throws IOException {
        Objects.requireNonNull(dataInfo);
        Objects.requireNonNull(writableByteChannel);
        DataInfo existingDataInfo = this.dataInfo.get(dataInfo.getHash());

        if (!existingDataInfo.contains(dataInfo)) {
            throw new IllegalArgumentException("!existingDataInfo.contains(dataInfo)");
        }

        for (int chunkIndex : dataInfo.getCompletedChunks().toArray()) {
            // Create a buffer for the next chunk
            ByteBuf byteBuf =
                    Unpooled.buffer((int) dataInfo.getChunkSize(chunkIndex), (int) dataInfo.getChunkSize(chunkIndex));

            // Query buffer from data base
            processBufferAndComplete(dataInfo,
                                     chunkIndex,
                                     0,
                                     byteBuf,
                                     byteBuf.capacity(),
                                     false);

            // Write chunk buffer to channel
            ByteBuffer chunkBuffer = byteBuf.nioBuffer();
            while (chunkBuffer.hasRemaining()) {
                writableByteChannel.write(chunkBuffer);
            }
        }
    }

    @Override
    public SeekableByteChannel[] unsafeQueryRawWithName(String name) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public SeekableByteChannel unsafeQueryRaw(String hash) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public synchronized void processBufferAndComplete(DataInfo dataInfo,
                                                      int chunkIndex,
                                                      long offset,
                                                      ByteBuf byteBuf,
                                                      int length,
                                                      boolean download) throws IOException {

        // Process buffer as usual
        processBuffer(dataInfo, chunkIndex, offset, byteBuf, length, download);

        // Complete the chunk
        doComplete(dataInfo, chunkIndex, download);
    }
}
