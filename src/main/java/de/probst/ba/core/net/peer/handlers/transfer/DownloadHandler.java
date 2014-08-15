package de.probst.ba.core.net.peer.handlers.transfer;

import de.probst.ba.core.net.Transfer;
import de.probst.ba.core.net.peer.TransferManager;
import de.probst.ba.core.net.peer.handlers.transfer.messages.DownloadRejectedMessage;
import de.probst.ba.core.net.peer.handlers.transfer.messages.DownloadRequestMessage;
import io.netty.buffer.ByteBuf;
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

    private final TransferManager transferManager;

    public DownloadHandler(TransferManager transferManager) {
        Objects.requireNonNull(transferManager);

        if (transferManager.getTransfer().isUpload()) {
            throw new IllegalArgumentException("transferManager.getTransfer().isUpload()");
        }

        this.transferManager = transferManager;
    }

    public TransferManager getTransferManager() {
        return transferManager;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Transfer transfer = getTransferManager().getTransfer();
        logger.info("Request download transfer: " + transfer);
        ctx.writeAndFlush(new DownloadRequestMessage(transfer.getDataInfo()))
                .addListener(fut -> {
                    if (!fut.isSuccess()) {
                        logger.warn("Failed to send download request",
                                fut.cause());

                        // Not able to process download request,
                        // we can stop here!
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
        } else if (msg instanceof ByteBuf) {
            ByteBuf buffer = (ByteBuf) msg;

            while (buffer.readableBytes() > 0) {

                // Simply process the transfer manager
                if (!getTransferManager().process(buffer)) {
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
}
