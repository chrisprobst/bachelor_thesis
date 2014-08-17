package de.probst.ba.core.net.peer.peers.netty;

import de.probst.ba.core.logic.Body;
import de.probst.ba.core.logic.Brain;
import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.Transfer;
import de.probst.ba.core.net.peer.AbstractPeer;
import de.probst.ba.core.net.peer.peers.netty.handlers.datainfo.AnnounceHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.datainfo.DataInfoHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.group.ChannelGroupHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.transfer.DownloadHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.transfer.UploadHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.SocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by chrisprobst on 12.08.14.
 */
abstract class AbstractNettyPeer extends AbstractPeer implements Body {


    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(AbstractNettyPeer.class);

    private static final long NETTY_TRAFFIC_INTERVAL = 500;

    private final long uploadRate;

    private final long downloadRate;

    private final EventLoopGroup eventLoopGroup =
            createEventGroup();

    private final ChannelGroupHandler serverChannelGroupHandler =
            new ChannelGroupHandler(eventLoopGroup.next());

    private final ChannelGroupHandler channelGroupHandler =
            new ChannelGroupHandler(eventLoopGroup.next());

    private final CompletableFuture<?> initFuture =
            new CompletableFuture<>();

    private final LoggingHandler logHandler =
            new LoggingHandler(LogLevel.WARN);

    protected final ChannelInitializer<Channel> serverChannelInitializer = new ChannelInitializer<Channel>() {
        @Override
        public void initChannel(Channel ch) {
            ch.pipeline().addLast(

                    new ChannelTrafficShapingHandler(
                            getUploadRate(),
                            getDownloadRate(),
                            NETTY_TRAFFIC_INTERVAL),

                    logHandler,
                    serverChannelGroupHandler,
                    new ChunkedWriteHandler(),
                    new UploadHandler(AbstractNettyPeer.this),
                    new AnnounceHandler(AbstractNettyPeer.this)
            );
        }
    };

    protected final ChannelInitializer<Channel> channelInitializer = new ChannelInitializer<Channel>() {
        @Override
        public void initChannel(Channel ch) {
            ch.pipeline().addLast(

                    new ChannelTrafficShapingHandler(
                            getUploadRate(),
                            getDownloadRate(),
                            NETTY_TRAFFIC_INTERVAL),

                    logHandler,
                    channelGroupHandler,
                    new DataInfoHandler()
            );
        }
    };

    protected ChannelInitializer<Channel> getServerChannelInitializer() {
        return serverChannelInitializer;
    }

    protected ChannelInitializer<Channel> getChannelInitializer() {
        return channelInitializer;
    }

    protected LoggingHandler getLogHandler() {
        return logHandler;
    }

    protected ChannelGroup getServerChannelGroup() {
        return serverChannelGroupHandler.getChannelGroup();
    }

    protected ChannelGroup getChannelGroup() {
        return channelGroupHandler.getChannelGroup();
    }

    protected EventLoopGroup getEventLoopGroup() {
        return eventLoopGroup;
    }

    @Override
    protected long getUploadRate() {
        return uploadRate;
    }

    @Override
    protected long getDownloadRate() {
        return downloadRate;
    }

    @Override
    protected Map<Object, Transfer> getUploads() {
        return UploadHandler.getUploads(getServerChannelGroup());
    }

    @Override
    protected Map<Object, Transfer> getDownloads() {
        return DownloadHandler.getDownloads(getChannelGroup());
    }

    @Override
    protected Map<String, DataInfo> getDataInfo() {
        return getDataBase().getDataInfo();
    }

    @Override
    protected Map<Object, Map<String, DataInfo>> getRemoteDataInfo() {
        return DataInfoHandler.getRemoteDataInfo(getChannelGroup());
    }

    @Override
    protected Body getBody() {
        return this;
    }

    protected abstract void initServerBootstrap();

    protected abstract void initBootstrap();

    protected abstract EventLoopGroup createEventGroup();

    protected abstract ChannelFuture createInitFuture();

    protected AbstractNettyPeer(long uploadRate,
                                long downloadRate,
                                SocketAddress localAddress,
                                DataBase dataBase,
                                Brain brain) {

        super(localAddress, dataBase, brain);

        // Save args
        this.uploadRate = uploadRate;
        this.downloadRate = downloadRate;

        // Init bootstrap
        initBootstrap();
        initServerBootstrap();

        // Set init future
        createInitFuture().addListener(fut -> {
            if (fut.isSuccess()) {
                getInitFuture().complete(null);
            } else {
                getInitFuture().completeExceptionally(fut.cause());
            }
        });
    }

    // ************ INTERFACE METHODS

    @Override
    public Future<?> getCloseFuture() {
        return getEventLoopGroup().terminationFuture();
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return getEventLoopGroup();
    }

    @Override
    public void requestTransfer(Transfer transfer) {
        Objects.requireNonNull(transfer);

        Channel remotePeer = getChannelGroup().find(
                (ChannelId) transfer.getRemotePeerId());

        if (remotePeer == null) {
            logger.warn("The brain requested to " +
                    "download from a dead peer");
        }

        // Request the download
        DownloadHandler.request(
                getDataBase(),
                remotePeer,
                transfer);
    }

    @Override
    public void close() {
        getEventLoopGroup().shutdownGracefully();
    }
}
