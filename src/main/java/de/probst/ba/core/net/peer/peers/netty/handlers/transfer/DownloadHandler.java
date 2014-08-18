package de.probst.ba.core.net.peer.peers.netty.handlers.transfer;

import de.probst.ba.core.net.Transfer;
import de.probst.ba.core.net.TransferManager;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.peers.netty.NettyPeerId;
import de.probst.ba.core.net.peer.peers.netty.handlers.transfer.messages.UploadRejectedMessage;
import de.probst.ba.core.net.peer.peers.netty.handlers.transfer.messages.UploadRequestMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 14.08.14.
 */
public final class DownloadHandler extends ChannelHandlerAdapter {

    public static Map<PeerId, Transfer> getDownloads(ChannelGroup channelGroup) {
        return channelGroup.stream()
                .map(c -> Optional.ofNullable(c.pipeline().get(DownloadHandler.class)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(
                        p -> p.getTransferManager().getTransfer().getRemotePeerId(),
                        p -> p.getTransferManager().getTransfer()));
    }

    public static synchronized DownloadHandler request(Peer peer,
                                                       Channel remotePeer,
                                                       Transfer transfer) {
        Objects.requireNonNull(peer);
        Objects.requireNonNull(remotePeer);
        Objects.requireNonNull(transfer);

        if (transfer.isUpload()) {
            throw new IllegalArgumentException("transfer.isUpload()");
        }

        if (!transfer.getRemotePeerId().equals(new NettyPeerId(remotePeer))) {
            throw new IllegalArgumentException(
                    "!transfer.getRemotePeerId().equals(remotePeer.id())");
        }

        // Create a new download handler
        DownloadHandler downloadHandler = new DownloadHandler(peer,
                peer.getDataBase().createTransferManager(transfer));

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

    private final Peer peer;
    private final TransferManager transferManager;
    private boolean receivedBuffer = false;

    private void remove(ChannelHandlerContext ctx) {
        ctx.pipeline().remove(this);
    }

    private DownloadHandler(Peer peer,
                            TransferManager transferManager) {
        Objects.requireNonNull(peer);
        Objects.requireNonNull(transferManager);


        if (transferManager.getTransfer().isUpload()) {
            throw new IllegalArgumentException(
                    "transferManager.getTransfer().isUpload()");
        }

        this.peer = peer;
        this.transferManager = transferManager;
    }

    public Peer getPeer() {
        return peer;
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
        logger.debug("Request upload transfer: " + transfer);
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

        // DIAGNOSTIC
        getPeer().getDiagnostic().peerRequestedDownload(
                getPeer(), getTransferManager());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof UploadRejectedMessage) {
            UploadRejectedMessage uploadRejectedMessage =
                    (UploadRejectedMessage) msg;

            logger.debug("Upload rejected: " +
                    uploadRejectedMessage.getCause().getMessage());

            // Upload rejected, lets just
            // remove this download
            remove(ctx);

            // DIAGNOSTIC
            getPeer().getDiagnostic().peerRejectedDownload(
                    getPeer(), getTransferManager(), uploadRejectedMessage.getCause());
        } else if (msg instanceof ByteBuf) {
            ByteBuf buffer = (ByteBuf) msg;

            // Consume the whole buffer
            while (buffer.readableBytes() > 0) {

                // Simply process the transfer manager
                if (!getTransferManager().process(buffer)) {
                    logger.debug("Download completed: " +
                            getTransferManager().getTransfer());

                    // We are ready when there
                    // are no further chunks to
                    // download
                    remove(ctx);

                    // DIAGNOSTIC
                    getPeer().getDiagnostic().peerSucceededDownload(
                            getPeer(), getTransferManager());
                } else {
                    logger.debug("Download processed: " +
                            getTransferManager().getTransfer());

                    // DIAGNOSTIC
                    getPeer().getDiagnostic().peerProgressedDownload(
                            getPeer(), getTransferManager());
                }

                if (!receivedBuffer) {
                    receivedBuffer = true;
                    // DIAGNOSTIC
                    getPeer().getDiagnostic().peerStartedDownload(
                            getPeer(), getTransferManager());
                }
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }
}
