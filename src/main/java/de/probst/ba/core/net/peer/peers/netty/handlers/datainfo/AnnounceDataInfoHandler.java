package de.probst.ba.core.net.peer.peers.netty.handlers.datainfo;

import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Seeder;
import de.probst.ba.core.net.peer.peers.netty.NettyPeerId;
import de.probst.ba.core.net.peer.peers.netty.handlers.datainfo.messages.DataInfoMessage;
import de.probst.ba.core.util.collections.Tuple2;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.ClosedChannelException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

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

    private final Logger logger = LoggerFactory.getLogger(AnnounceDataInfoHandler.class);

    private final Seeder seeder;
    private long token;
    private ChannelHandlerContext ctx;
    private Map<String, DataInfo> lastTransformedDataInfo = Collections.emptyMap();

    private void doAnnounce(Map<String, DataInfo> dataInfo) {
        Objects.requireNonNull(dataInfo);

        // Create the netty seeder id
        PeerId peerId = new NettyPeerId(ctx.channel());

        // Transform the data info using the algorithm
        Map<String, DataInfo> transformedDataInfo =
                seeder.getDistributionAlgorithm().transformUploadDataInfo(seeder, dataInfo, peerId);

        // Do not announce the same data info twice
        if (transformedDataInfo.equals(lastTransformedDataInfo)) {
            return;
        }

        // Set last transformed data info
        lastTransformedDataInfo = transformedDataInfo;

        // Create a new data info message
        DataInfoMessage dataInfoMessage = new DataInfoMessage(transformedDataInfo);

        // Write and flush the data info message
        ctx.writeAndFlush(dataInfoMessage).addListener(fut -> {

            // Close if this exception was not expected
            if (!fut.isSuccess() && !(fut.cause() instanceof ClosedChannelException)) {
                ctx.close();

                logger.warn("Seeder " + seeder.getPeerId() + " failed to announce data info, connection closed",
                            fut.cause());
            }
        });

        logger.debug("Seeder " + seeder.getPeerId() + " announced " + transformedDataInfo + " to " + peerId);

        // HANDLER
        seeder.getPeerHandler().announced(seeder, peerId, dataInfoMessage.getDataInfo());
    }

    public AnnounceDataInfoHandler(Seeder seeder) {
        Objects.requireNonNull(seeder);
        this.seeder = seeder;
    }

    public void announce(Map<String, DataInfo> dataInfo) {
        Objects.requireNonNull(dataInfo);
        ctx.channel().eventLoop().execute(() -> doAnnounce(dataInfo));
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // Subscribe to new data info
        Tuple2<Long, Map<String, DataInfo>> tuple = seeder.getDataBase().subscribe(this::announce);

        // Init vars
        this.ctx = ctx;
        token = tuple.first();

        // Init first state
        doAnnounce(tuple.second());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // Cancel subscription
        seeder.getDataBase().cancel(token);
        super.channelInactive(ctx);
    }
}
