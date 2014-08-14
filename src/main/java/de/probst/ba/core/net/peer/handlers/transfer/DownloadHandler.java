package de.probst.ba.core.net.peer.handlers.transfer;

import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.net.Transfer;
import de.probst.ba.core.net.peer.handlers.transfer.messages.DownloadBufferMessage;
import de.probst.ba.core.net.peer.handlers.transfer.messages.DownloadRejectedMessage;
import de.probst.ba.core.net.peer.handlers.transfer.messages.DownloadRequestMessage;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Objects;

/**
 * Created by chrisprobst on 14.08.14.
 */
public final class DownloadHandler extends ChannelHandlerAdapter {

    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(DownloadHandler.class);

    private final DataBase dataBase;
    private volatile Transfer transfer;

    private Transfer advanceTransfer(long amount) {
        transfer = transfer.advance(amount);
        logger.info("Advanced download transfer: " + transfer);
        return transfer;
    }

    public DownloadHandler(DataBase dataBase, Transfer transfer) {
        Objects.requireNonNull(dataBase);
        Objects.requireNonNull(transfer);
        this.dataBase = dataBase;
        this.transfer = transfer;
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

                        ctx.close();
                    }
                });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof DownloadRejectedMessage) {
            logger.warn("Download rejected",
                    ((DownloadRejectedMessage) msg).getCause());

            ctx.close();
        } else if (msg instanceof DownloadBufferMessage) {
            DownloadBufferMessage bufferMessage = (DownloadBufferMessage) msg;

            // Advance the transfer
            Transfer transfer = advanceTransfer(bufferMessage.getLength());

            // Update the data base
            if (transfer.isCompleted()) {
                dataBase.storeBufferAndComplete(
                        transfer.getDataInfo().getHash(),
                        bufferMessage.getChunkIndex(),
                        bufferMessage.getOffset(),
                        bufferMessage.getLength(),
                        bufferMessage.getBuffer());

                // We are done, remove this handler
                ctx.pipeline().remove(this);
            } else {
                dataBase.storeBuffer(
                        transfer.getDataInfo().getHash(),
                        bufferMessage.getChunkIndex(),
                        bufferMessage.getOffset(),
                        bufferMessage.getLength(),
                        bufferMessage.getBuffer());
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }

    public Transfer getTransfer() {
        return transfer;
    }
}
