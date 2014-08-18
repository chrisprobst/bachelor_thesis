package de.probst.ba.core.net.peer.peers.netty.handlers.transfer;

import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.net.Transfer;
import de.probst.ba.core.net.TransferManager;
import de.probst.ba.core.net.peer.peers.netty.handlers.transfer.messages.UploadRejectedMessage;
import de.probst.ba.core.net.peer.peers.netty.handlers.transfer.messages.UploadRequestMessage;
import de.probst.ba.core.util.Tuple;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 14.08.14.
 */
public final class DownloadHandler extends ChannelHandlerAdapter {

    public static Map<Object, Transfer> getDownloads(ChannelGroup channelGroup) {
        return channelGroup.stream()
                .map(c -> Tuple.of(c, c.pipeline().get(DownloadHandler.class)))
                .filter(h -> h.second() != null)
                .collect(Collectors.toMap(
                        p -> p.first().id(),
                        p -> p.second().getTransferManager().getTransfer()));
    }

    public static synchronized DownloadHandler request(DataBase dataBase,
                                                       Channel remotePeer,
                                                       Transfer transfer) {
        Objects.requireNonNull(dataBase);
        Objects.requireNonNull(remotePeer);
        Objects.requireNonNull(transfer);

        if (transfer.isUpload()) {
            throw new IllegalArgumentException("transfer.isUpload()");
        }

        if (!transfer.getRemotePeerId().equals(remotePeer.id())) {
            throw new IllegalArgumentException(
                    "!transfer.getRemotePeerId().equals(remotePeer.id())");
        }

        // Create a new download handler
        DownloadHandler downloadHandler = new DownloadHandler(
                dataBase.createTransferManager(transfer));

        // Check that the handler does not exist yet
        if (remotePeer.pipeline().context(DownloadHandler.class) != null) {
            throw new IllegalStateException(
                    "remotePeer.pipeline().context(DownloadHandler.class) != null");
        }

        // Add to pipeline
        remotePeer.pipeline().addLast(downloadHandler);

        return downloadHandler;
    }

    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(DownloadHandler.class);

    private final TransferManager transferManager;

    private void remove(ChannelHandlerContext ctx) {
        ctx.pipeline().remove(this);
    }

    private DownloadHandler(TransferManager transferManager) {
        Objects.requireNonNull(transferManager);

        if (transferManager.getTransfer().isUpload()) {
            throw new IllegalArgumentException(
                    "transferManager.getTransfer().isUpload()");
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
        logger.info("Request upload transfer: " + transfer);
        ctx.writeAndFlush(new UploadRequestMessage(transfer.getDataInfo()))
                .addListener(fut -> {
                    if (!fut.isSuccess()) {
                        logger.warn("Failed to send upload request",
                                fut.cause());

                        // Not able to process upload request,
                        // we can stop here!
                        ctx.close();
                    }
                });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof UploadRejectedMessage) {
            logger.warn("Upload rejected: " +
                    ((UploadRejectedMessage) msg).getCause().getMessage());

            // Upload rejected, lets just
            // remove this download
            remove(ctx);
        } else if (msg instanceof ByteBuf) {
            ByteBuf buffer = (ByteBuf) msg;

            // Consume the whole buffer
            while (buffer.readableBytes() > 0) {

                // Simply process the transfer manager
                if (!getTransferManager().process(buffer)) {
                    logger.info("Download completed: " +
                            getTransferManager().getTransfer());

                    // We are ready when there
                    // are no further chunks to
                    // download
                    remove(ctx);
                } else {
                    logger.debug("Download processed: " +
                            getTransferManager().getTransfer());
                }
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }
}
