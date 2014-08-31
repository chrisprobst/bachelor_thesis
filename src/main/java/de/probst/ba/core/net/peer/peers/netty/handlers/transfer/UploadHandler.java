package de.probst.ba.core.net.peer.peers.netty.handlers.transfer;

import de.probst.ba.core.net.Transfer;
import de.probst.ba.core.net.TransferManager;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.peers.netty.NettyPeerId;
import de.probst.ba.core.net.peer.peers.netty.handlers.transfer.messages.UploadRejectedMessage;
import de.probst.ba.core.net.peer.peers.netty.handlers.transfer.messages.UploadRequestMessage;
import de.probst.ba.core.util.concurrent.AtomicCounter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.stream.ChunkedInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 13.08.14.
 */
public final class UploadHandler extends SimpleChannelInboundHandler<UploadRequestMessage> {

    public static Map<PeerId, Transfer> getUploads(ChannelGroup channelGroup) {
        return channelGroup.stream()
                .map(UploadHandler::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(UploadHandler::getTransferManager)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(
                        p -> p.getTransfer().getRemotePeerId(),
                        TransferManager::getTransfer));
    }

    public static Optional<UploadHandler> get(Channel remotePeer) {
        return Optional.ofNullable(remotePeer.pipeline().get(UploadHandler.class));
    }

    private static final class ChunkedDataBaseInput implements ChunkedInput<ByteBuf> {

        private final TransferManager transferManager;

        public ChunkedDataBaseInput(TransferManager transferManager) {
            Objects.requireNonNull(transferManager);
            this.transferManager = transferManager;
        }

        @Override
        public boolean isEndOfInput() throws Exception {
            return transferManager.getTransfer().isCompleted();
        }

        @Override
        public void close() throws Exception {
            // We do not need to do anything for now
        }

        @Override
        public ByteBuf readChunk(ChannelHandlerContext ctx) throws Exception {
            ByteBuf byteBuf = Unpooled.buffer(500);
            transferManager.process(byteBuf);
            return byteBuf;
        }

        @Override
        public long length() {
            return transferManager.getTransfer().getSize();
        }

        @Override
        public long progress() {
            return transferManager.getTransfer().getCompletedSize();
        }
    }


    private final Logger logger =
            LoggerFactory.getLogger(UploadHandler.class);

    private final Peer peer;
    private final AtomicCounter parallelUploads;
    private volatile TransferManager transferManager;

    private boolean setup(ChannelHandlerContext ctx, TransferManager transferManager) {
        if (parallelUploads.tryIncrement(peer.getBrain().getMaxParallelUploads())) {
            this.transferManager = transferManager;
            ctx.channel().config().setAutoRead(false);
            return true;
        }
        return false;
    }

    private void reset(ChannelHandlerContext ctx) {
        transferManager = null;
        ctx.channel().config().setAutoRead(true);
        parallelUploads.tryDecrement();
    }

    public UploadHandler(Peer peer, AtomicCounter parallelUploads) {
        Objects.requireNonNull(peer);
        Objects.requireNonNull(parallelUploads);
        this.parallelUploads = parallelUploads;
        this.peer = peer;
    }

    public Optional<TransferManager> getTransferManager() {
        return Optional.ofNullable(transferManager);
    }

    /**
     * Checks the message.
     *
     * @param uploadRequestMessage
     * @return
     */
    private boolean isUploadRequestMessageValid(UploadRequestMessage uploadRequestMessage) {
        return uploadRequestMessage != null &&
                uploadRequestMessage.getDataInfo() != null &&
                !uploadRequestMessage.getDataInfo().isEmpty();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        super.exceptionCaught(ctx, cause);
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx,
                                   UploadRequestMessage msg) throws Exception {

        if (!isUploadRequestMessageValid(msg)) {
            Exception cause = new IllegalArgumentException(
                    "Upload request message null or empty");

            logger.warn(cause.getMessage());
            ctx.writeAndFlush(new UploadRejectedMessage(cause));

            // DIAGNOSTIC
            peer.getDiagnostic().uploadRejected(
                    peer, null, cause);
        } else {

            // Create a new transfer manager
            TransferManager newTransferManager = peer.getDataBase()
                    .createTransferManager(Transfer.upload(
                            new NettyPeerId(ctx.channel()),
                            msg.getDataInfo()));

            // If the upload is not allowed, reject it!
            if (!setup(ctx, newTransferManager)) {
                Exception cause = new IllegalStateException(
                        "The brain rejected the upload, because it " +
                                "reached the maximum number of uploads");

                logger.debug(cause.getMessage());
                ctx.writeAndFlush(new UploadRejectedMessage(cause));

                // DIAGNOSTIC
                peer.getDiagnostic().uploadRejected(
                        peer, newTransferManager, cause);
            } else {
                logger.debug("Starting upload: " + newTransferManager.getTransfer());

                // Upload chunked input
                ctx.writeAndFlush(new ChunkedDataBaseInput(newTransferManager)).addListener(fut -> {
                    if (!fut.isSuccess()) {
                        logger.warn("Upload failed", fut.cause());

                        // No success means network or chunked input exception
                        ctx.close();
                    } else {
                        logger.debug("Upload succeeded");

                        // DIAGNOSTIC
                        peer.getDiagnostic().uploadSucceeded(
                                peer, transferManager);

                        // Restore
                        reset(ctx);
                    }
                });

                // DIAGNOSTIC
                peer.getDiagnostic().uploadStarted(
                        peer, newTransferManager);
            }
        }
    }
}
