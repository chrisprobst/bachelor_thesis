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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.stream.ChunkedInput;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 13.08.14.
 */
public final class UploadHandler extends SimpleChannelInboundHandler<UploadRequestMessage> {

    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(UploadHandler.class);

    public static Map<PeerId, Transfer> getUploads(ChannelGroup channelGroup) {
        return channelGroup.stream()
                .map(c -> c.pipeline().get(UploadHandler.class).getTransferManager())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(
                        p -> p.getTransfer().getRemotePeerId(),
                        p -> p.getTransfer()));
    }

    private final class ChunkedDataBaseInput implements ChunkedInput<ByteBuf> {

        @Override
        public boolean isEndOfInput() throws Exception {
            return getTransferManager().get().isCompleted();
        }

        @Override
        public void close() throws Exception {
            // We do not need to do anything for now
        }

        @Override
        public ByteBuf readChunk(ChannelHandlerContext ctx) throws Exception {
            ByteBuf byteBuf = Unpooled.buffer(500);
            getTransferManager().get().process(byteBuf);
            return byteBuf;
        }

        @Override
        public long length() {
            return getTransferManager().get().getTransfer().getSize();
        }

        @Override
        public long progress() {
            return getTransferManager().get().getTransfer().getCompletedSize();
        }
    }

    private final Peer peer;
    private final AtomicCounter parallelUploads;
    private volatile TransferManager transferManager;

    private boolean setup(ChannelHandlerContext ctx, TransferManager transferManager) {
        if (parallelUploads.tryIncrement(getPeer().getBrain().getMaxParallelUploads())) {
            this.transferManager = transferManager;
            ctx.channel().config().setAutoRead(false);
            return true;
        }
        return false;
    }

    private void restore(ChannelHandlerContext ctx, boolean decrement) {
        transferManager = null;
        ctx.channel().config().setAutoRead(true);
        if (decrement) {
            parallelUploads.tryDecrement();
        }
    }

    public UploadHandler(Peer peer, AtomicCounter parallelUploads) {
        Objects.requireNonNull(peer);
        Objects.requireNonNull(parallelUploads);
        this.parallelUploads = parallelUploads;
        this.peer = peer;
    }

    public Peer getPeer() {
        return peer;
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
            Exception cause = new IllegalArgumentException("Upload request message null or empty");

            logger.warn(cause.getMessage());
            ctx.writeAndFlush(new UploadRejectedMessage(cause));

            // DIAGNOSTIC
            getPeer().getDiagnostic().peerRejectedUpload(
                    getPeer(), null, cause);
        } else {
            TransferManager newTransferManager;
            boolean wasIncremented = false;
            try {
                // Create a new transfer manager
                newTransferManager = getPeer().getDataBase()
                        .createTransferManager(Transfer.upload(
                                new NettyPeerId(ctx.channel()),
                                msg.getDataInfo()));

                // If the upload is not allowed, reject it!
                if (!(wasIncremented = setup(ctx, newTransferManager))) {
                    Exception cause = new IllegalStateException(
                            "The brain rejected the upload, because it " +
                                    "reached the maximum number of uploads");

                    logger.debug(cause.getMessage());
                    ctx.writeAndFlush(new UploadRejectedMessage(cause));

                    // DIAGNOSTIC
                    getPeer().getDiagnostic().peerRejectedUpload(
                            getPeer(), newTransferManager, cause);
                } else {
                    logger.debug("Starting upload: " + newTransferManager.getTransfer());

                    // Upload chunked input
                    ctx.writeAndFlush(new ChunkedDataBaseInput()).addListener(fut -> {
                        try {
                            if (!fut.isSuccess()) {
                                logger.warn("Upload failed", fut.cause());

                                // Upload failed, notify!
                                ctx.writeAndFlush(new UploadRejectedMessage(fut.cause()));

                                // DIAGNOSTIC
                                getPeer().getDiagnostic().peerRejectedUpload(
                                        getPeer(), transferManager, fut.cause());
                            } else {
                                logger.debug("Upload succeeded");

                                // DIAGNOSTIC
                                getPeer().getDiagnostic().peerSucceededUpload(
                                        getPeer(), transferManager);
                            }
                        } finally {
                            restore(ctx, true);
                        }
                    });

                    // DIAGNOSTIC
                    getPeer().getDiagnostic().peerStartedUpload(
                            getPeer(), newTransferManager);
                }

            } catch (Exception e) {
                try {
                    logger.warn("Upload failed", e);

                    // If the creation failed, reject!
                    ctx.writeAndFlush(new UploadRejectedMessage(e));

                    // DIAGNOSTIC
                    getPeer().getDiagnostic().peerRejectedUpload(
                            getPeer(), transferManager, e);
                } finally {
                    restore(ctx, wasIncremented);
                }
            }
        }
    }
}
