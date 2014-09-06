package de.probst.ba.core.net.peer.peers.netty.handlers.datainfo;

import de.probst.ba.core.Config;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.peers.netty.NettyPeerId;
import de.probst.ba.core.net.peer.peers.netty.handlers.datainfo.messages.DataInfoMessage;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.ClosedChannelException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
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

    private final Logger logger = LoggerFactory.getLogger(AnnounceHandler.class);

    private final Seeder seeder;
    private ChannelHandlerContext ctx;
    private ScheduledFuture<?> timer;
    private Map<String, DataInfo> lastTransformedDataInfo = Collections.emptyMap();

    public AnnounceHandler(Seeder seeder) {
        Objects.requireNonNull(seeder);
        this.seeder = seeder;
    }

    /**
     * Schedule the announce task with this
     * event loop.
     */
    private void schedule() {
        timer = ctx.channel().eventLoop().schedule(this, Config.getAnnounceDelay(), Config.getDefaultTimeUnit());
    }

    /**
     * Cancel the running task,
     * if present.
     */
    private void cancel() {
        if (timer != null) {
            timer.cancel(false);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        schedule();
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        cancel();
        super.channelInactive(ctx);
    }

    @Override
    public void run() {
        // Create the netty seeder id
        PeerId peerId = new NettyPeerId(ctx.channel());

        // Transform the data info using the brain
        Map<String, DataInfo> transformedDataInfo =
                seeder.getDistributionAlgorithm().transformUploadDataInfo(seeder, peerId);

        // This is actually a bug in the brain
        if (transformedDataInfo == null) {
            logger.warn(
                    "Seeder " + seeder.getPeerId() + " got null for the transformed data info, check the algorithm");
            schedule();
            return;
        }

        // Do not announce the same data info twice
        if (lastTransformedDataInfo.equals(transformedDataInfo)) {
            schedule();
            return;
        }

        // Set last transformed data info
        lastTransformedDataInfo = transformedDataInfo;

        // Create a new data info message
        DataInfoMessage dataInfoMessage = new DataInfoMessage(transformedDataInfo);

        // Write and flush the data info message
        ctx.writeAndFlush(dataInfoMessage).addListener(fut -> {
            if (fut.isSuccess()) {
                // Success, lets schedule again
                schedule();
            } else {
                // Close if this exception was not expected
                if (!(fut.cause() instanceof ClosedChannelException)) {
                    ctx.close();

                    // log the cause
                    logger.warn("Seeder " + seeder.getPeerId() + " failed to announce data info, connection closed",
                                fut.cause());
                }
            }
        });

        logger.debug("Seeder " + seeder.getPeerId() + " announced " + transformedDataInfo + " to " + peerId);

        // HANDLER
        seeder.getPeerHandler().announced(seeder, peerId, dataInfoMessage.getDataInfo());
    }
}
