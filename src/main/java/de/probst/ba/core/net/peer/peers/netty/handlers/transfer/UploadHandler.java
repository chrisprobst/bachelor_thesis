package de.probst.ba.core.net.peer.peers.netty.handlers.transfer;

import de.probst.ba.core.media.database.DataBaseReadChannel;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.Transfer;
import de.probst.ba.core.net.peer.peers.netty.NettyConfig;
import de.probst.ba.core.net.peer.peers.netty.handlers.transfer.messages.UploadRejectedMessage;
import de.probst.ba.core.net.peer.peers.netty.handlers.transfer.messages.UploadRequestMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.stream.ChunkedNioStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.ClosedChannelException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 13.08.14.
 */
public final class UploadHandler extends SimpleChannelInboundHandler<UploadRequestMessage>
        implements ChannelProgressiveFutureListener {

    public static Map<PeerId, Transfer> collectUploads(ChannelGroup channelGroup) {
        return Collections.unmodifiableMap(channelGroup.stream()
                                                       .map(UploadHandler::get)
                                                       .map(UploadHandler::getTransfer)
                                                       .filter(Optional::isPresent)
                                                       .map(Optional::get)
                                                       .collect(Collectors.toMap(Transfer::getRemotePeerId,
                                                                                 t -> t)));
    }

    public static UploadHandler get(Channel remotePeer) {
        return remotePeer.pipeline().get(UploadHandler.class);
    }

    private final Logger logger = LoggerFactory.getLogger(UploadHandler.class);
    private final Seeder seeder;
    private final Object allowLock;
    private ChannelHandlerContext ctx;
    private volatile Transfer transfer;

    private void reject(String reason, Transfer transfer, BiConsumer<String, Throwable> logFunction) {
        Exception cause = new IllegalStateException(reason);
        ctx.writeAndFlush(new UploadRejectedMessage(cause));

        logFunction.accept("Seeder " + seeder.getPeerId() + " rejected upload " + transfer, cause);

        // HANDLER
        seeder.getPeerHandler().uploadRejected(seeder, transfer, cause);
    }

    private boolean setup(Transfer transfer) {
        boolean allowed;
        synchronized (allowLock) {
            if (this.transfer != null) {
                throw new IllegalStateException("this.transfer != null");
            }

            if ((allowed = seeder.getDistributionAlgorithm().isUploadAllowed(seeder, transfer))) {
                this.transfer = transfer;
            }
        }
        if (allowed) {
            ctx.channel().config().setAutoRead(false);
        }
        return allowed;
    }

    private void reset() {
        transfer = null;
        ctx.channel().config().setAutoRead(true);
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, UploadRequestMessage msg) throws Exception {

        if (msg.getDataInfo().isEmpty()) {
            reject("Requested empty data info upload", null, logger::warn);
        } else {
            // Create a new transfer
            Transfer transfer =
                    Transfer.upload(new PeerId(ctx.channel().remoteAddress(), ctx.channel().id()), msg.getDataInfo());

            // The byte channel
            Optional<DataBaseReadChannel> dataBaseReadChannel;

            // If the upload is not allowed, reject it!
            if (!setup(transfer)) {
                reject("Upload denied", transfer, logger::debug);
            } else if (!(dataBaseReadChannel = seeder.getDataBase().lookup(transfer.getDataInfo())).isPresent()) {
                reject("Failed to open database channel", transfer, logger::warn);
            } else {

                // Upload chunked input
                ctx.writeAndFlush(new ChunkedNioStream(dataBaseReadChannel.get(), NettyConfig.getUploadBufferSize()),
                                  ctx.newProgressivePromise()).addListener(this);

                logger.debug("Seeder " + seeder.getPeerId() + " started upload " + transfer);

                // HANDLER
                seeder.getPeerHandler().uploadStarted(seeder, transfer);
            }
        }
    }

    public UploadHandler(Seeder seeder, Object allowLock) {
        Objects.requireNonNull(seeder);
        Objects.requireNonNull(allowLock);
        this.seeder = seeder;
        this.allowLock = allowLock;
    }

    public Optional<Transfer> getTransfer() {
        return Optional.ofNullable(transfer);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        super.channelActive(ctx);
    }

    @Override
    public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) throws Exception {
        transfer = transfer.update(progress);
    }

    @Override
    public void operationComplete(ChannelProgressiveFuture future) throws Exception {
        if (future.isSuccess()) {
            logger.debug("Seeder " + seeder.getPeerId() + " succeeded upload " + transfer);

            // HANDLER
            seeder.getPeerHandler().uploadSucceeded(seeder, transfer);

            // Restore
            reset();
        } else if (!(future.cause() instanceof ClosedChannelException)) {
            ctx.close();

            logger.warn("Seeder " + seeder.getPeerId() + " failed to upload, connection closed", future.cause());
        }
    }
}
