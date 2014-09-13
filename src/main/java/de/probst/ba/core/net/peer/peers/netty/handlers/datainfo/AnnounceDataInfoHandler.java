package de.probst.ba.core.net.peer.peers.netty.handlers.datainfo;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.peers.netty.handlers.datainfo.messages.DataInfoMessage;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class AnnounceDataInfoHandler extends ChannelHandlerAdapter implements Runnable {

    public static final long ANNOUNCE_DELAY = 250;

    private final Logger logger = LoggerFactory.getLogger(AnnounceDataInfoHandler.class);

    private final Seeder seeder;
    private ChannelHandlerContext ctx;
    private PeerId peerId;
    private ScheduledFuture<?> timer;
    private Map<String, DataInfo> lastDataInfo = Collections.emptyMap();

    private void schedule() {
        timer = ctx.channel().eventLoop().schedule(this, ANNOUNCE_DELAY, TimeUnit.MILLISECONDS);
    }

    private void cancel() {
        if (timer != null) {
            timer.cancel(false);
        }
    }

    public AnnounceDataInfoHandler(Seeder seeder) {
        Objects.requireNonNull(seeder);
        this.seeder = seeder;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        peerId = new PeerId(ctx.channel().remoteAddress(), ctx.channel().id());
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
        Map<String, DataInfo> transformedDataInfo = seeder.getDistributionAlgorithm()
                                                          .transformUploadDataInfo(seeder,
                                                                                   seeder.getDataBase().getDataInfo(),
                                                                                   peerId);

        if (transformedDataInfo == null) {
            logger.warn("Seeder " + seeder.getPeerId() +
                        " has an algorithm which returned null for the transformed data info");
            schedule();
            return;
        }

        if (transformedDataInfo.equals(lastDataInfo)) {
            schedule();
            return;
        }

        lastDataInfo = transformedDataInfo;
        ctx.writeAndFlush(new DataInfoMessage(transformedDataInfo)).addListener(fut -> {
            if (fut.isSuccess()) {
                schedule();

                // HANDLER
                seeder.getPeerHandler().announced(seeder, peerId, transformedDataInfo);
            }
        });
    }
}