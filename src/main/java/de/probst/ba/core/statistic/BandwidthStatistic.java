package de.probst.ba.core.statistic;

import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.state.BandwidthStatisticState;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

/**
 * Created by chrisprobst on 04.09.14.
 */
public final class BandwidthStatistic extends AbstractStatistic {

    private final Collection<Peer> peers;
    private final Function<BandwidthStatisticState, Number> bandwidthMapper;
    private final boolean total;

    private void writeHeader() {
        csv.writeElement("Time");

        if (total) {
            csv.writeElement("TotalAccumulated");
            csv.writeElement("TotalAverage");
            csv.writeElement("TotalLowerDeviation");
            csv.writeElement("TotalUpperDeviation");
        } else {
            peers.stream()
                 .forEach(peer -> csv.writeElement(peer.getPeerId().getSocketAddress()));
        }

        csv.writeLine();
    }

    private void writeTotalBandwidth() {
        double totalAccumulated = 0;
        for (Peer peer : peers) {
            totalAccumulated += bandwidthMapper.apply(peer.getBandwidthStatisticState()).doubleValue();
        }
        double totalAverage = totalAccumulated / peers.size();

        double totalLowerDeviation = 0;
        double totalUpperDeviation = 0;
        for (Peer peer : peers) {
            double peerBandwidth = bandwidthMapper.apply(peer.getBandwidthStatisticState()).doubleValue();

            if (peerBandwidth >= totalAverage) {
                totalUpperDeviation += peerBandwidth - totalAverage;
            } else {
                totalLowerDeviation += totalAverage - peerBandwidth;
            }
        }
        totalLowerDeviation /= peers.size();
        totalUpperDeviation /= peers.size();

        if (Double.isFinite(totalAccumulated)) {
            csv.writeElement(totalAccumulated);
        }
        if (Double.isFinite(totalAverage)) {
            csv.writeElement(totalAverage);
        }
        if (Double.isFinite(totalLowerDeviation)) {
            csv.writeElement(totalLowerDeviation);
        }
        if (Double.isFinite(totalUpperDeviation)) {
            csv.writeElement(totalUpperDeviation);
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

        if (total) {
            writeTotalBandwidth();
        } else {
            writeIndividualBandwidth();
        }

        csv.writeLine();
    }

    public BandwidthStatistic(String name,
                              Collection<Peer> peers,
                              Function<BandwidthStatisticState, Number> bandwidthMapper,
                              boolean total) {
        super(name);
        Objects.requireNonNull(peers);
        Objects.requireNonNull(bandwidthMapper);
        this.peers = peers;
        this.bandwidthMapper = bandwidthMapper;
        this.total = total;
    }
}
