package de.probst.ba.core.net.peer.peers.netty.handlers.transfer;

import de.probst.ba.core.media.database.DataBaseWriteChannel;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Transfer;
import de.probst.ba.core.net.peer.peers.netty.handlers.datainfo.CollectDataInfoHandler;
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
import java.util.Collections;
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

    public static Map<PeerId, Transfer> collectDownloads(ChannelGroup channelGroup) {
        return Collections.unmodifiableMap(channelGroup.stream()
                                                       .map(DownloadHandler::get)
                                                       .map(DownloadHandler::getTransfer)
                                                       .filter(Optional::isPresent)
                                                       .map(Optional::get)
                                                       .collect(Collectors.toMap(Transfer::getRemotePeerId,
                                                                                 Function.identity())));
    }

    public static DownloadHandler get(Channel remotePeer) {
        return remotePeer.pipeline().get(DownloadHandler.class);
    }

    public static void download(Channel remoteChannel, Transfer transfer) {

        // Make sure it is a download transfer
        if (transfer.isUpload()) {
            throw new IllegalArgumentException("transfer.isUpload()");
        }

        // Check that the ids are the same
        if (!transfer.getRemotePeerId().equals(new PeerId(remoteChannel.remoteAddress(), remoteChannel.id()))) {
            throw new IllegalArgumentException("!transfer.getRemotePeerId().equals(new NettyPeerId(remoteChannel))");
        }

        // Mark as downloading
        get(remoteChannel).download(transfer);

        // Trigger event
        remoteChannel.pipeline().fireUserEventTriggered(transfer);
    }

    private final Logger logger = LoggerFactory.getLogger(DownloadHandler.class);
    private final Leecher leecher;
    private final Runnable leech;
    private final AtomicReference<Transfer> transfer = new AtomicReference<>();
    private DataBaseWriteChannel dataBaseWriteChannel;
    private boolean receivedBuffer;

    public DownloadHandler(Leecher leecher, Runnable leech) {
        Objects.requireNonNull(leecher);
        Objects.requireNonNull(leech);
        this.leecher = leecher;
        this.leech = leech;
    }

    private void download(Transfer transfer) {
        Objects.requireNonNull(transfer);

        if (this.transfer.getAndSet(transfer) != null) {
            throw new IllegalStateException("this.transfer.getAndSet(transfer) != null");
        }
    }

    private void setup() throws IOException {
        receivedBuffer = false;
        Transfer transfer = this.transfer.get();
        dataBaseWriteChannel = leecher.getDataBase()
                                      .insert(transfer.getDataInfo())
                                      .orElseThrow(() -> new IllegalStateException(
                                              "Database write channel locked for: " + transfer));
    }

    private Transfer update(ByteBuf buffer) throws IOException {
        int total = buffer.readableBytes();
        while (buffer.readableBytes() > 0) {
            buffer.readBytes(dataBaseWriteChannel, buffer.readableBytes());
        }

        return transfer.updateAndGet(t -> t.update(t.getCompletedSize() + total));
    }

    private void reset() throws IOException {
        dataBaseWriteChannel.close();
        dataBaseWriteChannel = null;
        transfer.set(null);
        leech.run();
    }

    private Optional<Transfer> getTransfer() {
        return Optional.ofNullable(transfer.get());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // Make sure the channel is definitely closed
        if (dataBaseWriteChannel != null) {
            dataBaseWriteChannel.close();
            dataBaseWriteChannel = null;
        }

        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (dataBaseWriteChannel != null) {
            throw new IllegalStateException("dataBaseWriteChannel != null");
        }

        Transfer transfer = this.transfer.get();
        if (transfer.equals(evt)) {

            // Setup vars
            setup();

            // Write the download request
            ctx.writeAndFlush(new UploadRequestMessage(transfer.getDataInfo()));

            logger.debug("Leecher " + leecher.getPeerId() + " requested download " + transfer);

            // HANDLER
            leecher.getPeerHandler().downloadRequested(leecher, transfer);
        }

        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Transfer transfer = this.transfer.get();
        if (msg instanceof UploadRejectedMessage) {
            UploadRejectedMessage uploadRejectedMessage = (UploadRejectedMessage) msg;

            logger.debug("Leecher " + leecher.getPeerId() + " requested the download " + transfer +
                         ", but was rejected");

            // HANDLER
            leecher.getPeerHandler().downloadRejected(leecher, transfer, uploadRejectedMessage.getCause());

            // Very important:
            // Remove the rejected data info from the remote data info
            CollectDataInfoHandler.get(ctx.channel()).removeDataInfo(transfer.getDataInfo());

            reset();
        } else if (msg instanceof ByteBuf) {
            ByteBuf buffer = (ByteBuf) msg;

            // First buffer ? -> Download started!
            if (!receivedBuffer) {
                receivedBuffer = true;
                logger.debug("Leecher " + leecher.getPeerId() + " started download " + transfer);

                // HANDLER
                leecher.getPeerHandler().downloadStarted(leecher, transfer);
            }

            // Process the buffer and check for completion
            boolean completed = (transfer = update(buffer)).isCompleted();

            logger.debug("Leecher " + leecher.getPeerId() + " progressed download " + transfer);

            // HANDLER
            leecher.getPeerHandler().downloadProgressed(leecher, transfer);

            if (completed) {
                // Reset and prepare for next transfer
                reset();

                logger.debug("Leecher " + leecher.getPeerId() + " succeeded download " + transfer);

                // HANDLER
                leecher.getPeerHandler().downloadSucceeded(leecher, transfer);

                // Query data base
                DataInfo dataInfo = leecher.getDataBase().get(transfer.getDataInfo().getHash());

                if (dataInfo != null && dataInfo.isCompleted()) {
                    logger.info("Leecher " + leecher.getPeerId() + " completed the data " + dataInfo + " with " +
                                transfer);

                    // HANDLER
                    leecher.getPeerHandler().dataCompleted(leecher, dataInfo, transfer);
                }
            }

            // Nobody is gonna use this
            buffer.release();
        } else {
            super.channelRead(ctx, msg);
        }
    }
}
