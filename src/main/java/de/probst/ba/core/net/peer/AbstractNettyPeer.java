package de.probst.ba.core.net.peer;

import de.probst.ba.core.logic.Brain;
import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.Transfer;
import de.probst.ba.core.net.peer.handlers.ChannelGroupHandler;
import de.probst.ba.core.net.peer.handlers.datainfo.AnnounceHandler;
import de.probst.ba.core.net.peer.handlers.datainfo.DataInfoHandler;
import de.probst.ba.core.net.peer.handlers.transfer.DownloadHandler;
import de.probst.ba.core.net.peer.handlers.transfer.UploadHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;

import java.net.SocketAddress;
import java.util.Map;
import java.util.Objects;

/**
 * Created by chrisprobst on 12.08.14.
 */
public abstract class AbstractNettyPeer implements Peer {

    private static final long NETTY_TRAFFIC_SHAPING_INTERVAL = 500;

    private final LogLevel logLevel = LogLevel.INFO;

    private final SocketAddress address;

    private final EventLoopGroup eventLoopGroup =
            createEventGroup();

    private final DataBase dataBase;

    private final Brain brain;

    private final ChannelGroupHandler serverChannelGroupHandler =
            new ChannelGroupHandler(eventLoopGroup.next());

    private final ChannelGroupHandler channelGroupHandler =
            new ChannelGroupHandler(eventLoopGroup.next());

    private final ChannelFuture initFuture;

    private final LoggingHandler logHandler =
            new LoggingHandler(logLevel);

    protected final ChannelInitializer<Channel> serverChannelInitializer = new ChannelInitializer<Channel>() {
        @Override
        public void initChannel(Channel ch) {
            ch.pipeline().addLast(
                    new ChannelTrafficShapingHandler(
                            getUploadRate(),
                            getDownloadRate(),
                            NETTY_TRAFFIC_SHAPING_INTERVAL),

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
                            NETTY_TRAFFIC_SHAPING_INTERVAL),

                    logHandler,

                    // Manages all outgoing client channels
                    // for downloading data and receiving
                    // data info announcements
                    channelGroupHandler,

                    // Only the clients receive announcements
                    // from other peers
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

    protected abstract void initServerBootstrap();

    protected abstract void initBootstrap();

    protected abstract EventLoopGroup createEventGroup();

    protected abstract ChannelFuture createInitFuture();

    public AbstractNettyPeer(SocketAddress address, DataBase dataBase, Brain brain) {

        Objects.requireNonNull(address);
        Objects.requireNonNull(dataBase);
        Objects.requireNonNull(brain);

        // Save args
        this.address = address;
        this.dataBase = dataBase;
        this.brain = brain;

        // Init bootstrap
        initBootstrap();
        initServerBootstrap();

        // Set init future
        initFuture = createInitFuture();
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

    public ChannelFuture getInitFuture() {
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
        return 1000;
    }

    @Override
    public long getDownloadRate() {
        return 1000;
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
