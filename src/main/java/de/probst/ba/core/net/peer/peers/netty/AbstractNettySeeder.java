package de.probst.ba.core.net.peer.peers.netty;

import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.net.peer.AbstractSeeder;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.handler.SeederPeerHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.datainfo.AnnounceDataInfoHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.discovery.DiscoverSocketAddressHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.group.ChannelGroupHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.statistic.BandwidthStatisticHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.traffic.MessageQueueHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.traffic.TrafficShapers;
import de.probst.ba.core.net.peer.peers.netty.handlers.transfer.UploadHandler;
import de.probst.ba.core.net.peer.state.BandwidthStatisticState;
import de.probst.ba.core.net.peer.Transfer;
import de.probst.ba.core.util.concurrent.CancelableRunnable;
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
    private final BandwidthStatisticHandler seederBandwidthStatisticHandler;
    private final Optional<CancelableRunnable> leastWrittenFirstTrafficShaper;
    private final Optional<CancelableRunnable> leastReadFirstTrafficShaper;
    private final LoggingHandler seederLogHandler = new LoggingHandler(LogLevel.TRACE);
    private final Class<? extends Channel> seederChannelClass;
    private final ChannelInitializer<Channel> seederChannelInitializer = new ChannelInitializer<Channel>() {
        @Override
        public void initChannel(Channel ch) {

            // Statistic & Traffic
            ch.pipeline().addLast(seederBandwidthStatisticHandler,
                                  new MessageQueueHandler(leastWrittenFirstTrafficShaper, leastReadFirstTrafficShaper));

            // Codec pipeline
            NettyConfig.getCodecPipeline().forEach(ch.pipeline()::addLast);

            // Log & Logic
            ch.pipeline().addLast(seederLogHandler,
                                  seederChannelGroupHandler,
                                  new ChunkedWriteHandler(),
                                  new DiscoverSocketAddressHandler(AbstractNettySeeder.this, getSeederChannelGroup()),
                                  new UploadHandler(AbstractNettySeeder.this, allowLock),
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
        return seederBandwidthStatisticHandler.getBandwidthStatisticState();
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
        seederBandwidthStatisticHandler = new BandwidthStatisticHandler(this, maxUploadRate, maxDownloadRate);
        seederChannelGroupHandler = new ChannelGroupHandler(seederEventLoopGroup.next());

        // Traffic shaping
        leastWrittenFirstTrafficShaper = getLeakyUploadBucket().map(leakyBucket -> TrafficShapers.leastWrittenFirst(
                seederEventLoopGroup.next()::submit,
                leakyBucket,
                () -> MessageQueueHandler.collect(getSeederChannelGroup())));
        leastReadFirstTrafficShaper = getLeakyDownloadBucket().map(leakyBucket -> TrafficShapers.leastReadFirst(
                seederEventLoopGroup.next()::submit,
                leakyBucket,
                () -> MessageQueueHandler.collect(getSeederChannelGroup())));


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
            leastWrittenFirstTrafficShaper.ifPresent(CancelableRunnable::cancel);
            leastReadFirstTrafficShaper.ifPresent(CancelableRunnable::cancel);
        } finally {
            super.close();
        }
    }
}
