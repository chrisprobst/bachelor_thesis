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

import java.io.IOException;
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
                .map(UploadHandler::getTransferManager)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(
                        p -> p.getTransfer().getRemotePeerId(),
                        TransferManager::getTransfer));
    }

    public static UploadHandler get(Channel remotePeer) {
        return remotePeer.pipeline().get(UploadHandler.class);
    }

    private static final class ChunkedDataBaseInput implements ChunkedInput<ByteBuf> {

        private final TransferManager transferManager;
        private volatile boolean abort = false;

        public ChunkedDataBaseInput(TransferManager transferManager) {
            Objects.requireNonNull(transferManager);
            this.transferManager = transferManager;
        }

        public void abort() {
            abort = true;
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
            if (abort) {
                throw new IOException("Aborted");
            }
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
            ChunkedDataBaseInput chunkedDataBaseInput = null;
            boolean wasIncremented = false;
            try {
                // Create a new transfer manager
                TransferManager newTransferManager = peer.getDataBase()
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
                    peer.getDiagnostic().uploadRejected(
                            peer, newTransferManager, cause);
                } else {
                    logger.debug("Starting upload: " + newTransferManager.getTransfer());

                    // Create the message
                    chunkedDataBaseInput = new ChunkedDataBaseInput(newTransferManager);

                    // Upload chunked input
                    ctx.writeAndFlush(chunkedDataBaseInput).addListener(fut -> {
                        try {
                            if (!fut.isSuccess()) {
                                logger.warn("Upload failed", fut.cause());

                                // Upload failed, notify!
                                ctx.writeAndFlush(new UploadRejectedMessage(fut.cause()));

                                // DIAGNOSTIC
                                peer.getDiagnostic().uploadRejected(
                                        peer, transferManager, fut.cause());
                            } else {
                                logger.debug("Upload succeeded");

                                // DIAGNOSTIC
                                peer.getDiagnostic().uploadSucceeded(
                                        peer, transferManager);
                            }
                        } finally {
                            restore(ctx, true);
                        }
                    });

                    // DIAGNOSTIC
                    peer.getDiagnostic().uploadStarted(
                            peer, newTransferManager);
                }

            } catch (Exception e) {
                try {
                    logger.warn("Upload failed", e);

                    // Abort upload
                    if (chunkedDataBaseInput != null) {
                        chunkedDataBaseInput.abort();
                    }

                    // If the creation failed, reject!
                    ctx.writeAndFlush(new UploadRejectedMessage(e));

                    // DIAGNOSTIC
                    peer.getDiagnostic().uploadRejected(
                            peer, transferManager, e);
                } finally {
                    restore(ctx, wasIncremented);
                }
            }
        }
    }
}
