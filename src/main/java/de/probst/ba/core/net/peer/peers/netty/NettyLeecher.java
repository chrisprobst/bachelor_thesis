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
import de.probst.ba.core.net.peer.peers.netty.handlers.datainfo.CollectDataInfoHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.discovery.PeerIdAnnounceHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.group.ChannelGroupHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.transfer.DownloadHandler;
import de.probst.ba.core.net.peer.state.BandwidthStatisticState;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
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
    private final Optional<PeerId> announcePeerId;
    private final BandwidthManager leecherBandwidthManager;
    private final LoggingHandler leecherLogHandler = new LoggingHandler(LogLevel.TRACE);
    private final Bootstrap leecherBootstrap;
    private final Class<? extends Channel> leecherChannelClass;
    private final ConcurrentMap<SocketAddress, Boolean> connecting = new ConcurrentHashMap<>();
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
                    new PeerIdAnnounceHandler(NettyLeecher.this, announcePeerId),
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

        Channel remotePeer = getLeecherChannelGroup().find((ChannelId) transfer.getRemotePeerId().getGuid());

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
                        boolean autoConnect,
                        EventLoopGroup leecherEventLoopGroup,
                        Class<? extends Channel> leecherChannelClass,
                        Optional<PeerId> announcePeerId) {

        super(peerId,
              dataBase,
              leecherDistributionAlgorithm,
              leecherHandler,
              autoConnect,
              leecherEventLoopGroup.next());

        Objects.requireNonNull(leecherEventLoopGroup);
        Objects.requireNonNull(leecherChannelClass);
        Objects.requireNonNull(announcePeerId);

        // Save args
        this.leecherEventLoopGroup = leecherEventLoopGroup;
        this.leecherChannelClass = leecherChannelClass;
        this.announcePeerId = announcePeerId;

        // Connect close future
        leecherEventLoopGroup.terminationFuture().addListener(fut -> {
            if (fut.isSuccess()) {
                getCloseFuture().complete(null);
            } else {
                getCloseFuture().completeExceptionally(fut.cause());
            }
        });

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
    public CompletableFuture<?> connect(PeerId peerId) {
        Objects.requireNonNull(peerId);
        if (!peerId.isConnectable()) {
            throw new IllegalArgumentException("!peerId.isConnectable()");
        }
        CompletableFuture<?> connectionFuture = new CompletableFuture<>();
        SocketAddress socketAddress = peerId.getAddress().get();
        Boolean previous;
        if ((previous = connecting.putIfAbsent(socketAddress, false)) == null) {
            logger.info("Leecher " + getPeerId() + " connecting to " + peerId);
            leecherBootstrap.connect(socketAddress).addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    connecting.remove(socketAddress);
                    logger.warn("Leecher " + getPeerId() + " failed to connect to " + peerId,
                                future.cause());
                    connectionFuture.completeExceptionally(future.cause());
                } else {
                    connecting.put(socketAddress, true);
                    logger.info("Leecher " + getPeerId() + " connected to " + peerId);
                    future.channel().closeFuture().addListener(fut -> {
                        connecting.remove(socketAddress);
                        logger.info("Leecher " + getPeerId() + " disconnected from " + peerId);
                    });
                    connectionFuture.complete(null);
                }
            });
        } else {
            connectionFuture.completeExceptionally(new IllegalStateException(previous ?
                                                                             "Already connected to " + peerId :
                                                                             "Already connecting to " + peerId));
        }

        return connectionFuture;
    }

    @Override
    public void close() throws IOException {
        leecherEventLoopGroup.shutdownGracefully();
        leecherBandwidthManager.close();
        super.close();
    }
}
