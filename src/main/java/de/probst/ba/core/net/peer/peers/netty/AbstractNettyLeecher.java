package de.probst.ba.core.net.peer.peers.netty;

import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.media.transfer.Transfer;
import de.probst.ba.core.net.peer.AbstractLeecher;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.handler.LeecherPeerHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.bandwidth.BandwidthManager;
import de.probst.ba.core.net.peer.peers.netty.handlers.codec.SimpleCodec;
import de.probst.ba.core.net.peer.peers.netty.handlers.datainfo.CollectHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.group.ChannelGroupHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.transfer.DownloadHandler;
import de.probst.ba.core.net.peer.state.BandwidthStatisticState;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;

/**
 * Created by chrisprobst on 01.09.14.
 */
public abstract class AbstractNettyLeecher extends AbstractLeecher {

    private final Logger logger = LoggerFactory.getLogger(AbstractNettyLeecher.class);

    private final EventLoopGroup leecherEventLoopGroup;

    private final ChannelGroupHandler leecherChannelGroupHandler;

    private final BandwidthManager leecherBandwidthManager;

    private final LoggingHandler leecherLogHandler = new LoggingHandler(LogLevel.TRACE);

    private final ChannelInitializer<Channel> leecherChannelInitializer = new ChannelInitializer<Channel>() {
        @Override
        public void initChannel(Channel ch) {
            ch.pipeline().addLast(

                    // Traffic shaper
                    leecherBandwidthManager.getGlobalTrafficShapingHandler(),

                    // Statistic handler
                    leecherBandwidthManager.getGlobalStatisticHandler(),

                    // Codec stuff
                    new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4),
                    new LengthFieldPrepender(4),
                    new SimpleCodec(),

                    // Logging
                    leecherLogHandler,

                    // Group
                    leecherChannelGroupHandler,

                    // Logic
                    new DownloadHandler(AbstractNettyLeecher.this),
                    new CollectHandler(AbstractNettyLeecher.this));

            initLeecherChannel(ch);
        }
    };

    protected AbstractNettyLeecher(long maxUploadRate,
                                   long maxDownloadRate,
                                   PeerId peerId,
                                   DataBase dataBase,
                                   LeecherDistributionAlgorithm leecherDistributionAlgorithm,
                                   Optional<LeecherPeerHandler> leecherHandler,
                                   EventLoopGroup leecherEventLoopGroup) {

        super(peerId, dataBase, leecherDistributionAlgorithm, leecherHandler, leecherEventLoopGroup.next());

        Objects.requireNonNull(leecherEventLoopGroup);

        // Save args
        this.leecherEventLoopGroup = leecherEventLoopGroup;

        // Create internal vars
        leecherBandwidthManager = new BandwidthManager(this, leecherEventLoopGroup, maxUploadRate, maxDownloadRate);
        leecherChannelGroupHandler = new ChannelGroupHandler(this.leecherEventLoopGroup.next());

        // Init bootstrap
        initLeecherBootstrap();

        // Set init future
        getInitFuture().complete(null);
    }

    protected void initLeecherChannel(Channel ch) {

    }

    protected ChannelInitializer<Channel> getLeecherChannelInitializer() {
        return leecherChannelInitializer;
    }

    protected ChannelGroup getLeecherChannelGroup() {
        return leecherChannelGroupHandler.getChannelGroup();
    }

    protected EventLoopGroup getLeecherEventLoopGroup() {
        return leecherEventLoopGroup;
    }

    @Override
    protected Map<PeerId, Transfer> getDownloads() {
        return DownloadHandler.getDownloads(getLeecherChannelGroup());
    }

    @Override
    protected Map<PeerId, Map<String, DataInfo>> getRemoteDataInfo() {
        return CollectHandler.getRemoteDataInfo(getLeecherChannelGroup());
    }

    @Override
    protected void requestDownload(Transfer transfer) {
        Objects.requireNonNull(transfer);

        Channel remotePeer = getLeecherChannelGroup().find((ChannelId) transfer.getRemotePeerId().getGuid());

        if (remotePeer == null) {
            logger.warn("The algorithm requested to download from a dead peer");
        } else {
            try {
                DownloadHandler.download(remotePeer, transfer);
            } catch (Exception e) {
                logger.warn("Failed to request download", e);
            }
        }
    }

    protected abstract void initLeecherBootstrap();

    @Override
    public BandwidthStatisticState getBandwidthStatisticState() {
        return leecherBandwidthManager.getBandwidthStatisticState();
    }

    @Override
    public Future<?> getCloseFuture() {
        return leecherEventLoopGroup.terminationFuture();
    }

    @Override
    public void close() throws IOException {
        leecherEventLoopGroup.shutdownGracefully();
        leecherBandwidthManager.close();
        super.close();
    }
}
