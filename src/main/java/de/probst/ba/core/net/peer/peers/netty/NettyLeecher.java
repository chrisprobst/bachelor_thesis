package de.probst.ba.core.net.peer.peers.netty;

import de.probst.ba.core.distribution.LeecherDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.DataInfo;
import de.probst.ba.core.net.peer.AbstractLeecher;
import de.probst.ba.core.net.peer.Leecher;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Transfer;
import de.probst.ba.core.net.peer.handler.LeecherPeerHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.datainfo.CollectDataInfoHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.discovery.AnnounceSocketAddressHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.group.ChannelGroupHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.traffic.TrafficHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.transfer.DownloadHandler;
import de.probst.ba.core.net.peer.state.BandwidthStatisticState;
import de.probst.ba.core.util.concurrent.trafficshaper.MessageSink;
import de.probst.ba.core.util.concurrent.trafficshaper.TrafficShaper;
import de.probst.ba.core.util.concurrent.trafficshaper.trafficshapers.TrafficShapers;
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
    private final TrafficShaper<Object> uploadTrafficShaper;
    private final TrafficShaper<Object> downloadTrafficShaper;
    private final LoggingHandler leecherLogHandler = new LoggingHandler(LogLevel.TRACE);
    private final long maxUploadRate;
    private final long maxDownloadRate;
    private final Bootstrap leecherBootstrap;
    private final Class<? extends Channel> leecherChannelClass;
    private final ConcurrentMap<SocketAddress, Boolean> connections = new ConcurrentHashMap<>();
    private final Map<SocketAddress, Boolean> connectionsView = Collections.unmodifiableMap(connections);
    private final ChannelInitializer<Channel> leecherChannelInitializer = new ChannelInitializer<Channel>() {

        @Override
        public void initChannel(Channel ch) {
            // Create message sinks for channel
            MessageSink<Object> uploadMessageSink =
                    uploadTrafficShaper.createMessageSink(Optional.empty(), Optional.empty());
            MessageSink<Object> downloadMessageSink =
                    downloadTrafficShaper.createMessageSink(Optional.of(() -> ch.config().setAutoRead(false)),
                                                            Optional.of(() -> ch.config().setAutoRead(true)));

            // Traffic
            ch.pipeline().addLast(new TrafficHandler(uploadMessageSink, downloadMessageSink));

            // Codec pipeline
            NettyConfig.getCodecPipeline().forEach(ch.pipeline()::addLast);

            // Log & Logic
            ch.pipeline().addLast(leecherLogHandler, leecherChannelGroupHandler);
            if (NettyConfig.isUseAutoConnect()) {
                ch.pipeline().addLast(new AnnounceSocketAddressHandler(NettyLeecher.this, announceSocketAddress));
            }
            ch.pipeline().addLast(new DownloadHandler(NettyLeecher.this, getLeechRunnable()),
                                  new CollectDataInfoHandler(NettyLeecher.this, getLeechRunnable()));
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
        return new BandwidthStatisticState(this,
                                           maxUploadRate,
                                           uploadTrafficShaper.getTotalTrafficRate(),
                                           uploadTrafficShaper.getCurrentTrafficRate(),
                                           uploadTrafficShaper.getTotalTraffic(),
                                           maxDownloadRate,
                                           downloadTrafficShaper.getTotalTrafficRate(),
                                           downloadTrafficShaper.getCurrentTrafficRate(),
                                           downloadTrafficShaper.getTotalTraffic());
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
        this.maxUploadRate = maxUploadRate;
        this.maxDownloadRate = maxDownloadRate;
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
        leecherChannelGroupHandler = new ChannelGroupHandler(leecherEventLoopGroup.next());

        // Traffic shaping
        uploadTrafficShaper = TrafficShapers.leastFirst(getLeakyUploadBucket(),
                                                        leecherEventLoopGroup.next()::submit,
                                                        NettyConfig.getResetTrafficInterval());
        downloadTrafficShaper = TrafficShapers.leastFirst(getLeakyDownloadBucket(),
                                                          leecherEventLoopGroup.next()::submit,
                                                          NettyConfig.getResetTrafficInterval());

        // Init bootstrap
        leecherBootstrap = initLeecherBootstrap();

        // Set init future
        getInitFuture().complete(this);
    }

    @Override
    public synchronized CompletableFuture<Leecher> connect(SocketAddress socketAddress) {
        Objects.requireNonNull(socketAddress);
        CompletableFuture<Leecher> connectionFuture = new CompletableFuture<>();
        Boolean previous;

        // Do not exceed the connection limit
        int limit = NettyConfig.getMaxConnectionsPerLeecher();
        if (limit > 0 && connections.size() >= limit) {
            connectionFuture.completeExceptionally(new IllegalStateException("Max connections reached"));
            return connectionFuture;
        }

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
            uploadTrafficShaper.cancel();
            downloadTrafficShaper.cancel();
        } finally {
            super.close();
        }
    }
}
