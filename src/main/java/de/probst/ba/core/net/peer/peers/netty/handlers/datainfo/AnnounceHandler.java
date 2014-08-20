package de.probst.ba.core.net.peer.peers.netty.handlers.datainfo;

import de.probst.ba.core.Config;
import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.peers.netty.NettyPeerId;
import de.probst.ba.core.net.peer.peers.netty.handlers.datainfo.messages.DataInfoMessage;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

/**
 * Periodically announces the data info from the
 * given data base.
 * <p>
 * If an error occurs during announcing,
 * the connection will be closed.
 * <p>
 * Created by chrisprobst on 13.08.14.
 */
public final class AnnounceHandler extends ChannelHandlerAdapter implements Runnable {

    private static final Logger logger =
            LoggerFactory.getLogger(AnnounceHandler.class);

    private final Peer peer;
    private ChannelHandlerContext ctx;
    private ScheduledFuture<?> timer;

    /**
     * Registers the announce task with this
     * event loop.
     */
    private void schedule() {
        timer = ctx.channel().eventLoop().schedule(
                this,
                Config.getDataInfoAnnounceDelay(),
                Config.getDataInfoAnnounceTimeUnit());
    }

    /**
     * Unregister the running task,
     * if present.
     */
    private void unschedule() {
        if (timer != null) {
            timer.cancel(false);
        }
    }

    public AnnounceHandler(Peer peer) {
        Objects.requireNonNull(peer);
        this.peer = peer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        schedule();
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        unschedule();
        super.channelInactive(ctx);
    }

    public Peer getPeer() {
        return peer;
    }

    @Override
    public void run() {
        // Create the netty peer id
        PeerId peerId = new NettyPeerId(ctx.channel());

        // Transform the data info using the brain
        Optional<Map<String, DataInfo>> transformedDataInfo =
                getPeer().getBrain().transformUploadDataInfo(
                        getPeer().getNetworkState(),
                        peerId);

        // This is actually a bug in the brain
        if (transformedDataInfo == null) {
            logger.warn("Brain returned null for " +
                    "the transformed data info");
            schedule();
            return;
        }

        // Create a new data info message
        DataInfoMessage dataInfoMessage =
                new DataInfoMessage(transformedDataInfo);

        // Write and flush the data info message
        ctx.writeAndFlush(dataInfoMessage)
                .addListener(fut -> {
                    if (fut.isSuccess()) {
                        // Success, lets schedule again
                        schedule();
                    } else {
                        // log the cause
                        logger.warn(
                                "Failed to announce data info, closing connection",
                                fut.cause());

                        // Error while writing, lets close the connection
                        ctx.close();
                    }
                });

        getPeer().getDiagnostic().announced(
                getPeer(), peerId, dataInfoMessage.getDataInfo());
    }
}
