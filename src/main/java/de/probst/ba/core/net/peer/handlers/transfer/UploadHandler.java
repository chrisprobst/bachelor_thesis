package de.probst.ba.core.net.peer.handlers.transfer;

import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.net.Transfer;
import de.probst.ba.core.net.peer.TransferManager;
import de.probst.ba.core.net.peer.handlers.transfer.messages.DownloadRejectedMessage;
import de.probst.ba.core.net.peer.handlers.transfer.messages.DownloadRequestMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.stream.ChunkedInput;

import java.util.Objects;
import java.util.Optional;

/**
 * Created by chrisprobst on 13.08.14.
 */
public final class UploadHandler extends SimpleChannelInboundHandler<DownloadRequestMessage> {

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

    private final DataBase dataBase;
    private volatile TransferManager transferManager;

    public UploadHandler(DataBase dataBase) {
        Objects.requireNonNull(dataBase);
        this.dataBase = dataBase;
    }

    public DataBase getDataBase() {
        return dataBase;
    }

    public Optional<TransferManager> getTransferManager() {
        return Optional.ofNullable(transferManager);
    }

    /**
     * Checks the message.
     *
     * @param downloadRequestMessage
     * @return
     */
    private boolean isUploadRequestMessageValid(DownloadRequestMessage downloadRequestMessage) {
        return downloadRequestMessage != null &&
                downloadRequestMessage.getDataInfo() != null &&
                !downloadRequestMessage.getDataInfo().isEmpty();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        super.exceptionCaught(ctx, cause);
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx,
                                   DownloadRequestMessage msg) throws Exception {

        if (!isUploadRequestMessageValid(msg)) {
            ctx.writeAndFlush(new DownloadRejectedMessage(
                    new IllegalArgumentException("download request message null or empty")));
        } else {
            // Do not read while sending
            ctx.channel().config().setAutoRead(false);

            // Create a new transfer manager
            transferManager = new TransferManager(getDataBase(),
                    new Transfer(false, ctx.channel().id(), msg.getDataInfo()));

            // Upload chunked input
            ctx.writeAndFlush(new ChunkedDataBaseInput()).addListener(fut -> {

                // Upload is finished
                transferManager = null;

                if (!fut.isSuccess()) {
                    // Upload failed, notify!
                    ctx.writeAndFlush(new DownloadRejectedMessage(fut.cause()));
                }

                // Enable reading
                ctx.channel().config().setAutoRead(true);
            });
        }
    }
}
