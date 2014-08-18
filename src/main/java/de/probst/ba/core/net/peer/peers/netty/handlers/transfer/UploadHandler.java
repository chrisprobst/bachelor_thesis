package de.probst.ba.core.net.peer.peers.netty.handlers.transfer;

import de.probst.ba.core.net.Transfer;
import de.probst.ba.core.net.TransferManager;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.peers.netty.handlers.transfer.messages.UploadRejectedMessage;
import de.probst.ba.core.net.peer.peers.netty.handlers.transfer.messages.UploadRequestMessage;
import de.probst.ba.core.util.Tuple;
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

    public static Map<Object, Transfer> getUploads(ChannelGroup channelGroup) {
        return channelGroup.stream()
                .map(c -> Tuple.of(c,
                        c.pipeline().get(UploadHandler.class).getTransferManager()))
                .filter(h -> h.second().isPresent())
                .collect(Collectors.toMap(
                        p -> p.first().id(),
                        p -> p.second().get().getTransfer()));
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
            ByteBuf byteBuf = Unpooled.buffer(8192);
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

    private void setup(ChannelHandlerContext ctx, TransferManager transferManager) {
        this.transferManager = transferManager;
        ctx.channel().config().setAutoRead(false);
    }

    private void restore(ChannelHandlerContext ctx) {
        transferManager = null;
        ctx.channel().config().setAutoRead(true);
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
            logger.warn("Upload request message null or empty");

            // Not a valid request
            ctx.writeAndFlush(new UploadRejectedMessage(
                    new IllegalArgumentException("Upload request message null or empty")));
        } else {
            TransferManager newTransferManager;

            try {
                // Create a new transfer manager
                newTransferManager = getPeer().getDataBase()
                        .createTransferManager(Transfer.upload(
                                ctx.channel().id(), msg.getDataInfo()));

                // If the upload is not allowed, reject it!
                if (!parallelUploads.tryIncrement(getPeer().getBrain().getMaxParallelUploads())) {
                    logger.warn("Reached maximum parallel uploads, rejecting upload");

                    // Not accepted
                    ctx.writeAndFlush(new UploadRejectedMessage(
                            new IllegalStateException(
                                    "The brain rejected the upload, because it " +
                                            "reached the maximum number of uploads")));

                } else {
                    System.out.println("MAX PARALLEL UPLOADS: " + parallelUploads.get());

                    // Setup
                    setup(ctx, newTransferManager);

                    logger.info("Starting upload: " + newTransferManager.getTransfer());

                    // Upload chunked input
                    ctx.writeAndFlush(new ChunkedDataBaseInput()).addListener(fut -> {
                        try {
                            restore(ctx);

                            if (!fut.isSuccess()) {
                                logger.info("Upload failed", fut.cause());

                                // Upload failed, notify!
                                ctx.writeAndFlush(new UploadRejectedMessage(fut.cause()));
                            } else {
                                logger.info("Upload succeeded");
                            }
                        } finally {
                            parallelUploads.tryDecrement();
                        }
                    });
                }

            } catch (Exception e) {
                logger.info("Upload failed", e);

                // If the creation failed, reject!
                ctx.writeAndFlush(new UploadRejectedMessage(e));

                // Restore
                restore(ctx);
            }
        }
    }
}
