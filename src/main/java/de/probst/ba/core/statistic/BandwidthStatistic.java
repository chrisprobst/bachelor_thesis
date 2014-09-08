package de.probst.ba.core.statistic;

import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.state.BandwidthStatisticState;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

/**
 * Created by chrisprobst on 04.09.14.
 */
public final class BandwidthStatistic extends AbstractScheduledStatistic {

    public enum BandwidthStatisticMode {
        Peer,
        TotalMedian,
        TotalAccumulated
    }

    private final Collection<Peer> peers;
    private final Function<BandwidthStatisticState, Number> bandwidthMapper;
    private final BandwidthStatisticMode bandwidthStatisticMode;

    public BandwidthStatistic(Path csvPath,
                              ScheduledExecutorService scheduledExecutorService,
                              long delay,
                              Collection<Peer> peers,
                              Function<BandwidthStatisticState, Number> bandwidthMapper,
                              BandwidthStatisticMode bandwidthStatisticMode) {
        super(csvPath, scheduledExecutorService, delay);
        Objects.requireNonNull(peers);
        Objects.requireNonNull(bandwidthMapper);
        Objects.requireNonNull(bandwidthStatisticMode);
        this.peers = peers;
        this.bandwidthMapper = bandwidthMapper;
        this.bandwidthStatisticMode = bandwidthStatisticMode;
    }

    private void writeHeader() {
        csv.writeElement("Time");

        if (bandwidthStatisticMode != BandwidthStatisticMode.Peer) {
            csv.writeElement("Total bandwidth");
        } else {
            peers.stream()
                 .forEach(peer -> csv.writeElement(peer.getPeerId().getSocketAddress()));
        }

        csv.writeLine();
    }

    private void writeTotalBandwidth() {
        double totalUpload = 0;
        for (Peer peer : peers) {
            totalUpload += bandwidthMapper.apply(peer.getBandwidthStatisticState()).doubleValue();
        }
        double upload =
                bandwidthStatisticMode == BandwidthStatisticMode.TotalMedian ? totalUpload / peers.size() : totalUpload;

        if (Double.isFinite(upload)) {
            csv.writeElement(upload);
        }
    }

    private void writeIndividualBandwidth() {
        peers.stream()
             .forEach(peer -> csv.writeElement(bandwidthMapper.apply(peer.getBandwidthStatisticState()).doubleValue()));
    }

    @Override
    protected void doWriteStatistic() {
        if (csv.isFirstElement()) {
            writeHeader();
        }

        csv.writeDuration();

        if (bandwidthStatisticMode == BandwidthStatisticMode.Peer) {
            writeIndividualBandwidth();
        } else {
            writeTotalBandwidth();
        }

        csv.writeLine();
    }
}
