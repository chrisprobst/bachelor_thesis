package de.probst.ba.core.net.peer.handlers.datainfo;

import de.probst.ba.core.Config;
import de.probst.ba.core.logic.Brain;
import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.peer.handlers.datainfo.messages.DataInfoMessage;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

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

    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(AnnounceHandler.class);

    private final Brain brain;
    private final DataBase dataBase;
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

    public AnnounceHandler(Brain brain,
                           DataBase dataBase) {
        Objects.requireNonNull(brain);
        Objects.requireNonNull(dataBase);
        this.brain = brain;
        this.dataBase = dataBase;
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

    public DataBase getDataBase() {
        return dataBase;
    }

    public Brain getBrain() {
        return brain;
    }

    @Override
    public void run() {
        // Transform the data info using the brain
        Optional<Map<String, DataInfo>> transformedDataInfo =
                //getBrain().transformUploadDataInfo(null, getDataBase().getDataInfo());
                Optional.of(getDataBase().getDataInfo());

        // Fail fast
        if (!transformedDataInfo.isPresent()) {
            schedule();
            return;
        }

        // Create a new data info message
        DataInfoMessage dataInfoMessage =
                new DataInfoMessage(transformedDataInfo.get());

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
    }
}
