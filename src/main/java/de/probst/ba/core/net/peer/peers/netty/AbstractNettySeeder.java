package de.probst.ba.core.net.peer.peers.netty;

import de.probst.ba.core.distribution.SeederDistributionAlgorithm;
import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.transfer.Transfer;
import de.probst.ba.core.net.peer.AbstractSeeder;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.net.peer.SeederHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.codec.SimpleCodec;
import de.probst.ba.core.net.peer.peers.netty.handlers.datainfo.AnnounceHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.group.ChannelGroupHandler;
import de.probst.ba.core.net.peer.peers.netty.handlers.throttle.WriteThrottle;
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

    private final long uploadRate;

    private final EventLoopGroup seederEventLoopGroup;

    private final LoggingHandler seederLogHandler =
            new LoggingHandler(LogLevel.TRACE);

    private final ChannelGroupHandler seederChannelGroupHandler;

    private final WriteThrottle seederWriteThrottle;

    private final ChannelInitializer<Channel> seederChannelInitializer = new ChannelInitializer<Channel>() {
        @Override
        public void initChannel(Channel ch) {
               /* new ChannelTrafficShapingHandler(
            getUploadRate(),
            getDownloadRate(),
            NETTY_TRAFFIC_INTERVAL),*/

            // Build pipeline
            ch.pipeline().addLast(

                    // Codec stuff
                    new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4),
                    new LengthFieldPrepender(4),
                    new SimpleCodec(),

                    // Throttle
                    seederWriteThrottle,

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
    protected long getUploadRate() {
        return uploadRate;
    }

    @Override
    protected Map<PeerId, Transfer> getUploads() {
        return UploadHandler.getUploads(getSeederChannelGroup());
    }

    protected abstract void initSeederBootstrap();

    protected abstract EventLoopGroup createSeederEventLoopGroup();

    protected abstract ChannelFuture createSeederInitFuture();

    protected AbstractNettySeeder(long uploadRate,
                                  PeerId peerId,
                                  DataBase dataBase,
                                  SeederDistributionAlgorithm distributionAlgorithm,
                                  SeederHandler peerHandler,
                                  Optional<EventLoopGroup> seederEventLoopGroup) {

        super(peerId, dataBase, distributionAlgorithm, peerHandler);

        Objects.requireNonNull(seederEventLoopGroup);

        // Save args
        this.uploadRate = uploadRate;
        this.seederEventLoopGroup = seederEventLoopGroup.orElseGet(
                this::createSeederEventLoopGroup);

        // Create internal vars
        seederWriteThrottle = new WriteThrottle(uploadRate);
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
        super.close();
    }
}
