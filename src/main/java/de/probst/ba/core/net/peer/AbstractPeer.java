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

import java.net.SocketAddress;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 12.08.14.
 */
public abstract class AbstractPeer {

    private final LogLevel logLevel = LogLevel.INFO;

    private final SocketAddress address;

    private final EventLoopGroup eventLoopGroup =
            createEventGroup();

    private final DataBase dataBase;

    private final ChannelGroupHandler serverChannelGroupHandler =
            new ChannelGroupHandler(eventLoopGroup.next());

    private final ChannelGroupHandler channelGroupHandler =
            new ChannelGroupHandler(eventLoopGroup.next());

    protected final ChannelInitializer<Channel> serverChannelInitializer = new ChannelInitializer<Channel>() {
        @Override
        public void initChannel(Channel ch) {
            ch.pipeline().addLast(
                    logHandler,
                    serverChannelGroupHandler,
                    new ChunkedWriteHandler(),
                    new UploadHandler(getDataBase()),
                    new AnnounceHandler(new Brain() {
                    }, dataBase)
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
                    new DataInfoHandler()
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

    public AbstractPeer(SocketAddress address, DataBase dataBase) {
        Objects.requireNonNull(address);
        Objects.requireNonNull(dataBase);

        // Save args
        this.address = address;
        this.dataBase = dataBase;

        // Init bootstrap
        initBootstrap();
        initServerBootstrap();

        // Bind to address
        initFuture = createInitFuture();
    }

    public Map<Object, Transfer> getUploads() {
        return getServerChannelGroup().stream()
                .map(c -> new AbstractMap.SimpleEntry<>(c,
                        c.pipeline().get(UploadHandler.class).getTransferManager()))
                .filter(h -> h.getValue().isPresent())
                .collect(Collectors.toMap(
                        p -> p.getKey().id(),
                        p -> p.getValue().get().getTransfer()));
    }

    public Map<Object, Transfer> getDownloads() {
        return getChannelGroup().stream()
                .map(c -> new AbstractMap.SimpleEntry<>(c, c.pipeline().get(DownloadHandler.class)))
                .filter(h -> h.getValue() != null)
                .collect(Collectors.toMap(
                        p -> p.getKey().id(),
                        p -> p.getValue().getTransferManager().getTransfer()));
    }

    public Map<String, DataInfo> getDataInfo() {
        return getDataBase().getDataInfo();
    }

    public Map<Object, Map<String, DataInfo>> getRemoteDataInfo() {
        return getChannelGroup().stream()
                .map(c -> new AbstractMap.SimpleEntry<>(
                        c, c.pipeline().get(DataInfoHandler.class).getRemoteDataInfo()))
                .filter(h -> h.getValue().isPresent())
                .collect(Collectors.toMap(
                        p -> p.getKey().id(),
                        p -> p.getValue().get()));
    }

    public DataBase getDataBase() {
        return dataBase;
    }

    public ChannelGroup getServerChannelGroup() {
        return serverChannelGroupHandler.getChannelGroup();
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
