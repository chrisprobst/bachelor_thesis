package de.probst.ba.core.net;

import de.probst.ba.core.logic.Config;
import de.probst.ba.core.logic.DataInfo;
import de.probst.ba.core.net.handlers.ChannelGroupHandler;
import de.probst.ba.core.net.handlers.DataInfoHandler;
import de.probst.ba.core.net.handlers.messages.DataInfoMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by chrisprobst on 12.08.14.
 */
public abstract class AbstractPeer {

    private final LogLevel logLevel = LogLevel.INFO;

    private final ConcurrentMap<String, DataInfo> dataInfo =
            new ConcurrentHashMap<>();

    private final SocketAddress address;

    private final EventLoopGroup eventLoopGroup =
            createEventGroup();

    private final DataInfoHandler dataInfoHandler =
            new DataInfoHandler();

    private final ChannelGroupHandler channelGroupHandler =
            new ChannelGroupHandler(eventLoopGroup.next());

    private final ChannelGroupHandler serverChannelGroupHandler =
            new ChannelGroupHandler(eventLoopGroup.next());

    private final ServerBootstrap localServerBootstrap =
            new ServerBootstrap();

    private final ChannelFuture bindFuture;

    private final Bootstrap localBootstrap =
            new Bootstrap();

    private LoggingHandler logHandler =
            new LoggingHandler(logLevel);

    /**
     * Periodically announces our data info to all clients.
     */
    private Runnable announcer = () -> {
        // A copy of the local data info
        Map<String, DataInfo> dataInfoCopy = new HashMap<>(dataInfo);
        if (dataInfoCopy.isEmpty()) {
            // We have nothing to announce so just wait
            scheduleAnnouncer();
        } else {
            getServerChannelGroup()
                    .writeAndFlush(new DataInfoMessage(dataInfoCopy))
                    .addListener(f -> {
                        if (f.isSuccess()) {
                            // The announce process was successful,
                            // reschedule this task later again
                            scheduleAnnouncer();
                        } else {
                            /*
                            TODO: Something went wrong
                            Maybe there is a better way of handling
                            those situations.
                             */
                            scheduleAnnouncer();
                        }
                    });
        }
    };

    private void scheduleAnnouncer() {
        getEventLoopGroup().schedule(announcer,
                Config.getDataInfoAnnounceDelay(),
                Config.getDataInfoAnnounceTimeUnit());
    }


    private void initServerBootstrap() {
        localServerBootstrap
                .group(eventLoopGroup)
                .channel(getServerChannelClass())
                .handler(logHandler)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    public void initChannel(Channel ch) {
                        ch.pipeline().addLast(
                                logHandler,

                                // Manages all incoming client channels
                                // for uploading data and announcing
                                // data info
                                serverChannelGroupHandler
                        );
                    }

                });
    }

    private void initBootstrap() {
        localBootstrap
                .group(eventLoopGroup)
                .channel(getChannelClass())
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    public void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(
                                logHandler,

                                // Manages all outgoing client channels
                                // for downloading data and receiving
                                // data info announcements
                                channelGroupHandler,

                                // Only the clients receive announcements
                                // from other peers
                                dataInfoHandler);
                    }
                });
    }

    protected abstract EventLoopGroup createEventGroup();

    protected abstract Class<? extends ServerChannel> getServerChannelClass();

    protected abstract Class<? extends Channel> getChannelClass();

    public AbstractPeer(SocketAddress address) {
        Objects.requireNonNull(address);

        // Save args
        this.address = address;

        // Init bootstrap
        initBootstrap();
        initServerBootstrap();

        // Bind to address
        bindFuture = getServerBootstrap().bind(getAddress());

        // Register announce scheduler
        bindFuture.addListener(f -> {
            if (f.isSuccess()) {
                scheduleAnnouncer();
            } else {
                // TODO: Better error handling
                f.cause().printStackTrace();

                // Failed to create server, this is not
                // going to work =(
                getEventLoopGroup().shutdownGracefully();
            }
        });
    }

    public ConcurrentMap<String, DataInfo> getDataInfo() {
        return dataInfo;
    }

    public Map<Object, Map<String, DataInfo>> getRemoteDataInfo() {
        return dataInfoHandler.getRemoteDataInfo();
    }

    public ChannelGroup getChannelGroup() {
        return channelGroupHandler.getChannelGroup();
    }

    public ChannelGroup getServerChannelGroup() {
        return serverChannelGroupHandler.getChannelGroup();
    }

    public ServerBootstrap getServerBootstrap() {
        return localServerBootstrap;
    }

    public Bootstrap getBootstrap() {
        return localBootstrap;
    }

    public SocketAddress getAddress() {
        return address;
    }

    public EventLoopGroup getEventLoopGroup() {
        return eventLoopGroup;
    }

    public ChannelFuture getBindFuture() {
        return bindFuture;
    }

    public ChannelFuture connect(SocketAddress remotePeer) {
        return getBootstrap().connect(remotePeer);
    }
}
