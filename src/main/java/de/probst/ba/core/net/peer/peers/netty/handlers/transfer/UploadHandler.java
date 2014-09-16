package de.probst.ba.core.net.peer.peers.netty.handlers.transfer;

import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.peers.netty.NettyConfig;
import de.probst.ba.core.net.peer.peers.netty.handlers.transfer.messages.UploadRejectedMessage;
import de.probst.ba.core.net.peer.peers.netty.handlers.transfer.messages.UploadRequestMessage;
import de.probst.ba.core.net.peer.transfer.Transfer;
import de.probst.ba.core.net.peer.transfer.TransferManager;
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
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 13.08.14.
 */
public final class UploadHandler extends SimpleChannelInboundHandler<UploadRequestMessage> {

    public static Map<PeerId, Transfer> collectUploads(ChannelGroup channelGroup) {
        return Collections.unmodifiableMap(channelGroup.stream()
                                                       .map(UploadHandler::get)
                                                       .map(UploadHandler::getTransferManager)
                                                       .filter(Optional::isPresent)
                                                       .map(Optional::get)
                                                       .collect(Collectors.toMap(p -> p.getTransfer().getRemotePeerId(),
                                                                                 TransferManager::getTransfer)));
    }

    public static UploadHandler get(Channel remotePeer) {
        return remotePeer.pipeline().get(UploadHandler.class);
    }

    private final Logger logger = LoggerFactory.getLogger(UploadHandler.class);
    private final Seeder seeder;
    private final Object allowLock;
    private volatile TransferManager transferManager;

    public UploadHandler(Seeder seeder, Object allowLock) {
        Objects.requireNonNull(seeder);
        Objects.requireNonNull(allowLock);
        this.seeder = seeder;
        this.allowLock = allowLock;
    }

    private boolean setup(ChannelHandlerContext ctx, TransferManager transferManager) {
        boolean allowed;
        synchronized (allowLock) {
            if ((allowed = seeder.getDistributionAlgorithm().isUploadAllowed(seeder, transferManager))) {
                this.transferManager = transferManager;
            }
        }
        if (allowed) {
            ctx.channel().config().setAutoRead(false);
        }
        return allowed;
    }

    private void reset(ChannelHandlerContext ctx) {
        transferManager = null;
        ctx.channel().config().setAutoRead(true);
    }

    public Optional<TransferManager> getTransferManager() {
        return Optional.ofNullable(transferManager);
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, UploadRequestMessage msg) throws Exception {

        if (msg.getDataInfo().isEmpty()) {
            Exception cause = new IllegalArgumentException("Requested empty data info upload");
            ctx.writeAndFlush(new UploadRejectedMessage(cause));

            logger.info("Seeder " + seeder.getPeerId() + " rejected upload " + transferManager, cause);

            // HANDLER
            seeder.getPeerHandler().uploadRejected(seeder, null, cause);
        } else {

            // Create a new transfer manager
            TransferManager newTransferManager = seeder.getDataBase().createTransferManager(Transfer.upload(new PeerId(
                    ctx.channel().remoteAddress(),
                    ctx.channel().id()), msg.getDataInfo()));

            // If the upload is not allowed, reject it!
            if (!setup(ctx, newTransferManager)) {
                Exception cause = new IllegalStateException("Upload denied");
                ctx.writeAndFlush(new UploadRejectedMessage(cause));

                logger.debug("Seeder " + seeder.getPeerId() + " denied upload " + newTransferManager);

                // HANDLER
                seeder.getPeerHandler().uploadRejected(seeder, newTransferManager, cause);
            } else {
                // Upload chunked input
                ctx.writeAndFlush(new ChunkedDataBaseInput(newTransferManager)).addListener(fut -> {
                    if (fut.isSuccess()) {
                        logger.debug("Seeder " + seeder.getPeerId() + " succeeded upload " + newTransferManager);

                        // HANDLER
                        seeder.getPeerHandler().uploadSucceeded(seeder, newTransferManager);

                        // Restore
                        reset(ctx);
                    } else if (!(fut.cause() instanceof ClosedChannelException)) {
                        ctx.close();

                        logger.warn("Seeder " + seeder.getPeerId() + " failed to upload, connection closed",
                                    fut.cause());
                    }
                });

                logger.debug("Seeder " + seeder.getPeerId() + " started upload " + newTransferManager);

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
            ByteBuf byteBuf = Unpooled.buffer(NettyConfig.getUploadBufferSize());
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
