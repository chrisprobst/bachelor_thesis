package de.probst.ba.core.net.peer.handlers.transfer;

import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.net.Transfer;
import de.probst.ba.core.net.peer.handlers.transfer.messages.DownloadRejectedMessage;
import de.probst.ba.core.net.peer.handlers.transfer.messages.DownloadRequestMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.PrimitiveIterator;

/**
 * Created by chrisprobst on 14.08.14.
 */
public final class DownloadHandler extends ChannelHandlerAdapter {

    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(DownloadHandler.class);

    // The data base
    private final DataBase dataBase;

    // Iterates all missing chunks
    private final PrimitiveIterator.OfInt missingChunks;

    // The data base
    // Must be volatile because we access
    // this field from other threads
    private volatile Transfer transfer;

    // Status variables for every chunk
    private int chunkIndex;
    private long chunkSize;
    private long offset;

    /**
     * Setup all internal variables for the next chunk.
     *
     * @return True if there are missing chunks,
     * otherwise false.
     */
    private boolean setupNextChunkTransfer() {
        if (!missingChunks.hasNext()) {
            return false;
        }

        chunkIndex = missingChunks.next();
        chunkSize = getTransfer()
                .getDataInfo()
                .getChunkSize(chunkIndex);
        offset = 0;

        return true;
    }

    /**
     * Advances the download transfer
     * by the given bytes.
     *
     * @param byteBuf
     * @return True if there is more to download,
     * otherwise false.
     * @throws IOException
     */
    private boolean advanceChunkTransfer(ByteBuf byteBuf) throws IOException {
        // Read the remaining bytes into the buffer
        int bufferLength = (int) Math.min(byteBuf.readableBytes(), chunkSize - offset);
        byte[] buffer = new byte[bufferLength];
        byteBuf.readBytes(buffer);

        // Advance the transfer
        transfer = transfer.advance(buffer.length);
        logger.info("Advanced download transfer: " + transfer);

        // Do we have finished the chunk
        boolean chunkCompleted = offset + buffer.length == chunkSize;

        if (chunkCompleted) {
            // We can complete the chunk
            dataBase.storeBufferAndComplete(
                    getTransfer().getDataInfo().getHash(),
                    chunkIndex,
                    offset,
                    buffer);

        } else {
            // Fill the chunk
            dataBase.storeBuffer(
                    getTransfer().getDataInfo().getHash(),
                    chunkIndex,
                    offset,
                    buffer);
        }

        // Increase
        offset += buffer.length;

        return !chunkCompleted || setupNextChunkTransfer();
    }

    public DownloadHandler(DataBase dataBase, Transfer transfer) {
        Objects.requireNonNull(dataBase);
        Objects.requireNonNull(transfer);
        this.dataBase = dataBase;
        this.transfer = transfer;
        missingChunks = transfer
                .getDataInfo()
                .getCompletedChunks()
                .iterator();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        logger.info("Request download transfer: " + transfer);
        ctx.writeAndFlush(new DownloadRequestMessage(transfer.getDataInfo()))
                .addListener(fut -> {
                    if (!fut.isSuccess()) {
                        logger.warn("Failed to send download request",
                                fut.cause());

                        // Not able to process download request,
                        // we can stop here!
                        ctx.close();
                    } else {
                        // Setup the next chunk transfer
                        // for the first time
                        setupNextChunkTransfer();
                    }
                });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof DownloadRejectedMessage) {
            logger.warn("Download rejected",
                    ((DownloadRejectedMessage) msg).getCause());

            ctx.close();
        } else if (msg instanceof ByteBuf) {
            ByteBuf buffer = (ByteBuf) msg;

            while (buffer.readableBytes() > 0) {
                if (!advanceChunkTransfer(buffer)) {
                    // We are ready when there
                    // are no further chunks to
                    // download
                    ctx.pipeline().remove(this);
                }
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }

    public Transfer getTransfer() {
        return transfer;
    }
}
