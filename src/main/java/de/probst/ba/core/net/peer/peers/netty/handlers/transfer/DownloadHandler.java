package de.probst.ba.core.net.peer.peers.netty.handlers.transfer;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.transfer.Transfer;
import de.probst.ba.core.media.transfer.TransferManager;
import de.probst.ba.core.net.peer.Leecher;
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

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
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
                .map(DownloadHandler::get)
                .map(DownloadHandler::getTransfer)
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

    private final Leecher leecher;
    private final AtomicReference<Transfer> transfer =
            new AtomicReference<>();

    private TransferManager transferManager;
    private boolean receivedBuffer;

    private void download(Transfer transfer) {
        Objects.requireNonNull(transfer);

        if (this.transfer.getAndSet(transfer) != null) {
            throw new IllegalStateException("this.transfer.getAndSet(transfer) != null");
        }
    }

    private void setup() {
        transferManager = leecher.getDataBase().createTransferManager(transfer.get());
        receivedBuffer = false;
    }

    private boolean update(ByteBuf buffer) throws IOException {
        boolean completed = !transferManager.process(buffer);
        transfer.set(transferManager.getTransfer());
        return completed;
    }

    private void reset() {
        transferManager = null;
        transfer.set(null);
        leecher.leech();
    }

    private Optional<Transfer> getTransfer() {
        return Optional.ofNullable(transfer.get());
    }

    public DownloadHandler(Leecher leecher) {
        Objects.requireNonNull(leecher);
        this.leecher = leecher;
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
            ctx.writeAndFlush(new UploadRequestMessage(transferManager.getTransfer().getDataInfo()))
                    .addListener(fut -> {
                        if (!fut.isSuccess()) {
                            // Close if this exception was not expected
                            if (!(fut.cause() instanceof ClosedChannelException)) {
                                ctx.close();

                                logger.warn("Leecher " + leecher.getPeerId() +
                                        " failed to send upload request, connection closed", fut.cause());
                            }
                        }
                    });

            logger.debug("Leecher " + leecher.getPeerId() +
                    " requested download " + transferManager);

            // HANDLER
            leecher.getPeerHandler().downloadRequested(
                    leecher, transferManager);
        }

        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof UploadRejectedMessage) {
            UploadRejectedMessage uploadRejectedMessage =
                    (UploadRejectedMessage) msg;

            logger.info("Leecher " + leecher.getPeerId() +
                    " requested the download " + transferManager +
                    ", but was rejected");

            // HANDLER
            leecher.getPeerHandler().downloadRejected(
                    leecher, transferManager, uploadRejectedMessage.getCause());

            reset();
        } else if (msg instanceof ByteBuf) {
            ByteBuf buffer = (ByteBuf) msg;

            // Consume the whole buffer
            while (buffer.readableBytes() > 0) {

                // First buffer ? -> Download started!
                if (!receivedBuffer) {
                    receivedBuffer = true;
                    logger.debug("Leecher " + leecher.getPeerId() +
                            " started download " + transferManager);

                    // HANDLER
                    leecher.getPeerHandler().downloadStarted(
                            leecher, transferManager);
                }

                // Process the buffer and check for completion
                boolean completed = update(buffer);

                logger.debug("Leecher " + leecher.getPeerId() +
                        " progressed download " + transferManager);

                // HANDLER
                leecher.getPeerHandler().downloadProgressed(
                        leecher, transferManager);

                if (completed) {
                    logger.debug("Leecher " + leecher.getPeerId() +
                            " succeeded download " + transferManager);

                    // HANDLER
                    leecher.getPeerHandler().downloadSucceeded(
                            leecher, transferManager);
                }

                // Query data base
                DataInfo dataInfo = leecher.getDataBase().get(
                        transferManager.getTransfer().getDataInfo().getHash());

                if (dataInfo != null && dataInfo.isCompleted()) {
                    logger.info("Leecher " + leecher.getPeerId() +
                            " completed the data " + dataInfo +
                            " with " + transferManager);

                    // HANDLER
                    leecher.getPeerHandler().dataCompleted(
                            leecher, dataInfo, transferManager);
                }

                // Ready for next download
                if (completed) {
                    reset();

                    // Stop consuming here
                    if (buffer.readableBytes() > 0) {
                        logger.warn("Leecher " + leecher.getPeerId() +
                                " received too much data " + buffer);
                        break;
                    }
                }
            }

            // Nobody is gonna use this
            buffer.release();
        } else {
            super.channelRead(ctx, msg);
        }
    }
}
