package de.probst.ba.core.net.peer.peers.netty;

import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.net.peer.AbstractSeeder;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.Transfer;
import de.probst.ba.core.net.peer.handler.SeederPeerHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.datainfo.AnnounceDataInfoHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.discovery.DiscoverSocketAddressHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.group.ChannelGroupHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.traffic.TrafficHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.transfer.UploadHandler;
import de.probst.ba.core.net.peer.state.BandwidthStatisticState;
import de.probst.ba.core.util.concurrent.trafficshaper.MessageSink;
import de.probst.ba.core.util.concurrent.trafficshaper.TrafficShaper;
import de.probst.ba.core.util.concurrent.trafficshaper.trafficshapers.TrafficShapers;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by chrisprobst on 01.09.14.
 */
public abstract class AbstractNettySeeder extends AbstractSeeder {

    private final Object allowLock = new Object();
    private final EventLoopGroup seederEventLoopGroup;
    private final ChannelGroupHandler seederChannelGroupHandler;
    private final TrafficShaper<Object> uploadTrafficShaper;
    private final TrafficShaper<Object> downloadTrafficShaper;
    private final LoggingHandler seederLogHandler = new LoggingHandler(LogLevel.TRACE);
    private final long maxUploadRate;
    private final long maxDownloadRate;
    private final Class<? extends Channel> seederChannelClass;
    private final ChannelInitializer<Channel> seederChannelInitializer = new ChannelInitializer<Channel>() {
        @Override
        public void initChannel(Channel ch) {

            // Create message sinks for channel
            MessageSink<Object> uploadMessageSink =
                    uploadTrafficShaper.createMessageSink(Optional.empty(), Optional.empty());
            MessageSink<Object> downloadMessageSink =
                    downloadTrafficShaper.createMessageSink(Optional.of(() -> ch.config().setAutoRead(false)),
                                                            Optional.of(() -> ch.config().setAutoRead(true)));

            // Traffic
            ch.pipeline().addLast(new TrafficHandler(uploadMessageSink, downloadMessageSink));

            // Codec pipeline
            NettyConfig.getCodecPipeline().forEach(ch.pipeline()::addLast);

            // Log & Logic
            ch.pipeline().addLast(seederLogHandler, seederChannelGroupHandler, new ChunkedWriteHandler());
            if (NettyConfig.isUseAutoConnect()) {
                ch.pipeline().addLast(new DiscoverSocketAddressHandler(AbstractNettySeeder.this,
                                                                       getSeederChannelGroup()));
            }
            ch.pipeline().addLast(new UploadHandler(AbstractNettySeeder.this, allowLock),
                                  new AnnounceDataInfoHandler(AbstractNettySeeder.this));
        }
    };

    protected Class<? extends Channel> getSeederChannelClass() {
        return seederChannelClass;
    }

    protected ChannelInitializer<Channel> getSeederChannelInitializer() {
        return seederChannelInitializer;
    }

    protected ChannelGroup getSeederChannelGroup() {
        return seederChannelGroupHandler.getChannelGroup();
    }

    protected EventLoopGroup getSeederEventLoopGroup() {
        return seederEventLoopGroup;
    }

    @Override
    protected Map<PeerId, Transfer> getUploads() {
        return UploadHandler.collectUploads(getSeederChannelGroup());
    }

    @Override
    protected BandwidthStatisticState createBandwidthStatisticState() {
        return new BandwidthStatisticState(this,
                                           maxUploadRate,
                                           uploadTrafficShaper.getTotalTrafficRate(),
                                           uploadTrafficShaper.getCurrentTrafficRate(),
                                           uploadTrafficShaper.getTotalTraffic(),
                                           maxDownloadRate,
                                           downloadTrafficShaper.getTotalTrafficRate(),
                                           downloadTrafficShaper.getCurrentTrafficRate(),
                                           downloadTrafficShaper.getTotalTraffic());
    }

    protected abstract ChannelFuture initSeederBootstrap(SocketAddress socketAddress);

    public AbstractNettySeeder(long maxUploadRate,
                               long maxDownloadRate,
                               SocketAddress socketAddress,
                               DataBase dataBase,
                               SeederDistributionAlgorithm seederDistributionAlgorithm,
                               Optional<SeederPeerHandler> seederHandler,
                               EventLoopGroup seederEventLoopGroup,
                               Class<? extends ServerChannel> seederChannelClass) {

        super(maxUploadRate,
              maxDownloadRate,
              dataBase,
              seederDistributionAlgorithm,
              seederHandler,
              seederEventLoopGroup.next());

        Objects.requireNonNull(seederEventLoopGroup);
        Objects.requireNonNull(seederChannelClass);

        // Save args
        this.maxUploadRate = maxUploadRate;
        this.maxDownloadRate = maxDownloadRate;
        this.seederEventLoopGroup = seederEventLoopGroup;
        this.seederChannelClass = seederChannelClass;

        // Connect close future
        seederEventLoopGroup.terminationFuture().addListener(fut -> {
            if (fut.isSuccess()) {
                getCloseFuture().complete(this);
            } else {
                getCloseFuture().completeExceptionally(fut.cause());
            }
        });

        // Create internal vars
        seederChannelGroupHandler = new ChannelGroupHandler(seederEventLoopGroup.next());

        // Traffic shaping
        uploadTrafficShaper = TrafficShapers.leastFirst(getLeakyUploadBucket(),
                                                        seederEventLoopGroup.next()::submit,
                                                        NettyConfig.getResetTrafficInterval());
        downloadTrafficShaper = TrafficShapers.leastFirst(getLeakyDownloadBucket(),
                                                          seederEventLoopGroup.next()::submit,
                                                          NettyConfig.getResetTrafficInterval());

        // Init bootstrap
        initSeederBootstrap(socketAddress).addListener((ChannelFutureListener) fut -> {
            if (fut.isSuccess()) {
                setPeerId(Optional.of(new PeerId(fut.channel().localAddress(), fut.channel().id())));
                getInitFuture().complete(this);
            } else {
                getInitFuture().completeExceptionally(fut.cause());
            }
        });
    }

    @Override
    public void close() throws IOException {
        try {
            seederEventLoopGroup.shutdownGracefully();
            uploadTrafficShaper.cancel();
            downloadTrafficShaper.cancel();
        } finally {
            super.close();
        }
    }
}
