package de.probst.ba.core.net.peer.peers.netty.handlers.transfer;

import de.probst.ba.core.media.transfer.Transfer;
import de.probst.ba.core.media.transfer.TransferManager;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;
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

import java.nio.channels.ClosedChannelException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 13.08.14.
 */
public final class UploadHandler extends SimpleChannelInboundHandler<UploadRequestMessage> {

    private final Logger logger = LoggerFactory.getLogger(UploadHandler.class);
    private final Seeder seeder;
    private final AtomicCounter parallelUploads;
    private volatile TransferManager transferManager;

    public UploadHandler(Seeder seeder, AtomicCounter parallelUploads) {
        Objects.requireNonNull(seeder);
        Objects.requireNonNull(parallelUploads);
        this.seeder = seeder;
        this.parallelUploads = parallelUploads;
    }

    public static Map<PeerId, Transfer> getUploads(ChannelGroup channelGroup) {
        return channelGroup.stream()
                           .map(UploadHandler::get)
                           .map(UploadHandler::getTransferManager)
                           .filter(Optional::isPresent)
                           .map(Optional::get)
                           .collect(Collectors.toMap(p -> p.getTransfer().getRemotePeerId(),
                                                     TransferManager::getTransfer));
    }

    public static UploadHandler get(Channel remotePeer) {
        return remotePeer.pipeline().get(UploadHandler.class);
    }

    private boolean setup(ChannelHandlerContext ctx, TransferManager transferManager) {
        if (parallelUploads.tryIncrement(seeder.getDistributionAlgorithm().getMaxParallelUploads(seeder))) {
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

    public Optional<TransferManager> getTransferManager() {
        return Optional.ofNullable(transferManager);
    }

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
    protected void messageReceived(ChannelHandlerContext ctx, UploadRequestMessage msg) throws Exception {

        if (!isUploadRequestMessageValid(msg)) {
            Exception cause = new IllegalArgumentException("Upload request message null or empty");

            logger.info("Seeder " + seeder.getPeerId() +
                                " rejected upload " + transferManager, cause);

            ctx.writeAndFlush(new UploadRejectedMessage(cause));

            // HANDLER
            seeder.getPeerHandler().uploadRejected(seeder, null, cause);
        } else {

            // Create a new transfer manager
            TransferManager newTransferManager = seeder.getDataBase()
                                                       .createTransferManager(Transfer.upload(new NettyPeerId(ctx.channel()),
                                                                                              msg.getDataInfo()));

            // If the upload is not allowed, reject it!
            if (!setup(ctx, newTransferManager)) {
                Exception cause = new IllegalStateException("Maximum number of uploads reached");

                logger.info("Seeder " + seeder.getPeerId() +
                                    " rejected upload " + transferManager, cause);

                ctx.writeAndFlush(new UploadRejectedMessage(cause));

                // HANDLER
                seeder.getPeerHandler().uploadRejected(seeder, newTransferManager, cause);
            } else {
                logger.debug("Seeder " + seeder.getPeerId() +
                                     " started upload " + newTransferManager);

                // Upload chunked input
                ctx.writeAndFlush(new ChunkedDataBaseInput(newTransferManager)).addListener(fut -> {
                    if (!fut.isSuccess()) {
                        // Close if this exception was not expected
                        if (!(fut.cause() instanceof ClosedChannelException)) {
                            ctx.close();

                            logger.warn("Seeder " + seeder.getPeerId() +
                                                " failed to upload, connection closed", fut.cause());
                        }
                    } else {
                        logger.debug("Seeder " + seeder.getPeerId() +
                                             " succeeded upload " + transferManager);

                        // HANDLER
                        seeder.getPeerHandler().uploadSucceeded(seeder, transferManager);

                        // Restore
                        reset(ctx);
                    }
                });

                // HANDLER
                seeder.getPeerHandler().uploadStarted(seeder, newTransferManager);
            }
        }
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
            ByteBuf byteBuf = Unpooled.buffer(4096);
            try {
                transferManager.process(byteBuf);
            } catch (Exception e) {
                e.printStackTrace();
            }
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
}
