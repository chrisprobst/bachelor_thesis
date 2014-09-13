package de.probst.ba.core.net.peer.peers.netty;

import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.transfer.Transfer;
import de.probst.ba.core.net.peer.AbstractSeeder;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.handler.SeederPeerHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.codec.SimpleCodec;
import de.probst.ba.core.net.peer.peers.netty.handlers.datainfo.AnnounceDataInfoHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.discovery.DiscoverSocketAddressHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.group.ChannelGroupHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.traffic.BandwidthStatisticHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.traffic.RoundRobinTrafficShaper;
import de.probst.ba.core.net.peer.peers.netty.handlers.traffic.WriteRequestHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.transfer.UploadHandler;
import de.probst.ba.core.net.peer.state.BandwidthStatisticState;
import de.probst.ba.core.util.concurrent.LeakyBucket;
import de.probst.ba.core.util.concurrent.LeakyBucketRefillTask;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
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

    private final LeakyBucketRefillTask leakyBucketRefillTask;
    private final LeakyBucket leakyBucket;
    private final RoundRobinTrafficShaper roundRobinTrafficShaper;

    private final Object allowLock = new Object();
    private final EventLoopGroup seederEventLoopGroup;
    private final ChannelGroupHandler seederChannelGroupHandler;
    private final BandwidthStatisticHandler seederBandwidthStatisticHandler;
    private final LoggingHandler seederLogHandler = new LoggingHandler(LogLevel.TRACE);
    private final Class<? extends Channel> seederChannelClass;
    private final ChannelInitializer<Channel> seederChannelInitializer = new ChannelInitializer<Channel>() {
        @Override
        public void initChannel(Channel ch) {

            // Build pipeline
            ch.pipeline().addLast(

                    // Statistic handler
                    seederBandwidthStatisticHandler,

                    // Traffic shaper
                    new WriteRequestHandler(roundRobinTrafficShaper),

                    // Codec stuff
                    //new ComplexCodec(),
                    new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4),
                    new LengthFieldPrepender(4),
                    new SimpleCodec(),

                    // Logging
                    seederLogHandler,

                    // Group
                    seederChannelGroupHandler,

                    // Chunked uploads
                    new ChunkedWriteHandler(),

                    // Logic
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

    protected abstract ChannelFuture initSeederBootstrap(SocketAddress socketAddress);

    public AbstractNettySeeder(long maxUploadRate,
                               long maxDownloadRate,
                               SocketAddress socketAddress,
                               DataBase dataBase,
                               SeederDistributionAlgorithm seederDistributionAlgorithm,
                               Optional<SeederPeerHandler> seederHandler,
                               EventLoopGroup seederEventLoopGroup,
                               Class<? extends ServerChannel> seederChannelClass) {

        super(dataBase, seederDistributionAlgorithm, seederHandler);

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
        seederChannelGroupHandler = new ChannelGroupHandler(this.seederEventLoopGroup.next());

        // Leaky bucket
        leakyBucketRefillTask = new LeakyBucketRefillTask(seederEventLoopGroup.next(), 250);
        leakyBucket = new LeakyBucket(leakyBucketRefillTask, maxUploadRate, maxUploadRate);
        roundRobinTrafficShaper =
                new RoundRobinTrafficShaper(leakyBucket,
                                            seederEventLoopGroup.next(),
                                            () -> WriteRequestHandler.collect(getSeederChannelGroup()));

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
    public BandwidthStatisticState getBandwidthStatisticState() {
        return seederBandwidthStatisticHandler.getBandwidthStatisticState();
    }

    @Override
    public void close() throws IOException {
        try {
            seederEventLoopGroup.shutdownGracefully();
            leakyBucketRefillTask.close();
            leakyBucket.close();
        } finally {
            super.close();
        }
    }
}
