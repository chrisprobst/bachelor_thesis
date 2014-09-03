package de.probst.ba.core.net.peer.peers.netty;

import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.transfer.Transfer;
import de.probst.ba.core.net.peer.AbstractSeeder;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.handler.SeederHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.codec.SimpleCodec;
import de.probst.ba.core.net.peer.peers.netty.handlers.datainfo.AnnounceHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.group.ChannelGroupHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.transfer.UploadHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;

/**
 * Created by chrisprobst on 01.09.14.
 */
public abstract class AbstractNettySeeder extends AbstractSeeder {

    private final Logger logger =
            LoggerFactory.getLogger(AbstractNettySeeder.class);

    private final long maxUploadRate;

    private final EventLoopGroup seederEventLoopGroup;

    private final LoggingHandler seederLogHandler =
            new LoggingHandler(LogLevel.TRACE);

    private final ChannelGroupHandler seederChannelGroupHandler;

    private final GlobalTrafficShapingHandler globalSeederTrafficShapingHandler;

    private final ChannelInitializer<Channel> seederChannelInitializer = new ChannelInitializer<Channel>() {
        @Override
        public void initChannel(Channel ch) {

            // Build pipeline
            ch.pipeline().addLast(

                    // Traffic shaper
                    globalSeederTrafficShapingHandler,

                    // Codec stuff
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
                    new UploadHandler(AbstractNettySeeder.this, getParallelUploads()),
                    new AnnounceHandler(AbstractNettySeeder.this)
            );

            // Initilaize seeder channel
            initSeederChannel(ch);
        }
    };

    protected void initSeederChannel(Channel ch) {

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
    protected long getMaxUploadRate() {
        return maxUploadRate;
    }

    @Override
    protected long getAverageUploadRate() {
        return 0;
    }

    @Override
    protected long getCurrentUploadRate() {
        return 0;
    }

    @Override
    protected long getTotalUploaded() {
        return 0;
    }

    @Override
    protected Map<PeerId, Transfer> getUploads() {
        return UploadHandler.getUploads(getSeederChannelGroup());
    }

    protected abstract void initSeederBootstrap();

    protected abstract ChannelFuture createSeederInitFuture();

    protected AbstractNettySeeder(long maxUploadRate,
                                  PeerId peerId,
                                  DataBase dataBase,
                                  SeederDistributionAlgorithm seederDistributionAlgorithm,
                                  Optional<SeederHandler> seederHandler,
                                  EventLoopGroup seederEventLoopGroup) {

        super(peerId, dataBase, seederDistributionAlgorithm, seederHandler);

        Objects.requireNonNull(seederEventLoopGroup);

        // Save args
        this.maxUploadRate = maxUploadRate;
        this.seederEventLoopGroup = seederEventLoopGroup;

        // Create internal vars
        globalSeederTrafficShapingHandler =
                new GlobalTrafficShapingHandler(
                        this.seederEventLoopGroup, getMaxUploadRate(), 0, 0);
        seederChannelGroupHandler = new ChannelGroupHandler(
                this.seederEventLoopGroup.next());

        // Init bootstrap
        initSeederBootstrap();

        // Set init future
        createSeederInitFuture().addListener(fut -> {
            if (fut.isSuccess()) {
                getInitFuture().complete(null);
            } else {
                getInitFuture().completeExceptionally(fut.cause());
            }
        });
    }

    @Override
    public Future<?> getCloseFuture() {
        return seederEventLoopGroup.terminationFuture();
    }

    @Override
    public void close() throws IOException {
        seederEventLoopGroup.shutdownGracefully();
        globalSeederTrafficShapingHandler.release();
        super.close();
    }
}
