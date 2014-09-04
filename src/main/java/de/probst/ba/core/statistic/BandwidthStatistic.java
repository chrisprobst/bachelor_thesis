package de.probst.ba.core.statistic;

import de.probst.ba.core.Config;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.state.BandwidthStatisticState;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

/**
 * Created by chrisprobst on 04.09.14.
 */
public final class BandwidthStatistic extends AbstractScheduledStatistic {

    private final Queue<Peer> peers;
    private final Function<BandwidthStatisticState, Number> bandwidthMapper;
    private final Mode mode;

    public BandwidthStatistic(Path csvPath,
                              ScheduledExecutorService scheduledExecutorService,
                              long delay,
                              Queue<Peer> peers,
                              Function<BandwidthStatisticState, Number> bandwidthMapper,
                              Mode mode) {
        super(csvPath, scheduledExecutorService, delay);
        Objects.requireNonNull(peers);
        Objects.requireNonNull(bandwidthMapper);
        Objects.requireNonNull(mode);
        this.peers = peers;
        this.bandwidthMapper = bandwidthMapper;
        this.mode = mode;
    }

    private void writeHeader() {
        csv.writeElement("Time", Config.getDefaultCVSElementWidth());

        if (mode != Mode.Peer) {
            csv.writeElement("Total bandwidth", Config.getDefaultCVSElementWidth());
        } else {
            peers.stream()
                 .forEach(peer -> csv.writeElement(peer.getPeerId().getAddress(), Config.getDefaultCVSElementWidth()));
        }

        csv.writeLine();
    }

    private void writeTotalBandwidth() {
        double totalUpload = 0;
        for (Peer peer : peers) {
            totalUpload += bandwidthMapper.apply(peer.getBandwidthStatisticState()).doubleValue();
        }
        double upload = mode == Mode.TotalMedian ? totalUpload / peers.size() : totalUpload;

        csv.writeElement(upload, Config.getDefaultCVSElementWidth());
    }

    private void writeIndividualBandwidth() {
        peers.stream()
             .forEach(peer -> csv.writeElement(bandwidthMapper.apply(peer.getBandwidthStatisticState()).doubleValue(),
                                               Config.getDefaultCVSElementWidth()));
    }

    @Override
    protected void doWriteStatistic() {
        if (csv.isFirstElement()) {
            writeHeader();
        }

        csv.writeDuration(Config.getDefaultCVSElementWidth());

        if (mode == Mode.Peer) {
            writeIndividualBandwidth();
        } else {
            writeTotalBandwidth();
        }

        csv.writeLine();
    }

    public enum Mode {
        Peer,
        TotalMedian,
        TotalAccumulated
    }
}
