package de.probst.ba.core.net.peer.peers.netty;

import de.probst.ba.core.diag.Diagnostic;
import de.probst.ba.core.logic.Brain;
import de.probst.ba.core.media.DataBase;
import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.net.Transfer;
import de.probst.ba.core.net.peer.AbstractPeer;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.peers.netty.handlers.datainfo.AnnounceHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.datainfo.CollectHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.group.ChannelGroupHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.throttle.WriteThrottle;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by chrisprobst on 12.08.14.
 */
abstract class AbstractNettyPeer extends AbstractPeer {


    private final Logger logger =
            LoggerFactory.getLogger(AbstractNettyPeer.class);

//    private static final long NETTY_TRAFFIC_INTERVAL = 10;

    private final long uploadRate;

    private final long downloadRate;

    private final EventLoopGroup eventLoopGroup;

    private final ChannelGroupHandler serverChannelGroupHandler;

    private final ChannelGroupHandler channelGroupHandler;

    private final CompletableFuture<?> initFuture =
            new CompletableFuture<>();

    private final LoggingHandler logHandler =
            new LoggingHandler(LogLevel.TRACE);

    private final WriteThrottle writeThrottle;

    protected final ChannelInitializer<Channel> serverChannelInitializer = new ChannelInitializer<Channel>() {
        @Override
        public void initChannel(Channel ch) {
            ch.pipeline().addLast(

                   /* new ChannelTrafficShapingHandler(
                            getUploadRate(),
                            getDownloadRate(),
                            NETTY_TRAFFIC_INTERVAL),*/

                    writeThrottle,

                   /* new ChannelHandlerAdapter() {
                        @Override
                        public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
                            System.out.println("WRITABLE: " + ctx.channel().isWritable());
                            super.channelWritabilityChanged(ctx);
                        }
                    },*/

                    logHandler,
                    serverChannelGroupHandler,
                    new ChunkedWriteHandler(),
                    new UploadHandler(AbstractNettyPeer.this, getParallelUploads()),
                    new AnnounceHandler(AbstractNettyPeer.this)
            );

            AbstractNettyPeer.this.initServerChannel(ch);
        }
    };

    protected final ChannelInitializer<Channel> channelInitializer = new ChannelInitializer<Channel>() {
        @Override
        public void initChannel(Channel ch) {
            ch.pipeline().addLast(
/*
                    new ChannelTrafficShapingHandler(
                            getUploadRate(),
                            getDownloadRate(),
                            NETTY_TRAFFIC_INTERVAL),*/


                    logHandler,
                    channelGroupHandler,
                    new DownloadHandler(AbstractNettyPeer.this),
                    new CollectHandler(AbstractNettyPeer.this)
            );

            AbstractNettyPeer.this.initChannel(ch);
        }
    };

    protected void initChannel(Channel ch) {

    }

    protected void initServerChannel(Channel ch) {

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
    protected Map<PeerId, Transfer> getUploads() {
        return UploadHandler.getUploads(getServerChannelGroup());
    }

    @Override
    protected Map<PeerId, Transfer> getDownloads() {
        return DownloadHandler.getDownloads(getChannelGroup());
    }

    @Override
    protected Map<PeerId, Map<String, DataInfo>> getRemoteDataInfo() {
        return CollectHandler.getRemoteDataInfo(getChannelGroup());
    }

    @Override
    protected ScheduledExecutorService getScheduler() {
        return getEventLoopGroup();
    }

    @Override
    protected void requestDownload(Transfer transfer) {
        Objects.requireNonNull(transfer);

        Channel remotePeer = getChannelGroup().find(
                (ChannelId) transfer.getRemotePeerId().getGuid());

        if (remotePeer == null) {
            logger.warn("The brain requested to " +
                    "download from a dead peer");
        } else {
            try {
                DownloadHandler.download(remotePeer, transfer);
            } catch (Exception e) {
                logger.warn("Failed to request download", e);
            }
        }
    }

    protected abstract void initServerBootstrap();

    protected abstract void initBootstrap();

    protected abstract EventLoopGroup createEventGroup();

    protected abstract ChannelFuture createInitFuture();

    protected AbstractNettyPeer(long uploadRate,
                                long downloadRate,
                                PeerId localPeerId,
                                DataBase dataBase,
                                Brain brain,
                                Diagnostic diagnostic,
                                Optional<EventLoopGroup> eventLoopGroup) {

        super(localPeerId, dataBase, brain, diagnostic);

        Objects.requireNonNull(eventLoopGroup);

        // Save args
        this.uploadRate = uploadRate;
        this.downloadRate = downloadRate;
        writeThrottle = new WriteThrottle(uploadRate);
        this.eventLoopGroup = eventLoopGroup.orElseGet(
                this::createEventGroup);

        // Implement channel group handlers
        serverChannelGroupHandler = new ChannelGroupHandler(
                getEventLoopGroup().next());
        channelGroupHandler = new ChannelGroupHandler(
                getEventLoopGroup().next());

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
    public void close() throws IOException {
        getEventLoopGroup().shutdownGracefully();
        super.close();
    }
}
