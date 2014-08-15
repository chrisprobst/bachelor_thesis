package de.probst.ba.core.net.peer.handlers.transfer;

import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.Transfer;
import de.probst.ba.core.net.TransferManager;
import de.probst.ba.core.net.peer.handlers.transfer.messages.UploadRejectedMessage;
import de.probst.ba.core.net.peer.handlers.transfer.messages.UploadRequestMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 14.08.14.
 */
public final class DownloadHandler extends ChannelHandlerAdapter {

    public static Map<Object, Transfer> getDownloads(ChannelGroup channelGroup) {
        return channelGroup.stream()
                .map(c -> new AbstractMap.SimpleEntry<>(c, c.pipeline().get(DownloadHandler.class)))
                .filter(h -> h.getValue() != null)
                .collect(Collectors.toMap(
                        p -> p.getKey().id(),
                        p -> p.getValue().getTransferManager().getTransfer()));
    }

    public static DownloadHandler request(DataBase dataBase,
                                          Channel remotePeer,
                                          DataInfo dataInfo) {

        // Create a new download handler
        DownloadHandler downloadHandler = new DownloadHandler(
                dataBase.createDownloadTransferManager(
                        remotePeer.id(), dataInfo));

        // Add to pipeline
        remotePeer.pipeline().addLast(downloadHandler);

        return downloadHandler;
    }

    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(DownloadHandler.class);

    private final TransferManager transferManager;

    private DownloadHandler(TransferManager transferManager) {
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
            logger.warn("Upload rejected",
                    ((UploadRejectedMessage) msg).getCause());

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
