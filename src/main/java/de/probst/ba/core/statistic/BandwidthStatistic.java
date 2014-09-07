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

    private final Collection<Peer> peers;
    private final Function<BandwidthStatisticState, Number> bandwidthMapper;
    private final Mode mode;

    public BandwidthStatistic(Path csvPath,
                              ScheduledExecutorService scheduledExecutorService,
                              long delay,
                              Collection<Peer> peers,
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
        csv.writeElement("Time");

        if (mode != Mode.Peer) {
            csv.writeElement("Total bandwidth");
        } else {
            peers.stream()
                 .forEach(peer -> csv.writeElement(peer.getPeerId().getAddress()));
        }

        csv.writeLine();
    }

    private void writeTotalBandwidth() {
        double totalUpload = 0;
        for (Peer peer : peers) {
            totalUpload += bandwidthMapper.apply(peer.getBandwidthStatisticState()).doubleValue();
        }
        double upload = mode == Mode.TotalMedian ? totalUpload / peers.size() : totalUpload;

        csv.writeElement(upload);
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
