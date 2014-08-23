package de.probst.ba.core.net.peer.peers.netty.handlers.transfer;

import de.probst.ba.core.media.DataInfo;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 14.08.14.
 */
public final class DownloadHandler extends ChannelHandlerAdapter {

    public static Map<PeerId, Transfer> getDownloads(ChannelGroup channelGroup) {
        return channelGroup.stream()
                .map(c -> get(c).getTransferManager())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(TransferManager::getTransfer)
                .collect(Collectors.toMap(
                        Transfer::getRemotePeerId,
                        Function.<Transfer>identity()));
    }

    public static DownloadHandler get(Channel remotePeer) {
        return remotePeer.pipeline().get(DownloadHandler.class);
    }

    private final Logger logger =
            LoggerFactory.getLogger(DownloadHandler.class);

    private final Peer peer;
    private volatile ChannelHandlerContext ctx;

    private volatile TransferManager transferManager;
    private volatile boolean receivedBuffer = false;

    public DownloadHandler(Peer peer) {
        Objects.requireNonNull(peer);
        this.peer = peer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        super.channelActive(ctx);
    }

    public synchronized void download(Transfer transfer) {
        if (this.transferManager != null) {
            throw new IllegalStateException("this.transferManager != null");
        }

        // Try to get ctx
        ChannelHandlerContext ctx = this.ctx;
        if (ctx == null) {
            throw new IllegalStateException("ctx == null");
        }

        // Check that the ids are the same
        if (!transfer.getRemotePeerId().equals(new NettyPeerId(ctx.channel()))) {
            throw new IllegalArgumentException(
                    "!transfer.getRemotePeerId().equals(new NettyPeerId(ctx.channel()))");
        }

        // Create a new transfer manager
        TransferManager newTransferManager = transferManager =
                peer.getDataBase().createTransferManager(transfer);

        // Make sure it is a download transfer
        if (newTransferManager.getTransfer().isUpload()) {
            throw new IllegalArgumentException(
                    "newTransferManager.getTransfer().isUpload()");
        }

        // Set vars
        receivedBuffer = false;

        // Write the download request
        logger.debug("Request upload transfer: " + newTransferManager.getTransfer());
        ctx.writeAndFlush(new UploadRequestMessage(newTransferManager.getTransfer().getDataInfo()))
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
        getPeer().getDiagnostic().downloadRequested(
                peer, newTransferManager);
    }

    public Peer getPeer() {
        return peer;
    }

    public Optional<TransferManager> getTransferManager() {
        return Optional.ofNullable(transferManager);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof UploadRejectedMessage) {
            try {
                UploadRejectedMessage uploadRejectedMessage =
                        (UploadRejectedMessage) msg;

                logger.debug("Upload rejected: " +
                        uploadRejectedMessage.getCause().getMessage());

                // DIAGNOSTIC
                getPeer().getDiagnostic().downloadRejected(
                        peer, transferManager, uploadRejectedMessage.getCause());
            } finally {
                // We are not downloading anymore
                transferManager = null;
            }
        } else if (msg instanceof ByteBuf) {
            ByteBuf buffer = (ByteBuf) msg;

            // Consume the whole buffer
            while (buffer.readableBytes() > 0) {
                boolean completed = false;
                try {
                    // First buffer ? -> Download started!
                    if (!receivedBuffer) {
                        receivedBuffer = true;
                        logger.debug("Download started: " +
                                transferManager.getTransfer());

                        // DIAGNOSTIC
                        getPeer().getDiagnostic().downloadStarted(
                                peer, transferManager);
                    }

                    // Process the buffer and check for completion
                    completed = !transferManager.process(buffer);

                    logger.debug("Download processed: " +
                            transferManager.getTransfer());

                    // DIAGNOSTIC
                    getPeer().getDiagnostic().downloadProgressed(
                            peer, transferManager);

                    // Simply process the transfer manager
                    if (completed) {
                        logger.debug("Download completed: " +
                                transferManager.getTransfer());

                        // DIAGNOSTIC
                        getPeer().getDiagnostic().downloadSucceeded(
                                peer, transferManager);
                    }

                    // Query data base
                    DataInfo dataInfoStatus = peer.getDataBase().get(
                            transferManager.getTransfer().getDataInfo().getHash());

                    if (dataInfoStatus != null && dataInfoStatus.isCompleted()) {
                        logger.debug("Data completed: " + dataInfoStatus);

                        // DIAGNOSTIC
                        peer.getDiagnostic().dataCompleted(
                                peer, dataInfoStatus, transferManager);
                    }

                } catch (Exception e) {

                    // Shutdown connection
                    ctx.close();

                    logger.debug("Download failed: " +
                            transferManager.getTransfer() +
                            ", connection closed. Cause:" + e);

                    // DIAGNOSTIC
                    getPeer().getDiagnostic().downloadFailed(
                            peer, transferManager, e);
                } finally {
                    if (completed) {
                        transferManager = null;
                    }
                }
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }
}
