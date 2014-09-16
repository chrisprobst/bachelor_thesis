package de.probst.ba.core.net.peer.peers.netty;

import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.AbstractLeecher;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.handler.LeecherPeerHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.datainfo.CollectDataInfoHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.discovery.AnnounceSocketAddressHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.group.ChannelGroupHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.statistic.BandwidthStatisticHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.traffic.MessageQueueHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.traffic.TrafficShapers;
import de.probst.ba.core.net.peer.peers.netty.handlers.transfer.DownloadHandler;
import de.probst.ba.core.net.peer.state.BandwidthStatisticState;
import de.probst.ba.core.net.peer.transfer.Transfer;
import de.probst.ba.core.util.concurrent.CancelableRunnable;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by chrisprobst on 01.09.14.
 */
public final class NettyLeecher extends AbstractLeecher {

    private final Logger logger = LoggerFactory.getLogger(NettyLeecher.class);
    private final EventLoopGroup leecherEventLoopGroup;
    private final ChannelGroupHandler leecherChannelGroupHandler;
    private final Optional<SocketAddress> announceSocketAddress;
    private final BandwidthStatisticHandler leecherBandwidthStatisticHandler;
    private final Optional<CancelableRunnable> leastWrittenFirstTrafficShaper;
    private final Optional<CancelableRunnable> leastReadFirstTrafficShaper;
    private final LoggingHandler leecherLogHandler = new LoggingHandler(LogLevel.TRACE);
    private final Bootstrap leecherBootstrap;
    private final Class<? extends Channel> leecherChannelClass;
    private final ConcurrentMap<SocketAddress, Boolean> connections = new ConcurrentHashMap<>();
    private final Map<SocketAddress, Boolean> connectionsView = Collections.unmodifiableMap(connections);
    private final ChannelInitializer<Channel> leecherChannelInitializer = new ChannelInitializer<Channel>() {

        @Override
        public void initChannel(Channel ch) {

            // Statistic & Traffic
            ch.pipeline().addLast(leecherBandwidthStatisticHandler,
                                  new MessageQueueHandler(leastWrittenFirstTrafficShaper, leastReadFirstTrafficShaper));

            // Codec pipeline
            NettyConfig.getCodecPipeline().forEach(ch.pipeline()::addLast);

            // Log & Logic
            ch.pipeline().addLast(leecherLogHandler,
                                  leecherChannelGroupHandler,
                                  new AnnounceSocketAddressHandler(NettyLeecher.this, announceSocketAddress),
                                  new DownloadHandler(NettyLeecher.this),
                                  new CollectDataInfoHandler(NettyLeecher.this));
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
        return CollectDataInfoHandler.collectRemoteDataInfo(getLeecherChannelGroup());
    }

    @Override
    protected void requestDownload(Transfer transfer) {
        Objects.requireNonNull(transfer);

        Channel remotePeer = getLeecherChannelGroup().find((ChannelId) transfer.getRemotePeerId().getUniqueId());

        if (remotePeer == null) {
            logger.warn("Leecher " + getPeerId() + " has a algorithm which requested to download from a dead peer");
        } else {
            try {
                DownloadHandler.download(remotePeer, transfer);
            } catch (Exception e) {
                logger.warn("Leecher " + getPeerId() + " failed to request download", e);
            }
        }
    }

    @Override
    protected BandwidthStatisticState createBandwidthStatisticState() {
        return leecherBandwidthStatisticHandler.getBandwidthStatisticState();
    }

    private Bootstrap initLeecherBootstrap() {
        return new Bootstrap().group(leecherEventLoopGroup)
                              .channel(leecherChannelClass)
                              .handler(leecherChannelInitializer);
    }

    public NettyLeecher(long maxUploadRate,
                        long maxDownloadRate,
                        Optional<PeerId> peerId,
                        DataBase dataBase,
                        LeecherDistributionAlgorithm leecherDistributionAlgorithm,
                        Optional<LeecherPeerHandler> leecherHandler,
                        boolean autoConnect,
                        EventLoopGroup leecherEventLoopGroup,
                        Class<? extends Channel> leecherChannelClass,
                        Optional<SocketAddress> announceSocketAddress) {

        super(maxUploadRate,
              maxDownloadRate,
              peerId,
              dataBase,
              leecherDistributionAlgorithm,
              leecherHandler,
              leecherEventLoopGroup.next(),
              autoConnect,
              leecherEventLoopGroup.next());

        Objects.requireNonNull(leecherEventLoopGroup);
        Objects.requireNonNull(leecherChannelClass);
        Objects.requireNonNull(announceSocketAddress);

        // Save args
        this.leecherEventLoopGroup = leecherEventLoopGroup;
        this.leecherChannelClass = leecherChannelClass;
        this.announceSocketAddress = announceSocketAddress;

        // Connect close future
        leecherEventLoopGroup.terminationFuture().addListener(fut -> {
            if (fut.isSuccess()) {
                getCloseFuture().complete(this);
            } else {
                getCloseFuture().completeExceptionally(fut.cause());
            }
        });

        // Create internal vars
        leecherBandwidthStatisticHandler = new BandwidthStatisticHandler(this, maxUploadRate, maxDownloadRate);
        leecherChannelGroupHandler = new ChannelGroupHandler(leecherEventLoopGroup.next());

        // Traffic shaping
        leastWrittenFirstTrafficShaper = getLeakyUploadBucket().map(leakyBucket -> TrafficShapers.leastWrittenFirst(
                leecherEventLoopGroup.next()::submit,
                leakyBucket,
                () -> MessageQueueHandler.collect(getLeecherChannelGroup())));
        leastReadFirstTrafficShaper = getLeakyDownloadBucket().map(leakyBucket -> TrafficShapers.leastReadFirst(
                leecherEventLoopGroup.next()::submit,
                leakyBucket,
                () -> MessageQueueHandler.collect(getLeecherChannelGroup())));


        // Init bootstrap
        leecherBootstrap = initLeecherBootstrap();

        // Set init future
        getInitFuture().complete(this);
    }

    @Override
    public CompletableFuture<Leecher> connect(SocketAddress socketAddress) {
        Objects.requireNonNull(socketAddress);
        CompletableFuture<Leecher> connectionFuture = new CompletableFuture<>();
        Boolean previous;


        if ((previous = connections.putIfAbsent(socketAddress, false)) == null) {
            logger.debug("Leecher " + getPeerId() + " connecting to " + socketAddress);
            leecherBootstrap.connect(socketAddress).addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    connections.remove(socketAddress);
                    logger.warn("Leecher " + getPeerId() + " failed to connect to " + socketAddress,
                                future.cause());
                    connectionFuture.completeExceptionally(future.cause());
                } else {
                    connections.put(socketAddress, true);
                    logger.debug("Leecher " + getPeerId() + " connected to " + socketAddress);
                    future.channel().closeFuture().addListener(fut -> {
                        connections.remove(socketAddress);
                        logger.debug("Leecher " + getPeerId() + " disconnected from " + socketAddress);
                    });
                    connectionFuture.complete(this);
                }
            });
        } else {
            connectionFuture.completeExceptionally(new IllegalStateException(previous ?
                                                                             "Already connected to " + socketAddress :
                                                                             "Already connecting to " + socketAddress));
        }

        return connectionFuture;
    }

    @Override
    public Map<SocketAddress, Boolean> getConnections() {
        return connectionsView;
    }

    @Override
    public void close() throws IOException {
        try {
            leecherEventLoopGroup.shutdownGracefully();
            leastWrittenFirstTrafficShaper.ifPresent(CancelableRunnable::cancel);
            leastReadFirstTrafficShaper.ifPresent(CancelableRunnable::cancel);
        } finally {
            super.close();
        }
    }
}
