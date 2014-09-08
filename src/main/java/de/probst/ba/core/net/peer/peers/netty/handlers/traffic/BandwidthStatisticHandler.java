package de.probst.ba.core.net.peer.peers.netty.handlers.traffic;

import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.state.BandwidthStatisticState;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by chrisprobst on 04.09.14.
 */
public final class BandwidthStatisticHandler implements AutoCloseable {

    private final Peer peer;
    private final long maxUploadRate;
    private final long maxDownloadRate;
    private final GlobalTrafficShapingHandler globalStatisticHandler;

    public BandwidthStatisticHandler(Peer peer,
                                     long maxUploadRate,
                                     long maxDownloadRate,
                                     ScheduledExecutorService scheduledExecutorService) {
        Objects.requireNonNull(peer);

        this.peer = peer;
        this.maxUploadRate = maxUploadRate;
        this.maxDownloadRate = maxDownloadRate;
        globalStatisticHandler = new GlobalTrafficShapingHandler(scheduledExecutorService, 0, 0);
    }

    private long getMaxUploadRate() {
        return maxUploadRate;
    }

    private long getAverageUploadRate() {
        TrafficCounter trafficCounter = globalStatisticHandler.trafficCounter();
        long bytesWritten = trafficCounter.cumulativeWrittenBytes();
        double time = (System.currentTimeMillis() - trafficCounter.lastCumulativeTime()) / 1000.0;
        return time > 0 ? (long) (bytesWritten / time) : 0;
    }

    private long getCurrentUploadRate() {
        return globalStatisticHandler.trafficCounter().lastWriteThroughput();
    }

    private long getTotalUploaded() {
        return globalStatisticHandler.trafficCounter().cumulativeWrittenBytes();
    }

    private long getMaxDownloadRate() {
        return maxDownloadRate;
    }

    private long getAverageDownloadRate() {
        TrafficCounter trafficCounter = globalStatisticHandler.trafficCounter();
        long bytesRead = trafficCounter.cumulativeReadBytes();
        double time = (System.currentTimeMillis() - trafficCounter.lastCumulativeTime()) / 1000.0;
        return time > 0 ? (long) (bytesRead / time) : 0;
    }

    private long getCurrentDownloadRate() {
        return globalStatisticHandler.trafficCounter().lastReadThroughput();
    }

    private long getTotalDownloaded() {
        return globalStatisticHandler.trafficCounter().cumulativeReadBytes();
    }

    public GlobalTrafficShapingHandler getGlobalStatisticHandler() {
        return globalStatisticHandler;
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

    @Override
    public void close() {
        globalStatisticHandler.release();
    }
}
