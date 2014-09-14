package de.probst.ba.core.net.peer.peers.netty.handlers.statistic;

import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.peers.netty.handlers.traffic.TrafficUtil;
import de.probst.ba.core.net.peer.state.BandwidthStatisticState;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by chrisprobst on 04.09.14.
 */
@ChannelHandler.Sharable
public final class BandwidthStatisticHandler extends ChannelHandlerAdapter {

    private final Peer peer;
    private final long maxUploadRate;
    private final long maxDownloadRate;
    private final Instant startTotalTimeStamp = Instant.now();
    private final AtomicLong totalWritten = new AtomicLong();
    private final AtomicLong totalRead = new AtomicLong();

    private Instant startCurrentWrittenTimeStamp = Instant.now();
    private final AtomicLong currentWritten = new AtomicLong();
    private Instant startCurrentReadTimeStamp = Instant.now();
    private final AtomicLong currentRead = new AtomicLong();

    public BandwidthStatisticHandler(Peer peer, long maxUploadRate, long maxDownloadRate) {
        Objects.requireNonNull(peer);

        this.peer = peer;
        this.maxUploadRate = maxUploadRate;
        this.maxDownloadRate = maxDownloadRate;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        totalWritten.getAndAdd(TrafficUtil.estimateMessageSize(msg));
        currentWritten.getAndAdd(TrafficUtil.estimateMessageSize(msg));
        super.write(ctx, msg, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        totalRead.getAndAdd(TrafficUtil.estimateMessageSize(msg));
        currentRead.getAndAdd(TrafficUtil.estimateMessageSize(msg));
        super.channelRead(ctx, msg);
    }

    private long getMaxUploadRate() {
        return maxUploadRate;
    }

    private long getAverageUploadRate() {
        Duration duration = Duration.between(startTotalTimeStamp, Instant.now());
        double seconds = duration.toMillis() / 1000.0;
        return seconds > 0 ? (long) (totalWritten.get() / seconds) : 0;
    }

    private long getCurrentUploadRate() {
        synchronized (currentWritten) {
            Instant now = Instant.now();
            Duration duration = Duration.between(startCurrentWrittenTimeStamp, now);
            startCurrentWrittenTimeStamp = now;

            double seconds = duration.toMillis() / 1000.0;
            long rate = seconds > 0 ? (long) (currentWritten.get() / seconds) : 0;
            currentWritten.set(0);
            return rate;
        }
    }

    private long getTotalUploaded() {
        return totalWritten.get();
    }

    private long getMaxDownloadRate() {
        return maxDownloadRate;
    }

    private long getAverageDownloadRate() {
        Duration duration = Duration.between(startTotalTimeStamp, Instant.now());
        double seconds = duration.toMillis() / 1000.0;
        return seconds > 0 ? (long) (totalRead.get() / seconds) : 0;
    }

    private long getCurrentDownloadRate() {
        synchronized (currentRead) {
            Instant now = Instant.now();
            Duration duration = Duration.between(startCurrentReadTimeStamp, now);
            startCurrentReadTimeStamp = now;

            double seconds = duration.toMillis() / 1000.0;
            long rate = seconds > 0 ? (long) (currentRead.get() / seconds) : 0;
            currentRead.set(0);
            return rate;
        }
    }

    private long getTotalDownloaded() {
        return totalRead.get();
    }

    public BandwidthStatisticState getBandwidthStatisticState() {
        return new BandwidthStatisticState(peer,
                                           getMaxUploadRate(),
                                           getAverageUploadRate(),
                                           getCurrentUploadRate(),
                                           getTotalUploaded(),

                                           getMaxDownloadRate(),
                                           getAverageDownloadRate(),
                                           getCurrentDownloadRate(),
                                           getTotalDownloaded());
    }
}
