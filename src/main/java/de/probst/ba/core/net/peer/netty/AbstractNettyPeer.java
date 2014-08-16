package de.probst.ba.core.net.peer.netty;

import de.probst.ba.core.Config;
import de.probst.ba.core.logic.Brain;
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
import de.probst.ba.core.net.peer.netty.handlers.util.FlushOnWriteHandler;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 12.08.14.
 */
public abstract class AbstractNettyPeer implements Peer {


    private static final InternalLogger logger =
            InternalLoggerFactory.getInstance(AbstractNettyPeer.class);

    private static final long NETTY_TRAFFIC_INTERVAL = 500;

    private final long uploadRate;

    private final long downloadRate;

    private final SocketAddress address;

    private final EventLoopGroup eventLoopGroup =
            createEventGroup();

    private final DataBase dataBase;

    private final Brain brain;

    private final ChannelGroupHandler serverChannelGroupHandler =
            new ChannelGroupHandler(eventLoopGroup.next());

    private final ChannelGroupHandler channelGroupHandler =
            new ChannelGroupHandler(eventLoopGroup.next());

    private final CompletableFuture<Void> initFuture =
            new CompletableFuture<>();

    private final LoggingHandler logHandler =
            new LoggingHandler(LogLevel.WARN);

    protected final ChannelInitializer<Channel> serverChannelInitializer = new ChannelInitializer<Channel>() {
        @Override
        public void initChannel(Channel ch) {
            ch.pipeline().addLast(
                    new FlushOnWriteHandler(),
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
                    new FlushOnWriteHandler(),
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

    protected void scheduleBrainWorker() {
        getEventLoopGroup().schedule(brainWorker,
                Config.getBrainDelay(),
                Config.getBrainTimeUnit());
    }

    protected void requestDownload(Transfer transfer) {
        Channel remotePeer = getChannelGroup().find(
                (ChannelId) transfer.getRemotePeerId());

        if (remotePeer == null) {
            logger.warn("The brain requested to " +
                    "download from a dead peer");
        }

        // Rquest the download
        DownloadHandler.request(
                getDataBase(),
                remotePeer,
                transfer);
    }

    protected final Runnable brainWorker = () -> {
        try {
            // Get the active network state
            NetworkState networkState = getNetworkState();

            // Let the brain generate transfers
            Optional<List<Transfer>> transfers = getBrain().process(networkState);

            // This is most likely a brain bug
            if (transfers == null) {
                logger.warn("Brain returned null for optional list of transfers");
                scheduleBrainWorker();
                return;
            }

            // The brain do not want to
            // download anything
            if (!transfers.isPresent() ||
                    transfers.get().isEmpty()) {
                scheduleBrainWorker();
                return;
            }

            // Create a list of transfers with distinct remote peer ids
            // and request them to download
            transfers.get().stream()
                    .filter(t -> !networkState.getDownloads().containsKey(t.getRemotePeerId()))
                    .collect(Collectors.groupingBy(Transfer::getRemotePeerId))
                    .entrySet().stream()
                    .filter(p -> p.getValue().size() == 1)
                    .map(p -> p.getValue().get(0))
                    .forEach(AbstractNettyPeer.this::requestDownload);

            // Rerun later
            scheduleBrainWorker();
        } catch (Exception e) {
            logger.error("The brain is dead, shutting peer down", e);
            getEventLoopGroup().shutdownGracefully();
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
        getInitFuture().thenRun(this::scheduleBrainWorker);

        // Set init future
        createInitFuture().addListener(fut -> {
            if (fut.isSuccess()) {
                initFuture.complete(null);
            } else {
                initFuture.completeExceptionally(fut.cause());
            }
        });
    }

    public ChannelGroup getServerChannelGroup() {
        return serverChannelGroupHandler.getChannelGroup();
    }

    public ChannelGroup getChannelGroup() {
        return channelGroupHandler.getChannelGroup();
    }

    public EventLoopGroup getEventLoopGroup() {
        return eventLoopGroup;
    }

    public CompletableFuture<Void> getInitFuture() {
        return initFuture;
    }

    @Override
    public Brain getBrain() {
        return brain;
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
    public long getUploadRate() {
        return uploadRate;
    }

    @Override
    public long getDownloadRate() {
        return downloadRate;
    }

    @Override
    public Map<Object, Transfer> getUploads() {
        return UploadHandler.getUploads(getServerChannelGroup());
    }

    @Override
    public Map<Object, Transfer> getDownloads() {
        return DownloadHandler.getDownloads(getChannelGroup());
    }

    @Override
    public Map<String, DataInfo> getDataInfo() {
        return getDataBase().getDataInfo();
    }

    @Override
    public Map<Object, Map<String, DataInfo>> getRemoteDataInfo() {
        return DataInfoHandler.getRemoteDataInfo(getChannelGroup());
    }

    @Override
    public DataBase getDataBase() {
        return dataBase;
    }
}
