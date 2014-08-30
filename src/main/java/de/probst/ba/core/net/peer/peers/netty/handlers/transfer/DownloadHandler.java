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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 14.08.14.
 */
public final class DownloadHandler extends ChannelHandlerAdapter {

    public static Map<PeerId, Transfer> getDownloads(ChannelGroup channelGroup) {
        return channelGroup.stream()
                .map(c -> get(c).getTransfer())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(
                        Transfer::getRemotePeerId,
                        Function.<Transfer>identity()));
    }

    public static DownloadHandler get(Channel remotePeer) {
        return remotePeer.pipeline().get(DownloadHandler.class);
    }

    public static void download(Channel remoteChannel, Transfer transfer) {

        // Make sure it is a download transfer
        if (transfer.isUpload()) {
            throw new IllegalArgumentException(
                    "transfer.isUpload()");
        }

        // Check that the ids are the same
        if (!transfer.getRemotePeerId().equals(new NettyPeerId(remoteChannel))) {
            throw new IllegalArgumentException(
                    "!transfer.getRemotePeerId().equals(new NettyPeerId(remoteChannel))");
        }

        // Mark as downloading
        get(remoteChannel).download(transfer);

        // Request the transfer
        remoteChannel.pipeline().fireUserEventTriggered(transfer);
    }

    private final Logger logger =
            LoggerFactory.getLogger(DownloadHandler.class);

    private final Peer peer;
    private final AtomicReference<Transfer> transfer = new AtomicReference<>();

    private TransferManager transferManager;
    private boolean receivedBuffer;

    private void download(Transfer transfer) {
        Objects.requireNonNull(transfer);

        if (this.transfer.getAndSet(transfer) != null) {
            throw new IllegalStateException("this.transfer.getAndSet(transfer) != null");
        }
    }

    private void setup() {
        transferManager = peer.getDataBase().createTransferManager(transfer.get());
        receivedBuffer = false;
    }


    private void update() {
        transfer.set(transferManager.getTransfer());
    }

    private void reset() {
        transferManager = null;
        transfer.set(null);
    }

    private Optional<Transfer> getTransfer() {
        return Optional.ofNullable(transfer.get());
    }

    public DownloadHandler(Peer peer) {
        Objects.requireNonNull(peer);
        this.peer = peer;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (transfer.get().equals(evt)) {

            // Setup vars
            setup();

            // Write the download request
            logger.debug("Request upload transfer: " + transferManager.getTransfer());
            ctx.writeAndFlush(new UploadRequestMessage(transferManager.getTransfer().getDataInfo()))
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
            peer.getDiagnostic().downloadRequested(
                    peer, transferManager);
        }

        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof UploadRejectedMessage) {
            UploadRejectedMessage uploadRejectedMessage =
                    (UploadRejectedMessage) msg;

            logger.debug("Upload rejected: " +
                    uploadRejectedMessage.getCause().getMessage());

            // DIAGNOSTIC
            peer.getDiagnostic().downloadRejected(
                    peer, transferManager, uploadRejectedMessage.getCause());

            reset();
        } else if (msg instanceof ByteBuf) {
            ByteBuf buffer = (ByteBuf) msg;

            // Consume the whole buffer
            while (buffer.readableBytes() > 0) {

                // First buffer ? -> Download started!
                if (!receivedBuffer) {
                    receivedBuffer = true;
                    logger.debug("Download started: " +
                            transferManager.getTransfer());

                    // DIAGNOSTIC
                    peer.getDiagnostic().downloadStarted(
                            peer, transferManager);
                }

                // Process the buffer and check for completion
                boolean completed = !transferManager.process(buffer);
                update();

                logger.debug("Download processed: " +
                        transferManager.getTransfer());

                // DIAGNOSTIC
                peer.getDiagnostic().downloadProgressed(
                        peer, transferManager);

                if (completed) {
                    logger.debug("Download succeeded: " +
                            transferManager.getTransfer());

                    // DIAGNOSTIC
                    peer.getDiagnostic().downloadSucceeded(
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

                // Ready for next download
                if (completed) {
                    reset();

                    if (buffer.readableBytes() > 0) {
                        logger.debug("Uploader sent too much data: " + buffer);
                    }
                }
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }
}
