package de.probst.ba.core.net.peer.netty;

import de.probst.ba.core.logic.Body;
import de.probst.ba.core.logic.Brain;
import de.probst.ba.core.logic.BrainWorker;
import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.NetworkState;
import de.probst.ba.core.net.Transfer;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.netty.handlers.datainfo.AnnounceHandler;
import de.probst.ba.core.net.peer.netty.handlers.datainfo.DataInfoHandler;
import de.probst.ba.core.net.peer.netty.handlers.group.ChannelGroupHandler;
import de.probst.ba.core.net.peer.netty.handlers.transfer.DownloadHandler;
import de.probst.ba.core.net.peer.netty.handlers.transfer.UploadHandler;
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
public abstract class AbstractNettyPeer implements Peer, Body {


    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(AbstractNettyPeer.class);

    private static final long NETTY_TRAFFIC_INTERVAL = 500;

    private final long uploadRate;

    private final long downloadRate;

    private final SocketAddress address;

    private final DataBase dataBase;

    private final Brain brain;

    private final BrainWorker brainWorker =
            new BrainWorker(this);

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

    protected long getUploadRate() {
        return uploadRate;
    }

    protected long getDownloadRate() {
        return downloadRate;
    }

    protected Map<Object, Transfer> getUploads() {
        return UploadHandler.getUploads(getServerChannelGroup());
    }

    protected Map<Object, Transfer> getDownloads() {
        return DownloadHandler.getDownloads(getChannelGroup());
    }

    protected Map<String, DataInfo> getDataInfo() {
        return getDataBase().getDataInfo();
    }

    protected Map<Object, Map<String, DataInfo>> getRemoteDataInfo() {
        return DataInfoHandler.getRemoteDataInfo(getChannelGroup());
    }

    protected abstract void initServerBootstrap();

    protected abstract void initBootstrap();

    protected abstract EventLoopGroup createEventGroup();

    protected abstract ChannelFuture createInitFuture();

    public AbstractNettyPeer(long uploadRate,
                             long downloadRate,
                             SocketAddress address,
                             DataBase dataBase,
                             Brain brain) {

        Objects.requireNonNull(address);
        Objects.requireNonNull(dataBase);
        Objects.requireNonNull(brain);

        // Save args
        this.uploadRate = uploadRate;
        this.downloadRate = downloadRate;
        this.address = address;
        this.dataBase = dataBase;
        this.brain = brain;

        // Init bootstrap
        initBootstrap();
        initServerBootstrap();

        // Register the brain worker for execution
        getInitFuture().thenRun(brainWorker::schedule);

        // Set init future
        createInitFuture().addListener(fut -> {
            if (fut.isSuccess()) {
                initFuture.complete(null);
            } else {
                initFuture.completeExceptionally(fut.cause());
            }
        });
    }

    // ************ INTERFACE METHODS

    @Override
    public CompletableFuture<?> getInitFuture() {
        return initFuture;
    }

    @Override
    public Future<?> getCloseFuture() {
        return getEventLoopGroup().terminationFuture();
    }

    @Override
    public Brain getBrain() {
        return brain;
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

    @Override
    public SocketAddress getAddress() {
        return address;
    }

    @Override
    public NetworkState getNetworkState() {
        return new NetworkState(
                getDownloads(),
                getDataInfo(),
                getRemoteDataInfo(),
                getUploads(),
                getUploadRate(),
                getDownloadRate());
    }

    @Override
    public DataBase getDataBase() {
        return dataBase;
    }
}
