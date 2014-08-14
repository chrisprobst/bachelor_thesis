package de.probst.ba.core.net.peer.handlers.transfer;

import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.net.peer.handlers.transfer.messages.DownloadRejectedMessage;
import de.probst.ba.core.net.peer.handlers.transfer.messages.DownloadRequestMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Objects;

/**
 * Created by chrisprobst on 13.08.14.
 */
@ChannelHandler.Sharable
public final class UploadHandler extends SimpleChannelInboundHandler<DownloadRequestMessage> {

    private final DataBase dataBase;

    /**
     * Checks the message.
     *
     * @param downloadRequestMessage
     * @return
     */
    private boolean isUploadRequestMessageValid(DownloadRequestMessage downloadRequestMessage) {
        return downloadRequestMessage != null &&
                downloadRequestMessage.getDataInfo() != null;
    }

    public UploadHandler(DataBase dataBase) {
        Objects.requireNonNull(dataBase);
        this.dataBase = dataBase;
    }

    public DataBase getDataBase() {
        return dataBase;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx,
                                   DownloadRequestMessage msg) throws Exception {
        ctx.writeAndFlush(new DownloadRejectedMessage(new Exception("Service not implemented yet")));
    }
}
