package de.probst.ba.core.net;

import de.probst.ba.core.logic.Config;
import de.probst.ba.core.logic.DataInfo;
import de.probst.ba.core.net.handlers.ChannelGroupHandler;
import de.probst.ba.core.net.handlers.DataInfoHandler;
import de.probst.ba.core.net.handlers.messages.DataInfoMessage;
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

    protected final ChannelInitializer<Channel> serverChannelInitializer = new ChannelInitializer<Channel>() {
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

    /**
     * Periodically announces our data info to all clients.
     */
    private Runnable announcer = () -> {
        // A copy of the local data info
        DataInfoMessage dataInfoMessage = new DataInfoMessage(dataInfo);
        if (dataInfoMessage.getDataInfo().isEmpty()) {
            // We have nothing to announce so just wait
            scheduleAnnouncer();
        } else {
            getServerChannelGroup()
                    .writeAndFlush(dataInfoMessage)
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

        // Register announce scheduler
        initFuture.addListener(f -> {
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
