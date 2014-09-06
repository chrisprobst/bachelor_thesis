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
import io.netty.bootstrap.Bootstrap;
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
import java.net.SocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;

/**
 * Created by chrisprobst on 01.09.14.
 */
public final class NettyLeecher extends AbstractLeecher {

    private final Logger logger = LoggerFactory.getLogger(NettyLeecher.class);
    private final EventLoopGroup leecherEventLoopGroup;
    private final ChannelGroupHandler leecherChannelGroupHandler;
    private final BandwidthManager leecherBandwidthManager;
    private final LoggingHandler leecherLogHandler = new LoggingHandler(LogLevel.TRACE);
    private final Bootstrap leecherBootstrap;
    private final Class<? extends Channel> leecherChannelClass;
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
                    new DownloadHandler(NettyLeecher.this),
                    new CollectHandler(NettyLeecher.this));
        }
    };

    private ChannelGroup getLeecherChannelGroup() {
        return leecherChannelGroupHandler.getChannelGroup();
    }

    @Override
    protected Map<PeerId, Transfer> getDownloads() {
        return DownloadHandler.collectDownloads(getLeecherChannelGroup());
    }

    @Override
    protected Map<PeerId, Map<String, DataInfo>> getRemoteDataInfo() {
        return CollectHandler.collectRemoteDataInfo(getLeecherChannelGroup());
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

    private Bootstrap initLeecherBootstrap() {
        return new Bootstrap().group(leecherEventLoopGroup)
                              .channel(leecherChannelClass)
                              .handler(leecherChannelInitializer);
    }

    public NettyLeecher(long maxUploadRate,
                        long maxDownloadRate,
                        PeerId peerId,
                        DataBase dataBase,
                        LeecherDistributionAlgorithm leecherDistributionAlgorithm,
                        Optional<LeecherPeerHandler> leecherHandler,
                        EventLoopGroup leecherEventLoopGroup,
                        Class<? extends Channel> leecherChannelClass) {

        super(peerId, dataBase, leecherDistributionAlgorithm, leecherHandler, leecherEventLoopGroup.next());

        Objects.requireNonNull(leecherEventLoopGroup);
        Objects.requireNonNull(leecherChannelClass);

        // Save args
        this.leecherEventLoopGroup = leecherEventLoopGroup;
        this.leecherChannelClass = leecherChannelClass;

        // Create internal vars
        leecherBandwidthManager = new BandwidthManager(this, leecherEventLoopGroup, maxUploadRate, maxDownloadRate);
        leecherChannelGroupHandler = new ChannelGroupHandler(this.leecherEventLoopGroup.next());

        // Init bootstrap
        leecherBootstrap = initLeecherBootstrap();

        // Set init future
        getInitFuture().complete(null);
    }

    @Override
    public BandwidthStatisticState getBandwidthStatisticState() {
        return leecherBandwidthManager.getBandwidthStatisticState();
    }

    @Override
    public Future<?> getCloseFuture() {
        return leecherEventLoopGroup.terminationFuture();
    }

    @Override
    public void connect(SocketAddress socketAddress) {
        leecherBootstrap.connect(socketAddress);
    }

    @Override
    public void close() throws IOException {
        leecherEventLoopGroup.shutdownGracefully();
        leecherBandwidthManager.close();
        super.close();
    }
}
