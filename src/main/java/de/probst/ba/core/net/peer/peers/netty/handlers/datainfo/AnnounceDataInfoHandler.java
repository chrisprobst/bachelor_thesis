package de.probst.ba.core.net.peer.peers.netty.handlers.datainfo;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.peers.netty.handlers.datainfo.messages.DataInfoMessage;
import de.probst.ba.core.util.collections.Tuple2;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.ClosedChannelException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * Periodically announces the data info from the
 * given data base.
 * <p>
 * If an error occurs during announcing,
 * the connection will be closed.
 * <p>
 * Created by chrisprobst on 13.08.14.
 */
public final class AnnounceDataInfoHandler extends ChannelHandlerAdapter {

    public static void announce(ChannelGroup channelGroup, Tuple2<Long, Map<String, DataInfo>> dataInfoWithStamp) {
        channelGroup.stream().map(AnnounceDataInfoHandler::get).forEach(h -> h.announce(dataInfoWithStamp));
    }

    public static AnnounceDataInfoHandler get(Channel remotePeer) {
        return remotePeer.pipeline().get(AnnounceDataInfoHandler.class);
    }

    private final Logger logger = LoggerFactory.getLogger(AnnounceDataInfoHandler.class);

    private final Seeder seeder;
    private volatile ChannelHandlerContext ctx;
    private volatile PeerId peerId;
    private long token;
    private OptionalLong stamp = OptionalLong.empty();
    private volatile Map<String, DataInfo> nextDataInfo = Collections.emptyMap();
    private boolean scheduled;

    private void doAnnounce() {
        Map<String, DataInfo> dataInfo = nextDataInfo;

        // Create a new data info message
        DataInfoMessage dataInfoMessage = new DataInfoMessage(dataInfo);

        // Write and flush the data info message
        ctx.writeAndFlush(dataInfoMessage).addListener(fut -> {
            if (fut.isSuccess()) {
                boolean execute = false;
                synchronized (this) {
                    if (!nextDataInfo.equals(dataInfo)) {
                        execute = true;
                    } else {
                        scheduled = false;
                    }
                }
                if (execute) {
                    ctx.channel().eventLoop().execute(this::doAnnounce);
                }
            } else if (!(fut.cause() instanceof ClosedChannelException)) {
                ctx.close();

                logger.warn("Seeder " + seeder.getPeerId() + " failed to announce data info to " + peerId +
                            ", connection closed", fut.cause());
            }
        });

        logger.debug("Seeder " + seeder.getPeerId() + " announced " + dataInfo + " to " + peerId);

        // HANDLER
        seeder.getPeerHandler().announced(seeder, peerId, dataInfoMessage.getDataInfo());
    }

    public AnnounceDataInfoHandler(Seeder seeder) {
        Objects.requireNonNull(seeder);
        this.seeder = seeder;
    }

    public void announce(Tuple2<Long, Map<String, DataInfo>> dataInfoWithStamp) {
        Objects.requireNonNull(dataInfoWithStamp);

        // Transform the data info using the algorithm
        Map<String, DataInfo> transformedDataInfo =
                seeder.getDistributionAlgorithm().transformUploadDataInfo(seeder, dataInfoWithStamp.second(), peerId);

        if (transformedDataInfo == null) {
            logger.warn("Seeder " + seeder.getPeerId() + " transformed data info to null");
            return;
        }

        boolean execute = false;
        synchronized (this) {
            if (!transformedDataInfo.equals(nextDataInfo) &&
                (!stamp.isPresent() || stamp.getAsLong() < dataInfoWithStamp.first())) {
                nextDataInfo = transformedDataInfo;
                stamp = OptionalLong.of(dataInfoWithStamp.first());
                if (!scheduled) {
                    execute = scheduled = true;
                }
            }
        }

        if (execute) {
            ctx.channel().eventLoop().execute(this::doAnnounce);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        // Init vars
        this.ctx = ctx;
        peerId = new PeerId(ctx.channel().remoteAddress(), ctx.channel().id());
        token = seeder.getDataBase().subscribe(this::announce);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // Cancel subscription
        seeder.getDataBase().cancel(token);
        super.channelInactive(ctx);
    }
}
