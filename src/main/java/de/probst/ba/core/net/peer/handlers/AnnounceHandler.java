package de.probst.ba.core.net.peer.handlers;

import de.probst.ba.core.Config;
import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.net.peer.handlers.messages.DataInfoMessage;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;

/**
 * Created by chrisprobst on 13.08.14.
 */
public final class AnnounceHandler extends ChannelHandlerAdapter implements Runnable {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AnnounceHandler.class);

    private final DataBase<?> dataBase;
    private ChannelHandlerContext ctx;
    private ScheduledFuture<?> timer;

    private void schedule() {
        timer = ctx.channel().eventLoop().schedule(
                this,
                Config.getDataInfoAnnounceDelay(),
                Config.getDataInfoAnnounceTimeUnit());
    }

    private void unschedule() {
        if (timer != null) {
            timer.cancel(false);
        }
    }

    public AnnounceHandler(DataBase<?> dataBase) {
        Objects.requireNonNull(dataBase);
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

    public DataBase<?> getDataBase() {
        return dataBase;
    }

    @Override
    public void run() {
        DataInfoMessage dataInfoMessage =
                new DataInfoMessage(getDataBase().getDataInfo());

        if (dataInfoMessage.getDataInfo().isEmpty()) {
            schedule();
        } else {
            ctx.writeAndFlush(dataInfoMessage)
                    .addListener(fut -> {
                        if (fut.isSuccess()) {
                            schedule();
                        } else {
                            logger.warn(
                                    "Failed to announce data info, closing connection",
                                    fut.cause());
                            ctx.close();
                        }
                    });
        }
    }
}
