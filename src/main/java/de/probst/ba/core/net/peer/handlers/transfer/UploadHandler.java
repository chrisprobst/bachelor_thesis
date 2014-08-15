package de.probst.ba.core.net.peer.handlers.transfer;

import de.probst.ba.core.net.Transfer;
import de.probst.ba.core.net.TransferManager;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.handlers.transfer.messages.UploadRejectedMessage;
import de.probst.ba.core.net.peer.handlers.transfer.messages.UploadRequestMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.stream.ChunkedInput;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 13.08.14.
 */
public final class UploadHandler extends SimpleChannelInboundHandler<UploadRequestMessage> {

    public static Map<Object, Transfer> getUploads(ChannelGroup channelGroup) {
        return channelGroup.stream()
                .map(c -> new AbstractMap.SimpleEntry<>(c,
                        c.pipeline().get(UploadHandler.class).getTransferManager()))
                .filter(h -> h.getValue().isPresent())
                .collect(Collectors.toMap(
                        p -> p.getKey().id(),
                        p -> p.getValue().get().getTransfer()));
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
    }

    private final Peer peer;
    private volatile TransferManager transferManager;

    public UploadHandler(Peer peer) {
        Objects.requireNonNull(peer);
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
            // Not a valid request
            ctx.writeAndFlush(new UploadRejectedMessage(
                    new IllegalArgumentException("Upload request message null or empty")));
        } else {
            TransferManager newTransferManager;

            try {
                // Create a new transfer manager
                newTransferManager = getPeer().getDataBase()
                        .createUploadTransferManager(ctx.channel().id(), msg.getDataInfo());

                // If the upload is not allowed, reject it!
                if (!getPeer().getBrain().isUploadAllowed(
                        getPeer().getNetworkState(),
                        newTransferManager.getTransfer())) {

                    // Not accepted
                    ctx.writeAndFlush(new UploadRejectedMessage(
                            new IllegalStateException(
                                    "The protocol rejected the upload")));

                } else {
                    // Set the new transfer manager
                    transferManager = newTransferManager;

                    // Do not read while sending
                    ctx.channel().config().setAutoRead(false);

                    // Upload chunked input
                    ctx.writeAndFlush(new ChunkedDataBaseInput()).addListener(fut -> {

                        // Upload is finished
                        transferManager = null;

                        // Enable reading
                        ctx.channel().config().setAutoRead(true);

                        if (!fut.isSuccess()) {
                            // Upload failed, notify!
                            ctx.writeAndFlush(new UploadRejectedMessage(fut.cause()));
                        }
                    });
                }

            } catch (Exception e) {
                // If the creation failed, reject!
                ctx.writeAndFlush(new UploadRejectedMessage(e));

                // Restore
                transferManager = null;
                ctx.channel().config().setAutoRead(true);
            }
        }
    }
}
