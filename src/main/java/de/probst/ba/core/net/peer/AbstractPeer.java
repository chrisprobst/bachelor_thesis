package de.probst.ba.core.net.peer;

import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.media.databases.DefaultDataBase;
import de.probst.ba.core.net.peer.handlers.AnnounceHandler;
import de.probst.ba.core.net.peer.handlers.ChannelGroupHandler;
import de.probst.ba.core.net.peer.handlers.DataInfoHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.net.SocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by chrisprobst on 12.08.14.
 */
public abstract class AbstractPeer {

    private final LogLevel logLevel = LogLevel.INFO;

    private final SocketAddress address;

    private final EventLoopGroup eventLoopGroup =
            createEventGroup();

    private final DataBase<?> dataBase =
            new DefaultDataBase();

    private final DataInfoHandler dataInfoHandler =
            new DataInfoHandler();

    private final ChannelGroupHandler channelGroupHandler =
            new ChannelGroupHandler(eventLoopGroup.next());

    protected final ChannelInitializer<Channel> serverChannelInitializer = new ChannelInitializer<Channel>() {
        @Override
        public void initChannel(Channel ch) {
            ch.pipeline().addLast(
                    logHandler,
                    new AnnounceHandler(dataBase)
            );
        }
    };

    protected final ChannelInitializer<Channel> channelInitializer = new ChannelInitializer<Channel>() {
        @Override
        public void initChannel(Channel ch) {
            ch.pipeline().addLast(
                    logHandler,

                    // Manages all outgoing client channels
                    // for downloading data and receiving
                    // data info announcements
                    channelGroupHandler,

                    // Only the clients receive announcements
                    // from other peers
                    dataInfoHandler
            );
        }
    };

    private final ChannelFuture initFuture;

    private LoggingHandler logHandler =
            new LoggingHandler(logLevel);

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

    public AbstractPeer(SocketAddress address) {
        Objects.requireNonNull(address);

        // Save args
        this.address = address;

        // Init bootstrap
        initBootstrap();
        initServerBootstrap();

        // Bind to address
        initFuture = createInitFuture();
    }

    public ConcurrentMap<String, DataInfo> getDataInfo() {
        return dataBase.getDataInfo();
    }

    public Map<Object, Map<String, DataInfo>> getRemoteDataInfo() {
        return dataInfoHandler.getRemoteDataInfo();
    }

    public ChannelGroup getChannelGroup() {
        return channelGroupHandler.getChannelGroup();
    }

    public SocketAddress getAddress() {
        return address;
    }

    public EventLoopGroup getEventLoopGroup() {
        return eventLoopGroup;
    }

    public ChannelFuture getInitFuture() {
        return initFuture;
    }
}
